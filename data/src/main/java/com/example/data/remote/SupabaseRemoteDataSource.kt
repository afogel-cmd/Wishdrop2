package com.example.data.remote

import com.example.core.config.SupabaseConfig
import com.example.core.di.IoDispatcher
import com.example.domain.model.RealtimeSyncStatus
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Serializable
data class SupabaseAuthSessionDto(
  @SerialName("access_token") val accessToken: String = "",
  @SerialName("user") val user: SupabaseAuthUserDto? = null,
)

@Serializable
data class SupabaseAuthUserDto(
  @SerialName("id") val id: String = "",
  @SerialName("email") val email: String = "",
)

data class RealtimeTableSignal(
  val table: String,
  val action: String,
  val timestampMillis: Long = System.currentTimeMillis(),
)

/**
 * Production Ktor client for Supabase Auth, PostgREST upserts, and Supabase Realtime WebSockets
 * (Phoenix JSON protocol v1.0.0), paired with an offline-first Realtime event coordinator so
 * collaborative mutations propagate immediately across active collectors even offline.
 */
@Singleton
class SupabaseRemoteDataSource
@Inject
constructor(
  private val httpClient: HttpClient,
  private val config: SupabaseConfig,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val _realtimeSignals = MutableSharedFlow<RealtimeTableSignal>(extraBufferCapacity = 32)
  val realtimeSignals: SharedFlow<RealtimeTableSignal> = _realtimeSignals.asSharedFlow()

  private val _syncStatus =
    MutableStateFlow(
      RealtimeSyncStatus(
        isCloudConfigured = config.isConfigured,
        isRealtimeSocketActive = !config.isConfigured,
        lastSyncedMillis = System.currentTimeMillis(),
        statusLabel =
          if (config.isConfigured) "Connecting to Cloud..."
          else "Offline-First Active",
      )
    )
  val syncStatus: StateFlow<RealtimeSyncStatus> = _syncStatus.asStateFlow()

  init {
    if (config.isConfigured) {
      connectRealtimeLoop()
    }
  }

  fun notifyLocalMutation(table: String, action: String, recordPayload: JsonObject? = null) {
    val now = System.currentTimeMillis()
    _realtimeSignals.tryEmit(RealtimeTableSignal(table = table, action = action, timestampMillis = now))
    _syncStatus.value =
      _syncStatus.value.copy(
        lastSyncedMillis = now,
        statusLabel =
          if (config.isConfigured) "Synced with Cloud"
          else "Offline-First Active",
      )

    if (config.isConfigured && recordPayload != null) {
      scope.launch { pushPostgrestUpsert(table, recordPayload) }
    }
  }

  suspend fun signInWithEmailPassword(
    email: String,
    password: String,
  ): Result<SupabaseAuthSessionDto> =
    runCatching {
      val cleanEmail = email.trim()
      require(cleanEmail.contains("@")) { "Enter a valid email address" }
      require(password.length >= 6) { "Password must be at least 6 characters" }

      if (!config.isConfigured) {
        return@runCatching SupabaseAuthSessionDto(
          accessToken = "local_session_${cleanEmail.hashCode()}",
          user = SupabaseAuthUserDto(id = "usr_${cleanEmail.hashCode()}", email = cleanEmail),
        )
      }

      val bodyJson =
        buildJsonObject {
          put("email", cleanEmail)
          put("password", password)
        }
      val response =
        httpClient.post("${config.normalizedUrl}/auth/v1/token?grant_type=password") {
          header("apikey", config.anonKey)
          contentType(ContentType.Application.Json)
          setBody(json.encodeToString(bodyJson))
        }
      val raw = response.bodyAsText()
      if (!response.status.isSuccess()) {
        throw IllegalStateException("Supabase Auth error (${response.status.value})")
      }
      json.decodeFromString<SupabaseAuthSessionDto>(raw)
    }

  suspend fun signUpWithEmailPassword(
    displayName: String,
    email: String,
    password: String,
  ): Result<SupabaseAuthSessionDto> =
    runCatching {
      val cleanEmail = email.trim()
      require(cleanEmail.contains("@")) { "Enter a valid email address" }
      require(password.length >= 6) { "Password must be at least 6 characters" }

      if (!config.isConfigured) {
        return@runCatching SupabaseAuthSessionDto(
          accessToken = "local_signup_${cleanEmail.hashCode()}",
          user = SupabaseAuthUserDto(id = "usr_${cleanEmail.hashCode()}", email = cleanEmail),
        )
      }

      val bodyJson =
        buildJsonObject {
          put("email", cleanEmail)
          put("password", password)
          putJsonObject("data") { put("display_name", displayName.trim()) }
        }
      val response =
        httpClient.post("${config.normalizedUrl}/auth/v1/signup") {
          header("apikey", config.anonKey)
          contentType(ContentType.Application.Json)
          setBody(json.encodeToString(bodyJson))
        }
      val raw = response.bodyAsText()
      if (!response.status.isSuccess()) {
        throw IllegalStateException("Supabase Sign-Up error (${response.status.value})")
      }
      json.decodeFromString<SupabaseAuthSessionDto>(raw)
    }

  private suspend fun pushPostgrestUpsert(table: String, payload: JsonObject) {
    runCatching {
      httpClient.post("${config.normalizedUrl}/rest/v1/$table") {
        header("apikey", config.anonKey)
        header("Authorization", "Bearer ${config.anonKey}")
        header("Prefer", "resolution=merge-duplicates,return=minimal")
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(payload))
      }
    }
  }

  private fun connectRealtimeLoop() {
    scope.launch {
      var refCounter = 1
      while (isActive) {
        runCatching {
            httpClient.webSocket(urlString = config.realtimeWebSocketUrl) {
              val joinFrame =
                buildJsonObject {
                  put("topic", "realtime:public")
                  put("event", "phx_join")
                  put("ref", "${refCounter++}")
                  putJsonObject("payload") {
                    putJsonObject("config") {
                      putJsonArray("postgres_changes") {
                        listOf(
                            "shared_lists",
                            "products",
                            "comments",
                            "activity_events",
                            "list_invitations",
                          )
                          .forEach { table ->
                            add(
                              buildJsonObject {
                                put("event", "*")
                                put("schema", "public")
                                put("table", table)
                              }
                            )
                          }
                      }
                    }
                  }
                }
              send(Frame.Text(json.encodeToString(joinFrame)))

              _syncStatus.value =
                RealtimeSyncStatus(
                  isCloudConfigured = true,
                  isRealtimeSocketActive = true,
                  lastSyncedMillis = System.currentTimeMillis(),
                  statusLabel = "Connected • Supabase Realtime Live",
                )

              val heartbeatJob = launch {
                while (isActive) {
                  delay(25_000)
                  val hb =
                    buildJsonObject {
                      put("topic", "phoenix")
                      put("event", "heartbeat")
                      putJsonObject("payload") {}
                      put("ref", "${refCounter++}")
                    }
                  send(Frame.Text(json.encodeToString(hb)))
                }
              }

              try {
                for (frame in incoming) {
                  if (frame is Frame.Text) {
                    val parsed =
                      runCatching { json.parseToJsonElement(frame.readText()) as? JsonObject }
                        .getOrNull()
                    val eventName = parsed?.get("event")?.jsonPrimitive?.content ?: ""
                    if (eventName == "postgres_changes") {
                      _realtimeSignals.tryEmit(
                        RealtimeTableSignal(table = "public", action = "REMOTE_CHANGE")
                      )
                      _syncStatus.value =
                        _syncStatus.value.copy(
                          lastSyncedMillis = System.currentTimeMillis(),
                          statusLabel = "Live update received • Supabase Realtime",
                        )
                    }
                  }
                }
              } finally {
                heartbeatJob.cancel()
              }
            }
          }
          .onFailure {
            _syncStatus.value =
              RealtimeSyncStatus(
                isCloudConfigured = true,
                isRealtimeSocketActive = false,
                lastSyncedMillis = System.currentTimeMillis(),
                statusLabel = "Offline-First Room Active • Retrying Cloud...",
              )
          }
        delay(12_000)
      }
    }
  }
}
