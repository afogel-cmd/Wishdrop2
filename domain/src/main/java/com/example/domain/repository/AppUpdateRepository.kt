package com.example.domain.repository

import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.DownloadProgress
import kotlinx.coroutines.flow.Flow

interface AppUpdateRepository {
  suspend fun checkForUpdate(currentVersionName: String): Result<AppUpdateInfo>
  fun downloadUpdate(downloadUrl: String, fileName: String): Flow<DownloadProgress>
}
