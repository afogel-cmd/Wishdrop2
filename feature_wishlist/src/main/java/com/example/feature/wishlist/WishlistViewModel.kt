package com.example.feature.wishlist

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.DataResult
import com.example.core.result.asDataResult
import com.example.domain.model.Product
import com.example.domain.model.SharedListSummary
import com.example.domain.model.SmartCollection
import com.example.domain.usecase.ManageSharedListUseCase
import com.example.domain.usecase.ManageWishlistItemUseCase
import com.example.domain.usecase.ObserveWishlistCatalogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SwipeUndoType {
  PURCHASED,
  ARCHIVED,
}

@Immutable
data class PendingSwipeEvent(
  val productId: String,
  val productTitle: String,
  val type: SwipeUndoType,
)

@Immutable
sealed interface WishlistUiState {
  data object Loading : WishlistUiState

  @Immutable
  data class Content(
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val activeCollectionQuery: String? = null,
    val filterChips: List<String> = DEFAULT_FILTERS,
    val smartCollections: List<SmartCollection> = emptyList(),
    val sharedLists: List<SharedListSummary> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val swipeEvent: PendingSwipeEvent? = null,
  ) : WishlistUiState

  @Immutable
  data class Error(val message: String) : WishlistUiState

  companion object {
    val DEFAULT_FILTERS =
      listOf(
        "All",
        "Electronics",
        "Audio",
        "Google",
        "Pixel",
        "Gift",
        "Home",
        "Purchased",
        "Archived",
      )
  }
}

private data class WishlistFilterBundle(
  val query: String,
  val chip: String,
  val collectionFilter: String?,
  val swipe: PendingSwipeEvent?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WishlistViewModel
@Inject
constructor(
  observeWishlistCatalog: ObserveWishlistCatalogUseCase,
  private val manageWishlistItem: ManageWishlistItemUseCase,
  private val manageSharedList: ManageSharedListUseCase,
) : ViewModel() {

  private val searchQueryFlow = MutableStateFlow("")
  private val selectedFilterFlow = MutableStateFlow("All")
  private val collectionQueryFlow = MutableStateFlow<String?>(null)
  private val swipeEventFlow = MutableStateFlow<PendingSwipeEvent?>(null)
  private val retryFlow = MutableStateFlow(0)

  private val filterBundleFlow =
    combine(searchQueryFlow, selectedFilterFlow, collectionQueryFlow, swipeEventFlow) { query, chip, collection, swipe ->
      WishlistFilterBundle(query, chip, collection, swipe)
    }

  val uiState: StateFlow<WishlistUiState> =
    retryFlow
      .flatMapLatest {
        combine(
          observeWishlistCatalog().asDataResult(),
          manageSharedList.observeVisibleLists(),
          filterBundleFlow,
        ) { catalogResult, visibleLists, (query, chip, collectionFilter, swipe) ->
          when (catalogResult) {
            DataResult.Loading -> WishlistUiState.Loading
            is DataResult.Error -> WishlistUiState.Error(catalogResult.message)
            is DataResult.Success -> {
              val allProducts = catalogResult.data.allProducts
              val filtered =
                allProducts.filter { product ->
                  val statusMatches =
                    when (chip) {
                      "Purchased" -> product.isPurchased && !product.isArchived
                      "Archived" -> product.isArchived
                      else -> !product.isPurchased && !product.isArchived
                    }
                  val categoryOrTagMatches =
                    when (chip) {
                      "All",
                      "Purchased",
                      "Archived" -> true
                      else ->
                        product.categories.any { it.equals(chip, ignoreCase = true) } ||
                          product.tags.any { it.equals(chip, ignoreCase = true) }
                    }
                  val collectionMatches =
                    if (collectionFilter.isNullOrBlank()) {
                      true
                    } else {
                      product.categories.any { it.equals(collectionFilter, ignoreCase = true) } ||
                        product.tags.any { it.equals(collectionFilter, ignoreCase = true) } ||
                        product.recipientName?.equals(collectionFilter, ignoreCase = true) == true ||
                        product.collectionName.equals(collectionFilter, ignoreCase = true)
                    }
                  val textMatches =
                    if (query.isBlank()) {
                      true
                    } else {
                      val q = query.trim().lowercase()
                      product.title.lowercase().contains(q) ||
                        product.store.lowercase().contains(q) ||
                        product.categories.any { it.lowercase().contains(q) } ||
                        product.tags.any { it.lowercase().contains(q) } ||
                        (product.recipientName?.lowercase()?.contains(q) == true)
                    }
                  statusMatches && categoryOrTagMatches && collectionMatches && textMatches
                }

              WishlistUiState.Content(
                searchQuery = query,
                selectedFilter = chip,
                activeCollectionQuery = collectionFilter,
                smartCollections = catalogResult.data.smartCollections,
                sharedLists = visibleLists,
                filteredProducts = filtered,
                swipeEvent = swipe,
              )
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WishlistUiState.Loading,
      )

  fun retry() {
    retryFlow.update { it + 1 }
  }

  fun applyInitialArgs(category: String?, collectionQuery: String?) {
    if (!category.isNullOrBlank()) {
      selectedFilterFlow.value = category
    }
    if (!collectionQuery.isNullOrBlank()) {
      collectionQueryFlow.value = collectionQuery
    }
  }

  fun onSearchQueryChanged(query: String) {
    searchQueryFlow.value = query
  }

  fun onFilterSelected(filter: String) {
    selectedFilterFlow.value = filter
    if (filter == "All") {
      collectionQueryFlow.value = null
    }
  }

  fun clearAllFilters() {
    searchQueryFlow.value = ""
    selectedFilterFlow.value = "All"
    collectionQueryFlow.value = null
  }

  fun onSmartCollectionClicked(collectionQuery: String) {
    collectionQueryFlow.value =
      if (collectionQueryFlow.value == collectionQuery) null else collectionQuery
  }

  fun markPurchasedBySwipe(product: Product) {
    viewModelScope.launch {
      manageWishlistItem.setPurchased(product.id, true)
      swipeEventFlow.value =
        PendingSwipeEvent(product.id, product.title, SwipeUndoType.PURCHASED)
    }
  }

  fun markArchivedBySwipe(product: Product) {
    viewModelScope.launch {
      manageWishlistItem.setArchived(product.id, true)
      swipeEventFlow.value =
        PendingSwipeEvent(product.id, product.title, SwipeUndoType.ARCHIVED)
    }
  }

  fun undoSwipe(event: PendingSwipeEvent) {
    viewModelScope.launch {
      when (event.type) {
        SwipeUndoType.PURCHASED -> manageWishlistItem.setPurchased(event.productId, false)
        SwipeUndoType.ARCHIVED -> manageWishlistItem.setArchived(event.productId, false)
      }
      swipeEventFlow.value = null
    }
  }

  fun assignProductToSharedList(productId: String, sharedListId: String?) {
    viewModelScope.launch {
      manageWishlistItem.assignSharedList(productId, sharedListId)
    }
  }

  fun consumeSwipeEvent() {
    swipeEventFlow.value = null
  }
}
