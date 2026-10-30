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

      val locale = androidx.compose.runtime.remember(preferences.appLanguage) {
        java.util.Locale.forLanguageTag(preferences.appLanguage.code)
      }

      val configuration = androidx.compose.ui.platform.LocalConfiguration.current
      val updatedConfiguration = androidx.compose.runtime.remember(configuration, locale) {
        android.content.res.Configuration(configuration).apply {
          setLocale(locale)
          setLayoutDirection(locale)
        }
      }

      val activity = this
      val localizedContext = androidx.compose.runtime.remember(activity, updatedConfiguration) {
        object : android.content.ContextWrapper(activity) {
          private val localizedRes = activity.createConfigurationContext(updatedConfiguration).resources
          override fun getResources(): android.content.res.Resources = localizedRes
        }
      }

      androidx.compose.runtime.LaunchedEffect(preferences.appLanguage) {
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
          val localeManager = getSystemService(android.app.LocaleManager::class.java)
          localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(preferences.appLanguage.code)
        }
      }

      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalConfiguration provides updatedConfiguration,
        androidx.compose.ui.platform.LocalContext provides localizedContext,
      ) {
        WishDropTheme(
          themeMode = preferences.themeMode,
          useDynamicColor = preferences.useDynamicColor,
        ) {
          WishDropAppShell(widthSizeClass = windowSizeClass.widthSizeClass)
        }
      }
    }
  }
}
