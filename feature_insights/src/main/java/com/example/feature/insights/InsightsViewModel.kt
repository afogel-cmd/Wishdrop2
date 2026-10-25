package com.example.feature.insights

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.DataResult
import com.example.core.result.asDataResult
import com.example.domain.model.InsightsReport
import com.example.domain.usecase.ObserveInsightsReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@Immutable
sealed interface InsightsUiState {
  data object Loading : InsightsUiState

  data object Empty : InsightsUiState

  @Immutable
  data class Success(val report: InsightsReport) : InsightsUiState

  @Immutable
  data class Error(val message: String) : InsightsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel
@Inject
constructor(private val observeInsightsReport: ObserveInsightsReportUseCase) : ViewModel() {

  private val retryTrigger = MutableStateFlow(0)

  val uiState: StateFlow<InsightsUiState> =
    retryTrigger
      .flatMapLatest {
        observeInsightsReport().asDataResult().map { result ->
          when (result) {
            DataResult.Loading -> InsightsUiState.Loading
            is DataResult.Error -> InsightsUiState.Error(result.message)
            is DataResult.Success -> {
              val report = result.data
              if (
                report.categoryDistribution.isEmpty() &&
                  report.biggestPriceDrops.isEmpty() &&
                  report.commonTags.isEmpty()
              ) {
                InsightsUiState.Empty
              } else {
                InsightsUiState.Success(report)
              }
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState.Loading,
      )

  fun retry() {
    retryTrigger.update { it + 1 }
  }
}
