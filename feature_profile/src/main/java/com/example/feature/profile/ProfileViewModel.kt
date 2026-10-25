package com.example.feature.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.preferences.ThemeMode
import com.example.core.preferences.UserPreferences
import com.example.core.preferences.UserPreferencesRepository
import com.example.core.result.DataResult
import com.example.core.result.asDataResult
import com.example.domain.model.FamilyPersona
import com.example.domain.model.GiftRecipient
import com.example.domain.model.ListInvitation
import com.example.domain.model.RealtimeSyncStatus
import com.example.domain.usecase.ManageBackupUseCase
import com.example.domain.usecase.ManageFamilyAuthUseCase
import com.example.domain.usecase.ManageGiftPlanUseCase
import com.example.domain.usecase.ManageSharedListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
sealed interface ProfileUiState {
  data object Loading : ProfileUiState

  @Immutable
  data class Success(
    val preferences: UserPreferences = UserPreferences(),
    val familyMembers: List<GiftRecipient> = emptyList(),
    val personas: List<FamilyPersona> = emptyList(),
    val pendingInvitations: List<ListInvitation> = emptyList(),
    val realtimeStatus: RealtimeSyncStatus? = null,
    val statusMessage: String? = null,
  ) : ProfileUiState

  @Immutable
  data class Error(val message: String) : ProfileUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel
@Inject
constructor(
  private val preferencesRepository: UserPreferencesRepository,
  private val manageGiftPlan: ManageGiftPlanUseCase,
  private val manageBackup: ManageBackupUseCase,
  private val manageFamilyAuth: ManageFamilyAuthUseCase,
  private val manageSharedList: ManageSharedListUseCase,
) : ViewModel() {

  private val messageFlow = MutableStateFlow<String?>(null)
  private val retryTrigger = MutableStateFlow(0)

  val uiState: StateFlow<ProfileUiState> =
    retryTrigger
      .flatMapLatest {
        val coreFlow =
          combine(
            preferencesRepository.preferencesFlow,
            manageGiftPlan.observeRecipients(),
            messageFlow,
          ) { prefs, members, message ->
            Triple(prefs, members, message)
          }
        val collabFlow =
          combine(
            manageFamilyAuth.observePersonas(),
            manageFamilyAuth.observePendingInvitations(),
            manageFamilyAuth.observeRealtimeSyncStatus(),
          ) { personas, invites, syncStatus ->
            Triple(personas, invites, syncStatus)
          }
        combine(coreFlow, collabFlow) { (prefs, members, message), (personas, invites, syncStatus) ->
            ProfileUiState.Success(
              preferences = prefs,
              familyMembers = members,
              personas = personas,
              pendingInvitations = invites,
              realtimeStatus = syncStatus,
              statusMessage = message,
            )
          }
          .asDataResult()
          .map { result ->
            when (result) {
              DataResult.Loading -> ProfileUiState.Loading
              is DataResult.Error -> ProfileUiState.Error(result.message)
              is DataResult.Success -> result.data
            }
          }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState.Loading,
      )

  fun retry() {
    retryTrigger.update { it + 1 }
  }

  fun switchPersona(memberId: String) {
    viewModelScope.launch {
      manageFamilyAuth.switchActivePersona(memberId)
      messageFlow.value = "Switched active family viewer"
    }
  }

  fun signInCloud(email: String, password: String) {
    viewModelScope.launch {
      manageFamilyAuth
        .signIn(email.trim(), password)
        .onSuccess { msg -> messageFlow.value = msg }
        .onFailure { err -> messageFlow.value = err.message ?: "Cloud sign-in failed" }
    }
  }

  fun signUpCloud(displayName: String, email: String, password: String) {
    viewModelScope.launch {
      manageFamilyAuth
        .signUp(displayName.trim(), email.trim(), password)
        .onSuccess { msg -> messageFlow.value = msg }
        .onFailure { err -> messageFlow.value = err.message ?: "Account creation failed" }
    }
  }

  fun signOutCloud() {
    viewModelScope.launch {
      manageFamilyAuth.signOut()
      messageFlow.value = "Signed out of cloud session (local vault active)"
    }
  }

  fun acceptInvitation(invitationId: String, onJoinedList: (String) -> Unit) {
    viewModelScope.launch {
      manageSharedList
        .acceptInvitation(invitationId)
        .onSuccess { listId ->
          messageFlow.value = "Joined shared list"
          onJoinedList(listId)
        }
        .onFailure { err -> messageFlow.value = err.message ?: "Unable to accept invitation" }
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setUseDynamicColor(enabled) }
  }

  fun setPriceAlerts(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setPriceDropNotifications(enabled) }
  }

  fun setEventReminders(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setEventReminders(enabled) }
  }

  fun setAiAutoCategorization(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setAiAutoCategorization(enabled) }
  }

  fun setAiRecipientMatching(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setAiRecipientMatching(enabled) }
  }

  fun setAiConfidenceThreshold(value: Float) {
    viewModelScope.launch { preferencesRepository.setAiConfidenceThreshold(value) }
  }

  fun updateProfile(name: String, email: String) {
    viewModelScope.launch {
      preferencesRepository.updateProfile(name, email)
      messageFlow.value = "Updated profile"
    }
  }

  fun exportBackupJson(onReady: (String) -> Unit) {
    viewModelScope.launch {
      val json = manageBackup.exportJson()
      onReady(json)
      messageFlow.value = "Exported WishDrop backup JSON"
    }
  }

  fun importBackupJson(jsonPayload: String) {
    viewModelScope.launch {
      manageBackup
        .importJson(jsonPayload)
        .onSuccess { count -> messageFlow.value = "Imported $count products from backup" }
        .onFailure { messageFlow.value = "Invalid backup file format" }
    }
  }

  fun resetCatalog() {
    viewModelScope.launch {
      manageBackup.resetCatalog()
      messageFlow.value = "Restored default WishDrop catalog"
    }
  }

  fun consumeMessage() {
    messageFlow.value = null
  }
}
