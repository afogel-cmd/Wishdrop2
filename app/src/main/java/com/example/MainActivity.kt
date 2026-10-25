package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.preferences.ThemeMode
import com.example.designsystem.theme.WishDropTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val appViewModel: AppViewModel = hiltViewModel()
      val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val isDarkTheme =
        when (preferences.themeMode) {
          ThemeMode.SYSTEM -> systemDark
          ThemeMode.LIGHT -> false
          ThemeMode.DARK -> true
        }

      DisposableEffect(isDarkTheme) {
        enableEdgeToEdge(
          statusBarStyle =
            if (isDarkTheme) {
              SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
              SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
          navigationBarStyle =
            if (isDarkTheme) {
              SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
              SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
        )
        onDispose {}
      }

      val windowSizeClass = calculateWindowSizeClass(this)

      WishDropTheme(
        themeMode = preferences.themeMode,
        useDynamicColor = preferences.useDynamicColor,
      ) {
        WishDropAppShell(widthSizeClass = windowSizeClass.widthSizeClass)
      }
    }
  }
}
