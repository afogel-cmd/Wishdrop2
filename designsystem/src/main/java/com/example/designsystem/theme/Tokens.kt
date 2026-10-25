package com.example.designsystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * WishDrop Shape System Tokens:
 * Small: 16dp | Medium: 24dp | Large: 32dp | ExtraLarge: 40dp
 */
val WishDropShapes =
  Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
  )

/**
 * WishDrop Spacing System Tokens (4dp - 48dp scale + adaptive layout bounds)
 */
@Immutable
data class WishDropSpacing(
  val xs: Dp = 4.dp,
  val sm: Dp = 8.dp,
  val md: Dp = 12.dp,
  val lg: Dp = 16.dp,
  val xl: Dp = 24.dp,
  val xxl: Dp = 32.dp,
  val xxxl: Dp = 48.dp,
  val maxContentWidth: Dp = 860.dp,
  val maxSheetWidth: Dp = 680.dp,
  val minTouchTarget: Dp = 48.dp,
  val hairlineBorder: Dp = 1.dp,
)

@Immutable
data class WishDropRadiusTokens(
  val small: CornerBasedShape = RoundedCornerShape(16.dp),
  val medium: CornerBasedShape = RoundedCornerShape(24.dp),
  val large: CornerBasedShape = RoundedCornerShape(32.dp),
  val extraLarge: CornerBasedShape = RoundedCornerShape(40.dp),
)

val LocalSpacing = staticCompositionLocalOf { WishDropSpacing() }
val LocalRadiusTokens = staticCompositionLocalOf { WishDropRadiusTokens() }
