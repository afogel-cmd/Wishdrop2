package com.example.feature.giftassistant

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.DataResult
import com.example.core.result.asDataResult
import com.example.domain.model.AiRecommendation
import com.example.domain.model.GiftRecipient
import com.example.domain.model.GiftStage
import com.example.domain.model.Product
import com.example.domain.usecase.ManageGiftPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
sealed interface GiftAssistantUiState {
  data object Loading : GiftAssistantUiState

  data object Empty : GiftAssistantUiState

  @Immutable
  data class Success(
    val recipients: List<GiftRecipient>,
    val selectedRecipient: GiftRecipient,
    val savedIdeas: List<Product>,
    val aiRecommendations: List<AiRecommendation>,
    val snackbarMessage: String? = null,
  ) : GiftAssistantUiState

  @Immutable
  data class Error(val message: String) : GiftAssistantUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GiftAssistantViewModel
@Inject
constructor(private val manageGiftPlan: ManageGiftPlanUseCase) : ViewModel() {

  private val selectedIdFlow = MutableStateFlow<String?>(null)
  private val snackbarFlow = MutableStateFlow<String?>(null)
  private val retryFlow = MutableStateFlow(0)

  val uiState: StateFlow<GiftAssistantUiState> =
    retryFlow
      .flatMapLatest {
        combine(manageGiftPlan.observeRecipients().asDataResult(), selectedIdFlow) {
          recipientsResult,
          selectedId ->
          Pair(recipientsResult, selectedId)
        }
      }
      .flatMapLatest { (recipientsResult, selectedId) ->
        when (recipientsResult) {
          DataResult.Loading -> flowOf(GiftAssistantUiState.Loading)
          is DataResult.Error -> flowOf(GiftAssistantUiState.Error(recipientsResult.message))
          is DataResult.Success -> {
            val recipients = recipientsResult.data
            val chosen = recipients.find { it.id == selectedId } ?: recipients.firstOrNull()
            if (chosen == null) {
              flowOf(GiftAssistantUiState.Empty)
            } else {
              combine(
                  manageGiftPlan.observeSavedIdeas(chosen.id),
                  manageGiftPlan.observeRecommendations(chosen.id),
                  snackbarFlow,
                ) { ideas, recs, snackbar ->
                  GiftAssistantUiState.Success(
                    recipients = recipients,
                    selectedRecipient = chosen,
                    savedIdeas = ideas,
                    aiRecommendations = recs,
                    snackbarMessage = snackbar,
                  ) as GiftAssistantUiState
                }
                .catch { e ->
                  emit(
                    GiftAssistantUiState.Error(
                      e.localizedMessage ?: "Failed to load gift plan."
                    )
                  )
                }
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GiftAssistantUiState.Loading,
      )

  fun retry() {
    retryFlow.update { it + 1 }
  }

  fun selectInitialRecipientIfProvided(recipientId: String?) {
    if (!recipientId.isNullOrBlank()) {
      selectedIdFlow.value = recipientId
    }
  }

  fun selectRecipient(recipientId: String) {
    selectedIdFlow.value = recipientId
  }

  fun updateStage(stage: GiftStage) {
    val current = uiState.value as? GiftAssistantUiState.Success ?: return
    viewModelScope.launch {
      manageGiftPlan.updateStage(current.selectedRecipient.id, stage)
    }
  }

  fun addInterest(interest: String) {
    val current = uiState.value as? GiftAssistantUiState.Success ?: return
    viewModelScope.launch {
      manageGiftPlan.addInterest(current.selectedRecipient.id, interest)
    }
  }

  fun removeInterest(interest: String) {
    val current = uiState.value as? GiftAssistantUiState.Success ?: return
    viewModelScope.launch {
      manageGiftPlan.removeInterest(current.selectedRecipient.id, interest)
    }
  }

  fun saveRecommendation(recommendation: AiRecommendation) {
    viewModelScope.launch {
      manageGiftPlan.saveRecommendation(recommendation)
      snackbarFlow.value = "Saved ${recommendation.title} to Wishlist"
    }
  }

  fun dismissRecommendation(recommendationId: String) {
    viewModelScope.launch {
      manageGiftPlan.dismissRecommendation(recommendationId)
      snackbarFlow.value = "Recommendation dismissed"
    }
  }

  fun generateAiRecommendations() {
    val current = uiState.value as? GiftAssistantUiState.Success ?: return
    viewModelScope.launch {
      manageGiftPlan.generateAiRecommendations(current.selectedRecipient.id)
      snackbarFlow.value = "Új AI ajándékötletek generálva ${current.selectedRecipient.name} részére"
    }
  }

  fun deletePerson(recipientId: String) {
    viewModelScope.launch {
      manageGiftPlan.deletePerson(recipientId)
      snackbarFlow.value = "Person removed"
    }
  }

  fun updatePerson(
    recipientId: String,
    name: String,
    relationship: String,
    eventName: String,
    daysUntilEvent: Int,
    interestsCsv: String,
  ) {
    if (name.isBlank()) return
    viewModelScope.launch {
      val interests =
        interestsCsv
          .split(",")
          .map { it.trim() }
          .filter { it.isNotEmpty() }
      manageGiftPlan.updatePerson(
        recipientId = recipientId,
        name = name,
        relationship = relationship,
        eventName = eventName,
        daysUntilEvent = daysUntilEvent,
        interests = interests,
      )
      snackbarFlow.value = "Updated $name"
    }
  }

  fun addNewPerson(
    name: String,
    relationship: String,
    eventName: String,
    daysUntilEvent: Int,
    interestsCsv: String,
  ) {
    if (name.isBlank()) return
    viewModelScope.launch {
      val interests =
        interestsCsv
          .split(",")
          .map { it.trim() }
          .filter { it.isNotEmpty() }
      val newId =
        manageGiftPlan.addPerson(
          name = name,
          relationship = relationship,
          eventName = eventName,
          daysUntilEvent = daysUntilEvent,
          budgetHuf = 35_000,
          interests = interests,
        )
      selectedIdFlow.value = newId
      snackbarFlow.value = "Added $name to Gift Assistant"
    }
  }

  fun consumeSnackbar() {
    snackbarFlow.value = null
  }
}
