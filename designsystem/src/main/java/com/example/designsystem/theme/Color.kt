package com.example.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Fallback Seed Color: #6C63FF (Material You Expressive Iris)
val SeedIris = Color(0xFF6C63FF)

val LightPrimary = Color(0xFF554AF0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE3DFFF)
val LightOnPrimaryContainer = Color(0xFF150067)

val LightSecondary = Color(0xFF006B5E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFA2F2E2)
val LightOnSecondaryContainer = Color(0xFF00201B)

val LightTertiary = Color(0xFF984061)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFD9E2)
val LightOnTertiaryContainer = Color(0xFF3E001D)

val LightBackground = Color(0xFFFAF8FF)
val LightOnBackground = Color(0xFF1B1B23)
val LightSurface = Color(0xFFFAF8FF)
val LightOnSurface = Color(0xFF1B1B23)
val LightSurfaceVariant = Color(0xFFE5E1F2)
val LightOnSurfaceVariant = Color(0xFF474654)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF4F1FD)
val LightSurfaceContainer = Color(0xFFEEEBF8)
val LightSurfaceContainerHigh = Color(0xFFE8E5F3)
val LightSurfaceContainerHighest = Color(0xFFE2DFED)
val LightOutline = Color(0xFF787686)
val LightOutlineVariant = Color(0xFFC8C5D6)

val DarkPrimary = Color(0xFFC6C0FF)
val DarkOnPrimary = Color(0xFF26179A)
val DarkPrimaryContainer = Color(0xFF3E31B8)
val DarkOnPrimaryContainer = Color(0xFFE3DFFF)

val DarkSecondary = Color(0xFF86D6C6)
val DarkOnSecondary = Color(0xFF003730)
val DarkSecondaryContainer = Color(0xFF005046)
val DarkOnSecondaryContainer = Color(0xFFA2F2E2)

val DarkTertiary = Color(0xFFFFB0C8)
val DarkOnTertiary = Color(0xFF5E1133)
val DarkTertiaryContainer = Color(0xFF7B2949)
val DarkOnTertiaryContainer = Color(0xFFFFD9E2)

val DarkBackground = Color(0xFF12121A)
val DarkOnBackground = Color(0xFFE4E1EE)
val DarkSurface = Color(0xFF12121A)
val DarkOnSurface = Color(0xFFE4E1EE)
val DarkSurfaceVariant = Color(0xFF474654)
val DarkOnSurfaceVariant = Color(0xFFC8C5D6)
val DarkSurfaceContainerLowest = Color(0xFF0D0D14)
val DarkSurfaceContainerLow = Color(0xFF1A1924)
val DarkSurfaceContainer = Color(0xFF1F1E2B)
val DarkSurfaceContainerHigh = Color(0xFF292836)
val DarkSurfaceContainerHighest = Color(0xFF343342)
val DarkOutline = Color(0xFF928FA0)
val DarkOutlineVariant = Color(0xFF474654)

val WishDropLightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
  )

val WishDropDarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
  )

@Immutable
data class WishDropExtendedColors(
  val savingsContainer: Color,
  val onSavingsContainer: Color,
  val coralContainer: Color,
  val onCoralContainer: Color,
  val amberContainer: Color,
  val onAmberContainer: Color,
  val skyContainer: Color,
  val onSkyContainer: Color,
  val heroStart: Color,
  val heroMiddle: Color,
  val heroEnd: Color,
  val onHero: Color,
  val chartPalette: List<Color>,
)

val LightExtendedColors =
  WishDropExtendedColors(
    savingsContainer = Color(0xFFD2F8E5),
    onSavingsContainer = Color(0xFF054E32),
    coralContainer = Color(0xFFFFDFD8),
    onCoralContainer = Color(0xFF641E11),
    amberContainer = Color(0xFFFFEAB8),
    onAmberContainer = Color(0xFF533B00),
    skyContainer = Color(0xFFD9E9FF),
    onSkyContainer = Color(0xFF0A3666),
    heroStart = Color(0xFF594EF4),
    heroMiddle = Color(0xFF7B5BF2),
    heroEnd = Color(0xFF2B8C83),
    onHero = Color(0xFFFFFFFF),
    chartPalette =
      listOf(
        Color(0xFF6C63FF),
        Color(0xFFFF6F91),
        Color(0xFF00B894),
        Color(0xFFFFB142),
      ),
  )

val DarkExtendedColors =
  WishDropExtendedColors(
    savingsContainer = Color(0xFF0E4D36),
    onSavingsContainer = Color(0xFF9DF5CA),
    coralContainer = Color(0xFF5E251A),
    onCoralContainer = Color(0xFFFFDAD2),
    amberContainer = Color(0xFF523C05),
    onAmberContainer = Color(0xFFFFDF94),
    skyContainer = Color(0xFF143A66),
    onSkyContainer = Color(0xFFD4E5FF),
    heroStart = Color(0xFF392FA6),
    heroMiddle = Color(0xFF56349E),
    heroEnd = Color(0xFF135C56),
    onHero = Color(0xFFF6F4FF),
    chartPalette =
      listOf(
        Color(0xFFA29BFE),
        Color(0xFFFF8FAB),
        Color(0xFF55EFC4),
        Color(0xFFFFD166),
      ),
  )

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
