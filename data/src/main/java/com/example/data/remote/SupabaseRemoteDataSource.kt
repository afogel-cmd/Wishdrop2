package com.example.data.remote

import com.example.core.config.SupabaseConfig
import com.example.core.di.IoDispatcher
  import com.example.core.preferences.UserPreferencesRepository
import com.example.domain.model.RealtimeSyncStatus
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.client.request.get
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
import kotlinx.coroutines.Job
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

@Serializable
data class CloudUserDto(
  @SerialName("id") val id: String = "",
  @SerialName("email") val email: String = "",
  @SerialName("display_name") val displayName: String = "",
  @SerialName("avatar_key") val avatarKey: String = "avatar_attila",
  @SerialName("linked_member_id") val linkedMemberId: String = "rec_attila",
  @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class CloudFamilyMemberDto(
  @SerialName("id") val id: String = "",
  @SerialName("name") val name: String = "",
  @SerialName("email") val email: String? = null,
  @SerialName("relationship") val relationship: String = "",
  @SerialName("avatar_key") val avatarKey: String = "avatar_attila",
  @SerialName("accent_key") val accentKey: String = "indigo",
  @SerialName("upcoming_event_name") val upcomingEventName: String = "",
  @SerialName("days_until_event") val daysUntilEvent: Int = 1,
  @SerialName("budget_huf") val budgetHuf: Int = 10000,
  @SerialName("interests_json") val interestsJson: String = "[]",
  @SerialName("events_json") val eventsJson: String = "[]",
  @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class CloudProductDto(
  @SerialName("id") val id: String = "",
  @SerialName("title") val title: String = "",
  @SerialName("store") val store: String = "",
  @SerialName("current_price_huf") val currentPriceHuf: Int = 0,
  @SerialName("original_price_huf") val originalPriceHuf: Int = 0,
  @SerialName("image_url") val imageUrl: String? = null,
  @SerialName("affiliate_url") val affiliateUrl: String = "",
  @SerialName("is_purchased") val isPurchased: Boolean = false,
  @SerialName("is_archived") val isArchived: Boolean = false,
  @SerialName("recipient_id") val recipientId: String? = null,
  @SerialName("shared_list_id") val sharedListId: String? = null,
  @SerialName("notes") val notes: String = "",
  @SerialName("tags_json") val tagsJson: String = "[]",
)

@Serializable
data class CloudSharedListDto(
  @SerialName("id") val id: String = "",
  @SerialName("title") val title: String = "",
  @SerialName("emoji") val emoji: String = "🎁",
  @SerialName("subtitle") val subtitle: String = "",
  @SerialName("owner_member_id") val ownerMemberId: String = "rec_attila",
  @SerialName("visibility") val visibility: String = "SHARED",
  @SerialName("event_date_label") val eventDateLabel: String = "",
  @SerialName("updated_at_ms") val updatedAtMs: Long = 0L,
)

data class RealtimeTableSignal(
  val table: String,
  val action: String,
  val timestampMillis: Long = System.currentTimeMillis(),
)

/**
 * Production Ktor client for Supabase Auth, PostgREST REST APIs, and Realtime WebSockets.
 * Supports dynamic configuration via UserPreferences or BuildConfig.
 */
@Singleton
class SupabaseRemoteDataSource
@Inject
constructor(
  private val httpClient: HttpClient,
  private val config: SupabaseConfig,
  private val preferencesRepository: UserPreferencesRepository,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
  private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
  }

  @Volatile
  private var activeConfig: SupabaseConfig = config
  private var realtimeJob: Job? = null

  private val _realtimeSignals = MutableSharedFlow<RealtimeTableSignal>(extraBufferCapacity = 32)
  val realtimeSignals: SharedFlow<RealtimeTableSignal> = _realtimeSignals.asSharedFlow()

  private val _syncStatus =
    MutableStateFlow(
      RealtimeSyncStatus(
        isCloudConfigured = config.isConfigured,
        isRealtimeSocketActive = false,
        lastSyncedMillis = System.currentTimeMillis(),
        statusLabel =
          if (config.isConfigured) "Felhőhöz kapcsolódás..."
          else "Helyi cache aktív • Felhő nincs konfigurálva",
      )
    )
  val syncStatus: StateFlow<RealtimeSyncStatus> = _syncStatus.asStateFlow()

  init {
    scope.launch {
      preferencesRepository.preferencesFlow.collect { prefs ->
        val customUrl = prefs.customSupabaseUrl
        val customKey = prefs.customSupabaseAnonKey
        val effective =
          if (!customUrl.isNullOrBlank() && !customKey.isNullOrBlank()) {
            SupabaseConfig(customUrl, customKey)
          } else {
            config
          }
        val changed =
          effective.normalizedUrl != activeConfig.normalizedUrl ||
            effective.anonKey != activeConfig.anonKey
        activeConfig = effective
        _syncStatus.value =
          _syncStatus.value.copy(
            isCloudConfigured = effective.isConfigured,
            statusLabel =
              if (effective.isConfigured) "Felhő szinkron aktív • Supabase Live"
              else "Helyi cache aktív • Felhő nincs konfigurálva",
          )
        if (changed && effective.isConfigured) {
          restartRealtime()
        }
      }
    }
    if (config.isConfigured) {
      restartRealtime()
    }
  }

  fun getEffectiveConfig(): SupabaseConfig = activeConfig

  suspend fun testConnection(url: String, anonKey: String): Result<Boolean> =
    runCatching {
      val cleanUrl = url.trim().trimEnd('/')
      val cleanKey = anonKey.trim()
      val response =
        httpClient.get("$cleanUrl/rest/v1/users?select=id&limit=1") {
          header("apikey", cleanKey)
          header("Authorization", "Bearer $cleanKey")
          header("Accept", "application/json")
        }
      response.status.isSuccess()
    }

  fun notifyLocalMutation(table: String, action: String, recordPayload: JsonObject? = null) {
    val now = System.currentTimeMillis()
    _realtimeSignals.tryEmit(
      RealtimeTableSignal(table = table, action = action, timestampMillis = now)
    )
    val cfg = getEffectiveConfig()
    _syncStatus.value =
      _syncStatus.value.copy(
        lastSyncedMillis = now,
        statusLabel =
          if (cfg.isConfigured) "Felhőbe szinkronizálva"
          else "Helyi cache aktív • Felhő nincs konfigurálva",
      )

    if (cfg.isConfigured && recordPayload != null) {
      scope.launch { pushPostgrestUpsert(table, recordPayload) }
    }
  }

  suspend fun pushPostgrestUpsert(table: String, payload: JsonObject): Result<Unit> =
    runCatching {
      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) return@runCatching
      val response =
        httpClient.post("${cfg.normalizedUrl}/rest/v1/$table") {
          header("apikey", cfg.anonKey)
          header("Authorization", "Bearer ${cfg.anonKey}")
          header("Prefer", "resolution=merge-duplicates,return=minimal")
          contentType(ContentType.Application.Json)
          setBody(json.encodeToString(payload))
        }
      if (!response.status.isSuccess()) {
        throw IllegalStateException("Supabase push failed: ${response.status}")
      }
    }

  suspend fun pushPostgrestDelete(table: String, id: String): Result<Unit> =
    runCatching {
      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) return@runCatching
      val response =
        httpClient.delete("${cfg.normalizedUrl}/rest/v1/$table?id=eq.$id") {
          header("apikey", cfg.anonKey)
          header("Authorization", "Bearer ${cfg.anonKey}")
        }
      if (!response.status.isSuccess()) {
        throw IllegalStateException("Supabase delete failed: ${response.status}")
      }
    }

  suspend fun fetchUserProfile(email: String): Result<CloudUserDto?> =
    runCatching {
      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) return@runCatching null
      val response =
        httpClient.get("${cfg.normalizedUrl}/rest/v1/users?email=eq.$email&select=*") {
          header("apikey", cfg.anonKey)
          header("Authorization", "Bearer ${cfg.anonKey}")
          header("Accept", "application/json")
        }
      if (!response.status.isSuccess()) return@runCatching null
      val raw = response.bodyAsText()
      val list = json.decodeFromString<List<CloudUserDto>>(raw)
      list.firstOrNull()
    }

  suspend fun upsertUserProfile(user: CloudUserDto): Result<Unit> =
    runCatching {
      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) return@runCatching
      val payload = buildJsonObject {
        put("id", user.id)
        put("email", user.email)
        put("display_name", user.displayName)
        put("avatar_key", user.avatarKey)
        put("linked_member_id", user.linkedMemberId)
        user.avatarUrl?.let { put("avatar_url", it) }
      }
      pushPostgrestUpsert("users", payload).getOrThrow()
    }

  suspend fun fetchFamilyMembers(): Result<List<CloudFamilyMemberDto>> =
    runCatching {
      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) return@runCatching emptyList()
      val response =
        httpClient.get("${cfg.normalizedUrl}/rest/v1/family_members?select=*") {
          header("apikey", cfg.anonKey)
          header("Authorization", "Bearer ${cfg.anonKey}")
          header("Accept", "application/json")
        }
      if (!response.status.isSuccess()) return@runCatching emptyList()
      json.decodeFromString<List<CloudFamilyMemberDto>>(response.bodyAsText())
    }

  suspend fun upsertFamilyMember(member: CloudFamilyMemberDto): Result<Unit> =
    runCatching {
      val payload = buildJsonObject {
        put("id", member.id)
        put("name", member.name)
        member.email?.let { put("email", it) }
        put("relationship", member.relationship)
        put("avatar_key", member.avatarKey)
        put("accent_key", member.accentKey)
        put("upcoming_event_name", member.upcomingEventName)
        put("days_until_event", member.daysUntilEvent)
        put("budget_huf", member.budgetHuf)
        put("interests_json", member.interestsJson)
        put("events_json", member.eventsJson)
        member.avatarUrl?.let { put("avatar_url", it) }
      }
      pushPostgrestUpsert("family_members", payload).getOrThrow()
    }

  suspend fun deleteFamilyMember(id: String): Result<Unit> =
    pushPostgrestDelete("family_members", id)

  suspend fun fetchProducts(): Result<List<CloudProductDto>> =
    runCatching {
      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) return@runCatching emptyList()
      val response =
        httpClient.get("${cfg.normalizedUrl}/rest/v1/products?select=*") {
          header("apikey", cfg.anonKey)
          header("Authorization", "Bearer ${cfg.anonKey}")
          header("Accept", "application/json")
        }
      if (!response.status.isSuccess()) return@runCatching emptyList()
      json.decodeFromString<List<CloudProductDto>>(response.bodyAsText())
    }

  suspend fun upsertProduct(product: CloudProductDto): Result<Unit> =
    runCatching {
      val payload = buildJsonObject {
        put("id", product.id)
        put("title", product.title)
        put("store", product.store)
        put("current_price_huf", product.currentPriceHuf)
        put("original_price_huf", product.originalPriceHuf)
        product.imageUrl?.let { put("image_url", it) }
        put("affiliate_url", product.affiliateUrl)
        put("is_purchased", product.isPurchased)
        put("is_archived", product.isArchived)
        product.recipientId?.let { put("recipient_id", it) }
        product.sharedListId?.let { put("shared_list_id", it) }
        put("notes", product.notes)
        put("tags_json", product.tagsJson)
      }
      pushPostgrestUpsert("products", payload).getOrThrow()
    }

  suspend fun deleteProduct(id: String): Result<Unit> =
    pushPostgrestDelete("products", id)

  suspend fun fetchSharedLists(): Result<List<CloudSharedListDto>> =
    runCatching {
      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) return@runCatching emptyList()
      val response =
        httpClient.get("${cfg.normalizedUrl}/rest/v1/shared_lists?select=*") {
          header("apikey", cfg.anonKey)
          header("Authorization", "Bearer ${cfg.anonKey}")
          header("Accept", "application/json")
        }
      if (!response.status.isSuccess()) return@runCatching emptyList()
      json.decodeFromString<List<CloudSharedListDto>>(response.bodyAsText())
    }

  suspend fun upsertSharedList(list: CloudSharedListDto): Result<Unit> =
    runCatching {
      val payload = buildJsonObject {
        put("id", list.id)
        put("title", list.title)
        put("emoji", list.emoji)
        put("subtitle", list.subtitle)
        put("owner_member_id", list.ownerMemberId)
        put("visibility", list.visibility)
        put("event_date_label", list.eventDateLabel)
        put("updated_at_ms", list.updatedAtMs)
      }
      pushPostgrestUpsert("shared_lists", payload).getOrThrow()
    }

  suspend fun deleteSharedList(id: String): Result<Unit> =
    pushPostgrestDelete("shared_lists", id)

  suspend fun signInWithEmailPassword(
    email: String,
    password: String,
  ): Result<SupabaseAuthSessionDto> =
    runCatching {
      val cleanEmail = email.trim()
      require(cleanEmail.contains("@")) { "Enter a valid email address" }
      require(password.length >= 6) { "Password must be at least 6 characters" }

      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) {
        return@runCatching SupabaseAuthSessionDto(
          accessToken = "local_session_${cleanEmail.hashCode()}",
          user = SupabaseAuthUserDto(id = "usr_${cleanEmail.hashCode()}", email = cleanEmail),
        )
      }

      val bodyJson = buildJsonObject {
        put("email", cleanEmail)
        put("password", password)
      }
      val response =
        httpClient.post("${cfg.normalizedUrl}/auth/v1/token?grant_type=password") {
          header("apikey", cfg.anonKey)
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

      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) {
        return@runCatching SupabaseAuthSessionDto(
          accessToken = "local_signup_${cleanEmail.hashCode()}",
          user = SupabaseAuthUserDto(id = "usr_${cleanEmail.hashCode()}", email = cleanEmail),
        )
      }

      val bodyJson = buildJsonObject {
        put("email", cleanEmail)
        put("password", password)
        putJsonObject("data") { put("display_name", displayName.trim()) }
      }
      val response =
        httpClient.post("${cfg.normalizedUrl}/auth/v1/signup") {
          header("apikey", cfg.anonKey)
          contentType(ContentType.Application.Json)
          setBody(json.encodeToString(bodyJson))
        }
      val raw = response.bodyAsText()
      if (!response.status.isSuccess()) {
        throw IllegalStateException("Supabase Sign-Up error (${response.status.value})")
      }
      json.decodeFromString<SupabaseAuthSessionDto>(raw)
    }

  private fun restartRealtime() {
    realtimeJob?.cancel()
    realtimeJob = scope.launch {
      val cfg = getEffectiveConfig()
      if (!cfg.isConfigured) return@launch
      var refCounter = 1
      while (isActive) {
        runCatching {
          httpClient.webSocket(urlString = cfg.realtimeWebSocketUrl) {
            val joinFrame = buildJsonObject {
              put("topic", "realtime:public")
              put("event", "phx_join")
              put("ref", "${refCounter++}")
              putJsonObject("payload") {
                putJsonObject("config") {
                  putJsonArray("postgres_changes") {
                    listOf(
                      "users",
                      "family_members",
                      "shared_lists",
                      "products",
                      "comments",
                      "activity_events",
                      "list_invitations",
                    ).forEach { table ->
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
                statusLabel = "Felhő szinkron aktív • Supabase Live",
              )

            val heartbeatJob = launch {
              while (isActive) {
                delay(25_000)
                val hb = buildJsonObject {
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
                    val table = parsed?.get("payload")?.let {
                      (it as? JsonObject)?.get("data")?.let { d ->
                        (d as? JsonObject)?.get("table")?.jsonPrimitive?.content
                      }
                    } ?: "public"
                    _realtimeSignals.tryEmit(
                      RealtimeTableSignal(table = table, action = "REMOTE_CHANGE")
                    )
                    _syncStatus.value =
                      _syncStatus.value.copy(
                        lastSyncedMillis = System.currentTimeMillis(),
                        statusLabel = "Felhőből frissítve • Supabase Live",
                      )
                  }
                }
              }
            } finally {
              heartbeatJob.cancel()
            }
          }
        }.onFailure {
          _syncStatus.value =
            RealtimeSyncStatus(
              isCloudConfigured = true,
              isRealtimeSocketActive = false,
              lastSyncedMillis = System.currentTimeMillis(),
              statusLabel = "Felhő újracsatlakozás...",
            )
        }
        delay(15_000)
      }
    }
  }
}
