package com.example.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.preferences.UserPreferencesRepository
import com.example.core.result.DataResult
import com.example.core.result.asDataResult
import com.example.domain.model.DashboardSummary
import com.example.domain.model.GiftRecipient
import com.example.domain.model.ListVisibility
import com.example.domain.usecase.ManageGiftPlanUseCase
import com.example.domain.usecase.ManageSharedListUseCase
import com.example.domain.usecase.ObserveDashboardUseCase
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

@Immutable
sealed interface HomeUiState {
  data object Loading : HomeUiState

  @Immutable
  data class Success(
    val userDisplayName: String,
    val summary: DashboardSummary,
    val familyRecipients: List<GiftRecipient> = emptyList(),
  ) : HomeUiState

  @Immutable
  data class Empty(val userDisplayName: String) : HomeUiState

  @Immutable
  data class Error(val message: String) : HomeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
@Inject
constructor(
  observeDashboard: ObserveDashboardUseCase,
  preferencesRepository: UserPreferencesRepository,
  manageGiftPlan: ManageGiftPlanUseCase,
  private val manageSharedList: ManageSharedListUseCase,
) : ViewModel() {

  private val retryTrigger = MutableStateFlow(0)

  val uiState: StateFlow<HomeUiState> =
    retryTrigger
      .flatMapLatest {
        combine(
          observeDashboard().asDataResult(),
          preferencesRepository.preferencesFlow,
          manageGiftPlan.observeRecipients(),
        ) { result, prefs, recipients ->
          when (result) {
            DataResult.Loading -> HomeUiState.Loading
            is DataResult.Error -> HomeUiState.Error(result.message)
            is DataResult.Success -> {
              val summary = result.data
              if (summary.activeWishesCount == 0 && summary.recommendations.isEmpty()) {
                HomeUiState.Empty(userDisplayName = prefs.displayName)
              } else {
                HomeUiState.Success(
                  userDisplayName = prefs.displayName,
                  summary = summary,
                  familyRecipients = recipients,
                )
              }
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
      )

  fun retry() {
    retryTrigger.update { it + 1 }
  }

  fun createSharedList(
    title: String,
    emoji: String,
    subtitle: String,
    hiddenFromMemberId: String?,
    onCreated: (String) -> Unit,
  ) {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return
    viewModelScope.launch {
      val visibility =
        if (!hiddenFromMemberId.isNullOrBlank()) {
          ListVisibility.HIDDEN
        } else {
          ListVisibility.SHARED
        }
      val listId =
        manageSharedList.createList(
          title = cleanTitle,
          emoji = emoji.trim().ifBlank { "🎁" },
          subtitle = subtitle.trim().ifBlank { "Shared family wishlist" },
          visibility = visibility,
          hiddenFromMemberId = hiddenFromMemberId,
        )
      onCreated(listId)
    }
  }
}
