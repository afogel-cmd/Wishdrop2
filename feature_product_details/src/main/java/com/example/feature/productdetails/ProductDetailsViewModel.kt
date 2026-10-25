package com.example.feature.productdetails

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.DataResult
import com.example.core.result.asDataResult
import com.example.domain.model.ActivityEvent
import com.example.domain.model.GiftRecipient
import com.example.domain.model.PricePeriod
import com.example.domain.model.PricePoint
import com.example.domain.model.Product
import com.example.domain.model.ProductComment
import com.example.domain.model.SharedListSummary
import com.example.domain.usecase.ManageWishlistItemUseCase
import com.example.domain.usecase.ObserveProductDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
sealed interface ProductDetailsUiState {
  data object Loading : ProductDetailsUiState

  data object Empty : ProductDetailsUiState

  @Immutable
  data class Success(
    val product: Product,
    val selectedPeriod: PricePeriod = PricePeriod.DAYS_90,
    val priceHistory: List<PricePoint> = emptyList(),
    val allRecipients: List<GiftRecipient> = emptyList(),
    val comments: List<ProductComment> = emptyList(),
    val activityEvents: List<ActivityEvent> = emptyList(),
    val sharedLists: List<SharedListSummary> = emptyList(),
  ) : ProductDetailsUiState

  @Immutable
  data class Error(val message: String) : ProductDetailsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductDetailsViewModel
@Inject
constructor(
  private val observeProductDetails: ObserveProductDetailsUseCase,
  private val manageWishlistItem: ManageWishlistItemUseCase,
) : ViewModel() {

  private val productIdFlow = MutableStateFlow<String?>(null)
  private val periodFlow = MutableStateFlow(PricePeriod.DAYS_90)
  private val retryTrigger = MutableStateFlow(0)

  val uiState: StateFlow<ProductDetailsUiState> =
    combine(productIdFlow.filterNotNull(), periodFlow, retryTrigger) { id, period, _ ->
        Pair(id, period)
      }
      .flatMapLatest { (id, period) ->
        observeProductDetails(id, period).asDataResult().map { result ->
          when (result) {
            DataResult.Loading -> ProductDetailsUiState.Loading
            is DataResult.Error -> ProductDetailsUiState.Error(result.message)
            is DataResult.Success -> {
              val product = result.data.product
              if (product == null) {
                ProductDetailsUiState.Empty
              } else {
                ProductDetailsUiState.Success(
                  product = product,
                  selectedPeriod = period,
                  priceHistory = result.data.priceHistory,
                  allRecipients = result.data.allRecipients,
                  comments = result.data.comments,
                  activityEvents = result.data.activityEvents,
                  sharedLists = result.data.sharedLists,
                )
              }
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductDetailsUiState.Loading,
      )

  fun loadProduct(productId: String) {
    if (productIdFlow.value != productId) {
      productIdFlow.value = productId
    }
  }

  fun selectPeriod(period: PricePeriod) {
    periodFlow.value = period
  }

  fun retry() {
    retryTrigger.update { it + 1 }
  }

  fun togglePurchased() {
    val current = (uiState.value as? ProductDetailsUiState.Success)?.product ?: return
    viewModelScope.launch {
      manageWishlistItem.setPurchased(current.id, !current.isPurchased)
    }
  }

  fun togglePriceAlert() {
    val current = (uiState.value as? ProductDetailsUiState.Success)?.product ?: return
    viewModelScope.launch {
      manageWishlistItem.togglePriceAlert(current.id)
    }
  }

  fun assignRecipient(recipientId: String?) {
    val current = (uiState.value as? ProductDetailsUiState.Success)?.product ?: return
    viewModelScope.launch {
      manageWishlistItem.assignRecipient(current.id, recipientId, confidence = 98)
    }
  }

  fun assignSharedList(sharedListId: String?) {
    val current = (uiState.value as? ProductDetailsUiState.Success)?.product ?: return
    viewModelScope.launch {
      manageWishlistItem.assignSharedList(current.id, sharedListId)
    }
  }

  fun addComment(message: String) {
    val clean = message.trim()
    if (clean.isBlank()) return
    val current = (uiState.value as? ProductDetailsUiState.Success)?.product ?: return
    viewModelScope.launch {
      manageWishlistItem.addComment(current.id, clean)
    }
  }
}
