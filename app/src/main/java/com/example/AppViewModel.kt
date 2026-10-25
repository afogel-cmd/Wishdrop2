package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.preferences.UserPreferences
import com.example.core.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppViewModel @Inject constructor(preferencesRepository: UserPreferencesRepository) :
  ViewModel() {

  val preferences: StateFlow<UserPreferences> =
    preferencesRepository.preferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = UserPreferences(),
    )
}
