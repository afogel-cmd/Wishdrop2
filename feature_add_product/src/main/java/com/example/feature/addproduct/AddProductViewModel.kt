package com.example.feature.addproduct

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.ai.ExtractionProgress
import com.example.domain.model.AddInputMode
import com.example.domain.model.ExtractedProductDraft
import com.example.domain.model.GiftRecipient
import com.example.domain.model.RecipientPrediction
import com.example.domain.usecase.AnalyzeProductInputUseCase
import com.example.domain.usecase.ManageGiftPlanUseCase
import com.example.domain.usecase.ManageWishlistItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
sealed interface AddFlowStep {
  data object Input : AddFlowStep

  @Immutable
  data class Processing(val stepLabel: String, val progress: Float) : AddFlowStep

  @Immutable
  data class Review(val draft: ExtractedProductDraft) : AddFlowStep

  @Immutable
  data class Error(val message: String) : AddFlowStep
}

@Immutable
data class AddProductUiState(
  val mode: AddInputMode = AddInputMode.URL,
  val step: AddFlowStep = AddFlowStep.Input,
  val urlInput: String = "",
  val manualName: String = "",
  val manualStore: String = "Alza",
  val manualPrice: String = "24990",
  val manualNotes: String = "",
  val barcodeInput: String = "0840353900240",
  val recipients: List<GiftRecipient> = emptyList(),
)

@HiltViewModel
class AddProductViewModel
@Inject
constructor(
  private val analyzeProductInput: AnalyzeProductInputUseCase,
  private val manageWishlistItem: ManageWishlistItemUseCase,
  manageGiftPlan: ManageGiftPlanUseCase,
) : ViewModel() {

  private val internalState = MutableStateFlow(AddProductUiState())

  val uiState: StateFlow<AddProductUiState> =
    combine(internalState, manageGiftPlan.observeRecipients()) { state, recipients ->
        state.copy(recipients = recipients)
      }
      .catch { e ->
        emit(
          internalState.value.copy(
            step = AddFlowStep.Error(e.localizedMessage ?: "Failed to load gift recipients.")
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddProductUiState(),
      )

  fun setInitialMode(modeName: String) {
    val parsed =
      runCatching { AddInputMode.valueOf(modeName.uppercase()) }.getOrDefault(AddInputMode.URL)
    internalState.update {
      if (it.step is AddFlowStep.Input) it.copy(mode = parsed) else it
    }
  }

  fun selectMode(mode: AddInputMode) {
    internalState.update { it.copy(mode = mode, step = AddFlowStep.Input) }
  }

  fun updateUrl(url: String) {
    internalState.update { it.copy(urlInput = url) }
  }

  fun updateManualFields(
    name: String = internalState.value.manualName,
    store: String = internalState.value.manualStore,
    price: String = internalState.value.manualPrice,
    notes: String = internalState.value.manualNotes,
  ) {
    internalState.update {
      it.copy(
        manualName = name,
        manualStore = store,
        manualPrice = price.filter(Char::isDigit),
        manualNotes = notes,
      )
    }
  }

  fun updateBarcode(barcode: String) {
    internalState.update { it.copy(barcodeInput = barcode.filter(Char::isDigit)) }
  }

  fun startExtraction() {
    val current = internalState.value
    val flow =
      analyzeProductInput(
        mode = current.mode,
        url = current.urlInput.ifBlank { "https://store.google.com/product/pixel_buds_2a" },
        manualName = current.manualName,
        manualStore = current.manualStore,
        manualPriceHuf = current.manualPrice.toIntOrNull() ?: 24_990,
        manualNotes = current.manualNotes,
        barcode = current.barcodeInput.ifBlank { "0840353900240" },
      )

    viewModelScope.launch {
      flow
        .catch { e ->
          internalState.update {
            it.copy(
              step =
                AddFlowStep.Error(
                  e.localizedMessage ?: "Unable to analyze product details right now."
                )
            )
          }
        }
        .collect { progress ->
          when (progress) {
            is ExtractionProgress.Analyzing -> {
              internalState.update {
                it.copy(
                  step = AddFlowStep.Processing(progress.stepLabel, progress.progressFraction)
                )
              }
            }
            is ExtractionProgress.Completed -> {
              internalState.update { it.copy(step = AddFlowStep.Review(progress.draft)) }
            }
          }
        }
    }
  }

  fun updateDraftField(transform: (ExtractedProductDraft) -> ExtractedProductDraft) {
    internalState.update { state ->
      val review = state.step as? AddFlowStep.Review ?: return@update state
      state.copy(step = AddFlowStep.Review(transform(review.draft)))
    }
  }

  fun selectDraftRecipient(recipient: GiftRecipient) {
    updateDraftField { draft ->
      draft.copy(
        recipientPrediction =
          RecipientPrediction(
            recipientId = recipient.id,
            recipientName = recipient.name,
            confidencePercent = 98,
            explanation = "Selected manually for ${recipient.upcomingEventName}.",
          )
      )
    }
  }

  fun resetToInput() {
    internalState.update { it.copy(step = AddFlowStep.Input) }
  }

  fun saveReviewedDraft(onSaved: (String) -> Unit) {
    val review = internalState.value.step as? AddFlowStep.Review ?: return
    viewModelScope.launch {
      runCatching { manageWishlistItem.saveDraft(review.draft) }
        .onSuccess(onSaved)
        .onFailure { e ->
          internalState.update {
            it.copy(step = AddFlowStep.Error(e.localizedMessage ?: "Could not save product."))
          }
        }
    }
  }
}
