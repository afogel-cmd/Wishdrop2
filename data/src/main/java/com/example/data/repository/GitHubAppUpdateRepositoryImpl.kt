package com.example.data.repository

import android.content.Context
import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.DownloadProgress
import com.example.domain.repository.AppUpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Serializable
internal data class GitHubReleaseDto(
  @SerialName("tag_name") val tagName: String = "",
  val name: String? = null,
  val body: String? = null,
  @SerialName("published_at") val publishedAt: String? = null,
  val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
internal data class GitHubAssetDto(
  val name: String = "",
  val size: Long = 0L,
  @SerialName("browser_download_url") val browserDownloadUrl: String = "",
  @SerialName("content_type") val contentType: String? = null,
)

@Singleton
class GitHubAppUpdateRepositoryImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val httpClient: HttpClient,
  @com.example.core.di.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppUpdateRepository {

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  companion object {
    private const val GITHUB_OWNER = "afogel-cmd"
    private const val GITHUB_REPO = "Wishdrop2"
    private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
  }

  override suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo> = withContext(ioDispatcher) {
    runCatching {
      val response = httpClient.get(RELEASES_API_URL) {
        header("User-Agent", "WishDrop-Android")
        header("Accept", "application/vnd.github.v3+json")
      }

      if (response.status == HttpStatusCode.NotFound) {
        return@runCatching AppUpdateInfo(
          currentVersion = currentVersionName,
          latestVersion = currentVersionName,
          isUpdateAvailable = false,
          releaseTitle = null,
          releaseNotes = null,
          downloadUrl = null,
          fileName = null,
        )
      }

      val responseText = response.bodyAsText()
      val release = json.decodeFromString<GitHubReleaseDto>(responseText)

      val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        ?: release.assets.firstOrNull()

      val rawLatestVersion = release.tagName.ifBlank { release.name ?: "" }
      val isAvailable = isNewerVersion(currentVersion = currentVersionName, latestVersion = rawLatestVersion) && apkAsset != null

      val notes = release.body?.trim()?.ifBlank { null }

      AppUpdateInfo(
        currentVersion = currentVersionName,
        latestVersion = rawLatestVersion,
        isUpdateAvailable = isAvailable,
        releaseTitle = release.name ?: rawLatestVersion,
        releaseNotes = notes,
        downloadUrl = apkAsset?.browserDownloadUrl,
        fileName = apkAsset?.name ?: "wishdrop-$rawLatestVersion.apk",
        fileSize = apkAsset?.size ?: 0L,
        publishedAt = release.publishedAt,
      )
    }
  }

  override fun downloadUpdate(downloadUrl: String, fileName: String): Flow<DownloadProgress> = flow {
    emit(DownloadProgress.Downloading(progressPercent = 0, bytesDownloaded = 0L, totalBytes = -1L))

    val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
    val destinationFile = File(updatesDir, fileName)

    if (destinationFile.exists()) {
      destinationFile.delete()
    }

    var currentUrl = downloadUrl
    var connection: HttpURLConnection? = null
    var redirects = 0
    val maxRedirects = 5

    try {
      while (redirects < maxRedirects) {
        val url = URL(currentUrl)
        connection = (url.openConnection() as HttpURLConnection).apply {
          setRequestProperty("User-Agent", "WishDrop-Android")
          setRequestProperty("Accept", "*/*")
          connectTimeout = 15_000
          readTimeout = 30_000
          instanceFollowRedirects = true
        }

        val code = connection.responseCode
        if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
          code == HttpURLConnection.HTTP_MOVED_PERM ||
          code == 307 || code == 308
        ) {
          val redirectLocation = connection.getHeaderField("Location")
          connection.disconnect()
          if (!redirectLocation.isNullOrBlank()) {
            currentUrl = redirectLocation
            redirects++
            continue
          }
        }
        break
      }

      val finalConnection = connection ?: throw IllegalStateException("Could not establish download connection")
      val totalBytes = finalConnection.contentLengthLong

      finalConnection.inputStream.use { input ->
        FileOutputStream(destinationFile).use { output ->
          val buffer = ByteArray(8 * 1024)
          var bytesRead: Int
          var totalRead = 0L
          var lastReportedPercent = -1

          while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            totalRead += bytesRead

            val percent = if (totalBytes > 0) {
              ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100)
            } else {
              0
            }

            if (percent != lastReportedPercent) {
              lastReportedPercent = percent
              emit(
                DownloadProgress.Downloading(
                  progressPercent = percent,
                  bytesDownloaded = totalRead,
                  totalBytes = totalBytes,
                )
              )
            }
          }
          output.flush()
        }
      }

      emit(DownloadProgress.Completed(apkPath = destinationFile.absolutePath))
    } catch (e: Exception) {
      if (destinationFile.exists()) {
        destinationFile.delete()
      }
      emit(DownloadProgress.Failed(error = e.localizedMessage ?: "Download failed"))
    } finally {
      connection?.disconnect()
    }
  }.flowOn(ioDispatcher)

  private fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
    val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
    val cleanLatest = latestVersion.trim().removePrefix("v").removePrefix("V")

    if (cleanCurrent.isBlank() || cleanLatest.isBlank()) return false
    if (cleanCurrent.equals(cleanLatest, ignoreCase = true)) return false

    val currentParts = cleanCurrent.split(".", "-").mapNotNull { it.toIntOrNull() }
    val latestParts = cleanLatest.split(".", "-").mapNotNull { it.toIntOrNull() }

    val maxParts = maxOf(currentParts.size, latestParts.size)
    for (i in 0 until maxParts) {
      val curr = currentParts.getOrElse(i) { 0 }
      val late = latestParts.getOrElse(i) { 0 }
      if (late > curr) return true
      if (late < curr) return false
    }
    return false
  }
}
