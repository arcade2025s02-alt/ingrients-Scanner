package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SemanticRiskColors(
    val excellent: Color,
    val excellentBg: Color,
    val good: Color,
    val goodBg: Color,
    val moderate: Color,
    val moderateBg: Color,
    val bad: Color,
    val badBg: Color,
    val cardBorder: Color,
    val softEmeraldSurface: Color
)

private val LightSemanticColors = SemanticRiskColors(
    excellent = RiskExcellent,
    excellentBg = RiskExcellentBg,
    good = RiskGood,
    goodBg = RiskGoodBg,
    moderate = RiskModerate,
    moderateBg = RiskModerateBg,
    bad = RiskBad,
    badBg = RiskBadBg,
    cardBorder = SlateBorder,
    softEmeraldSurface = EmeraldSoftSurface
)

private val DarkSemanticColors = SemanticRiskColors(
    excellent = Color(0xFF4ADE80),
    excellentBg = Color(0xFF14532D),
    good = Color(0xFF86EFAC),
    goodBg = Color(0xFF1A3A24),
    moderate = Color(0xFFFBBF24),
    moderateBg = Color(0xFF451A03),
    bad = Color(0xFFF87171),
    badBg = Color(0xFF450A0A),
    cardBorder = DarkBorder,
    softEmeraldSurface = Color(0xFF132A1E)
)

val LocalSemanticRiskColors = staticCompositionLocalOf { LightSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = EmeraldPrimaryDark,
    onSecondary = Color.White,
    secondaryContainer = SlateSurfaceVariant,
    onSecondaryContainer = SlateInk,
    tertiary = RiskModerate,
    onTertiary = Color.White,
    background = SlateBackground,
    onBackground = SlateInk,
    surface = SurfaceWhite,
    onSurface = SlateInk,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateSecondaryText,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = RiskBad,
    onError = Color.White,
    errorContainer = RiskBadBg,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Color(0xFF86EFAC),
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun SmartRiskAnalyzerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalSemanticRiskColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
