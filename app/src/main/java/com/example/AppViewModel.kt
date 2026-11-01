package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.preferences.UserPreferences
import com.example.core.preferences.UserPreferencesRepository
import com.example.core.utils.ApkInstaller
import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.DownloadProgress
import com.example.domain.usecase.CheckForAppUpdateUseCase
import com.example.domain.usecase.ManageFamilyAuthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface AppUpdateDialogState {
  data object Hidden : AppUpdateDialogState
  data class Available(val info: AppUpdateInfo) : AppUpdateDialogState
  data class Downloading(val info: AppUpdateInfo, val percent: Int) : AppUpdateDialogState
}

@HiltViewModel
class AppViewModel @Inject constructor(
  preferencesRepository: UserPreferencesRepository,
  private val checkForAppUpdate: CheckForAppUpdateUseCase,
  private val manageFamilyAuth: ManageFamilyAuthUseCase,
) : ViewModel() {

  init {
    viewModelScope.launch {
      val prefs = preferencesRepository.preferencesFlow.first()
      if (prefs.isCloudAuthenticated && prefs.email.isNotBlank()) {
        manageFamilyAuth.syncWithCloud(prefs.email)
      }
    }
  }

  val preferences: StateFlow<UserPreferences> =
    preferencesRepository.preferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = UserPreferences(),
    )

  private val _updateDialogState = MutableStateFlow<AppUpdateDialogState>(AppUpdateDialogState.Hidden)
  val updateDialogState: StateFlow<AppUpdateDialogState> = _updateDialogState.asStateFlow()

  private var updateIgnoredForSession = false

  fun checkForStartupUpdate(currentVersion: String) {
    if (updateIgnoredForSession) return
    viewModelScope.launch {
      checkForAppUpdate(currentVersion).onSuccess { info ->
        if (info.isUpdateAvailable && !updateIgnoredForSession) {
          _updateDialogState.value = AppUpdateDialogState.Available(info)
        }
      }
    }
  }

  fun dismissUpdateDialog() {
    updateIgnoredForSession = true
    _updateDialogState.value = AppUpdateDialogState.Hidden
  }

  fun downloadAndInstall(context: Context, info: AppUpdateInfo) {
    val downloadUrl = info.downloadUrl ?: return
    val fileName = info.fileName ?: "wishdrop-${info.latestVersion}.apk"
    viewModelScope.launch {
      checkForAppUpdate.download(downloadUrl, fileName).collect { progress ->
        when (progress) {
          is DownloadProgress.Idle -> {}
          is DownloadProgress.Downloading -> {
            _updateDialogState.value = AppUpdateDialogState.Downloading(info, progress.progressPercent)
          }
          is DownloadProgress.Completed -> {
            _updateDialogState.value = AppUpdateDialogState.Hidden
            ApkInstaller.installApk(context, File(progress.apkPath))
          }
          is DownloadProgress.Failed -> {
            _updateDialogState.value = AppUpdateDialogState.Hidden
          }
        }
      }
    }
  }
}
