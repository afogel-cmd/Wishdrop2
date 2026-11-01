package com.example.domain.usecase

import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.DownloadProgress
import com.example.domain.repository.AppUpdateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckForAppUpdateUseCase @Inject constructor(
  private val updateRepository: AppUpdateRepository,
) {
  suspend operator fun invoke(currentVersionName: String): Result<AppUpdateInfo> =
    updateRepository.checkForUpdate(currentVersionName)

  fun download(downloadUrl: String, fileName: String): Flow<DownloadProgress> =
    updateRepository.downloadUpdate(downloadUrl, fileName)
}
