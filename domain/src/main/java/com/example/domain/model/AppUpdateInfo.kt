package com.example.domain.model

data class AppUpdateInfo(
  val currentVersion: String,
  val latestVersion: String,
  val isUpdateAvailable: Boolean,
  val releaseTitle: String?,
  val releaseNotes: String?,
  val downloadUrl: String?,
  val fileName: String?,
  val fileSize: Long = 0L,
  val publishedAt: String? = null,
)

sealed interface DownloadProgress {
  data object Idle : DownloadProgress
  data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadProgress
  data class Completed(val apkPath: String) : DownloadProgress
  data class Failed(val error: String) : DownloadProgress
}
