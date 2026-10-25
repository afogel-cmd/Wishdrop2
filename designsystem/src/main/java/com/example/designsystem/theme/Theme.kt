package com.example.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.example.core.preferences.ThemeMode

@Composable
fun WishDropTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  useDynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val darkTheme =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }

  val context = LocalContext.current
  val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val colorScheme =
    remember(darkTheme, useDynamicColor, context) {
      when {
        useDynamicColor && dynamicSupported -> {
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WishDropDarkColorScheme
        else -> WishDropLightColorScheme
      }
    }

  // Harmonize Hero & Chart tokens with Dynamic Color when enabled while keeping semantic contrast
  val extendedColors =
    remember(darkTheme, useDynamicColor, colorScheme) {
      val base = if (darkTheme) DarkExtendedColors else LightExtendedColors
      if (useDynamicColor && dynamicSupported) {
        val heroAnchor = if (darkTheme) Color(0xFF1A1830) else Color(0xFF282068)
        base.copy(
          heroStart = lerp(colorScheme.primary, heroAnchor, if (darkTheme) 0.45f else 0.18f),
          heroMiddle = lerp(colorScheme.tertiary, heroAnchor, if (darkTheme) 0.45f else 0.22f),
          heroEnd = lerp(colorScheme.secondary, heroAnchor, if (darkTheme) 0.40f else 0.20f),
          onHero = Color(0xFFFFFFFF),
        )
      } else {
        base
      }
    }

  CompositionLocalProvider(
    LocalExtendedColors provides extendedColors,
    LocalSpacing provides WishDropSpacing(),
    LocalRadiusTokens provides WishDropRadiusTokens(),
    LocalNumericTypography provides WishDropNumericTypography(),
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = WishDropTypography,
      shapes = WishDropShapes,
      content = content,
    )
  }
}

object WishDropDesign {
  val colors: WishDropExtendedColors
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

  val spacing: WishDropSpacing
    @Composable @ReadOnlyComposable get() = LocalSpacing.current

  val radii: WishDropRadiusTokens
    @Composable @ReadOnlyComposable get() = LocalRadiusTokens.current

  val numericTypography: WishDropNumericTypography
    @Composable @ReadOnlyComposable get() = LocalNumericTypography.current
}
