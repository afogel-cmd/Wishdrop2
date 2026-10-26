package com.example.feature.sharedlist

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.DataResult
import com.example.core.result.asDataResult
import com.example.domain.model.GiftRecipient
import com.example.domain.model.ListMemberRole
import com.example.domain.model.ListVisibility
import com.example.domain.model.SharedListDetails
import com.example.domain.usecase.ManageGiftPlanUseCase
import com.example.domain.usecase.ManageSharedListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
sealed interface SharedListUiState {
  data object Loading : SharedListUiState

  data object HiddenOrMissing : SharedListUiState

  @Immutable
  data class Success(
    val details: SharedListDetails,
    val familyRecipients: List<GiftRecipient> = emptyList(),
    val statusMessage: String? = null,
  ) : SharedListUiState

  @Immutable
  data class Error(val message: String) : SharedListUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SharedListViewModel
@Inject
constructor(
  private val manageSharedList: ManageSharedListUseCase,
  private val manageGiftPlan: ManageGiftPlanUseCase,
) : ViewModel() {

  private val listIdFlow = MutableStateFlow<String?>(null)
  private val statusMessageFlow = MutableStateFlow<String?>(null)
  private val retryTrigger = MutableStateFlow(0)

  val uiState: StateFlow<SharedListUiState> =
    combine(listIdFlow.filterNotNull(), retryTrigger) { listId, _ -> listId }
      .flatMapLatest { listId ->
        combine(
          manageSharedList.observeListDetails(listId).asDataResult(),
          manageGiftPlan.observeRecipients(),
          statusMessageFlow,
        ) { detailsResult, recipients, message ->
          when (detailsResult) {
            DataResult.Loading -> SharedListUiState.Loading
            is DataResult.Error -> SharedListUiState.Error(detailsResult.message)
            is DataResult.Success -> {
              val details = detailsResult.data
              if (details == null) {
                SharedListUiState.HiddenOrMissing
              } else {
                SharedListUiState.Success(
                  details = details,
                  familyRecipients = recipients,
                  statusMessage = message,
                )
              }
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SharedListUiState.Loading,
      )

  fun loadList(listId: String) {
    if (listIdFlow.value != listId) {
      listIdFlow.value = listId
    }
  }

  fun retry() {
    retryTrigger.update { it + 1 }
  }

  fun updateVisibility(visibility: ListVisibility, hiddenFromMemberId: String?) {
    val listId = listIdFlow.value ?: return
    viewModelScope.launch {
      manageSharedList.updateVisibility(listId, visibility, hiddenFromMemberId)
      statusMessageFlow.value = "Updated visibility to ${visibility.label}"
    }
  }

  fun updateListDetails(
    title: String,
    emoji: String,
    subtitle: String,
    visibility: ListVisibility,
    hiddenFromMemberId: String?,
  ) {
    val listId = listIdFlow.value ?: return
    viewModelScope.launch {
      manageSharedList.updateListDetails(listId, title, emoji, subtitle, visibility, hiddenFromMemberId)
      statusMessageFlow.value = "Updated list details"
    }
  }

  fun deleteList(onDeleted: () -> Unit) {
    val listId = listIdFlow.value ?: return
    viewModelScope.launch {
      manageSharedList.deleteList(listId)
      statusMessageFlow.value = "Shared list deleted"
      onDeleted()
    }
  }

  fun updateMemberRole(memberId: String, role: ListMemberRole) {
    val listId = listIdFlow.value ?: return
    viewModelScope.launch {
      manageSharedList.updateMemberRole(listId, memberId, role)
      statusMessageFlow.value = "Updated collaborator role to ${role.label}"
    }
  }

  fun removeMember(memberId: String) {
    val listId = listIdFlow.value ?: return
    viewModelScope.launch {
      manageSharedList.removeMember(listId, memberId)
      statusMessageFlow.value = "Tag eltávolítva a listáról"
    }
  }

  fun sendInvitation(email: String, role: ListMemberRole) {
    val listId = listIdFlow.value ?: return
    viewModelScope.launch {
      manageSharedList
        .inviteByEmail(listId, email, role)
        .onSuccess { invite ->
          statusMessageFlow.value = "Invitation sent to ${invite.inviteeEmail}"
        }
        .onFailure { err ->
          statusMessageFlow.value = err.message ?: "Unable to send invitation"
        }
    }
  }

  fun acceptInvitation(invitationId: String) {
    viewModelScope.launch {
      manageSharedList
        .acceptInvitation(invitationId)
        .onSuccess { statusMessageFlow.value = "Collaborator joined shared list" }
        .onFailure { statusMessageFlow.value = "Unable to accept invitation" }
    }
  }

  fun addQuickProduct(title: String, store: String, priceHuf: Int) {
    val listId = listIdFlow.value ?: return
    viewModelScope.launch {
      manageSharedList.addQuickProductIdea(listId, title, store, priceHuf)
      statusMessageFlow.value = "Added \"$title\" to shared list"
    }
  }

  fun consumeMessage() {
    statusMessageFlow.value = null
  }
}
