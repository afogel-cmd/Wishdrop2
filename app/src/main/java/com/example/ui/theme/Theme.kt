package com.example.ui.theme

import androidx.compose.runtime.Composable
import com.example.core.preferences.ThemeMode
import com.example.designsystem.theme.WishDropTheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  WishDropTheme(
    themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
    useDynamicColor = dynamicColor,
    content = content,
  )
}
