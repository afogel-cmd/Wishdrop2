package com.example.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.designsystem.R

val OutfitDisplayFamily =
  FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold),
  )

val PlusJakartaBodyFamily =
  FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
  )

/**
 * Pixel 2026 Expressive Typography Scale.
 * Uses tabular lining numerals ("tnum") on display/price styles to prevent layout shift.
 */
@Immutable
data class WishDropNumericTypography(
  val heroCurrency: TextStyle =
    TextStyle(
      fontFamily = OutfitDisplayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 42.sp,
      lineHeight = 48.sp,
      letterSpacing = (-0.75).sp,
      fontFeatureSettings = "tnum",
    ),
  val cardPrice: TextStyle =
    TextStyle(
      fontFamily = OutfitDisplayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 22.sp,
      lineHeight = 28.sp,
      letterSpacing = (-0.25).sp,
      fontFeatureSettings = "tnum",
    ),
  val pillMetric: TextStyle =
    TextStyle(
      fontFamily = PlusJakartaBodyFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.1.sp,
      fontFeatureSettings = "tnum",
    ),
  val priceHero: TextStyle = heroCurrency,
  val priceMedium: TextStyle = cardPrice,
  val heroCounter: TextStyle =
    TextStyle(
      fontFamily = OutfitDisplayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 36.sp,
      lineHeight = 42.sp,
      letterSpacing = (-0.5).sp,
      fontFeatureSettings = "tnum",
    ),
)

val LocalNumericTypography = staticCompositionLocalOf { WishDropNumericTypography() }

val WishDropTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = OutfitDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 50.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = "tnum",
      ),
    displayMedium =
      TextStyle(
        fontFamily = OutfitDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = "tnum",
      ),
    headlineLarge =
      TextStyle(
        fontFamily = OutfitDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.3).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = OutfitDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.15).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = OutfitDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = OutfitDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = PlusJakartaBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = PlusJakartaBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = PlusJakartaBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = PlusJakartaBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = PlusJakartaBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.25.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = PlusJakartaBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = PlusJakartaBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = PlusJakartaBodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.35.sp,
      ),
  )
