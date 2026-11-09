package com.viniciusrio.finance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Cores com significado financeiro, sempre acompanhadas de sinal ou texto. */
@Immutable
data class SemanticColors(val income: Color, val expense: Color, val warning: Color, val transfer: Color)

val LocalSemantic = staticCompositionLocalOf { SemanticColors(Color.Green, Color.Red, Color.Yellow, Color.Gray) }

private val Light = lightColorScheme(
    primary = Color(0xFF2F5D50), onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE9DD), onPrimaryContainer = Color(0xFF0B2A21),
    secondary = Color(0xFF4F6358), secondaryContainer = Color(0xFFDDE8E1),
    tertiary = Color(0xFF6B5B95), tertiaryContainer = Color(0xFFE9DDFF),
    background = Color(0xFFF7F7FB), surface = Color(0xFFF7F7FB),
    surfaceContainer = Color(0xFFEDEFEE), surfaceContainerHigh = Color(0xFFE5E8E6),
    surfaceContainerLow = Color(0xFFF1F3F2),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9BD3BE), onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF1C4A3D), onPrimaryContainer = Color(0xFFCDE9DD),
    secondary = Color(0xFFB4CCBF), secondaryContainer = Color(0xFF364B40),
    tertiary = Color(0xFFCFBDFE), tertiaryContainer = Color(0xFF4B3F72),
    background = Color(0xFF111413), surface = Color(0xFF111413),
    surfaceContainer = Color(0xFF1D201F), surfaceContainerHigh = Color(0xFF272B29),
    surfaceContainerLow = Color(0xFF191C1B),
    error = Color(0xFFFFB4AB),
)

private val type = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

val Tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val semantic = if (dark) {
        SemanticColors(Color(0xFF7DD9A8), Color(0xFFFF8A80), Color(0xFFFFC857), Color(0xFF9FB8C9))
    } else {
        SemanticColors(Color(0xFF1B7A4B), Color(0xFFC0392B), Color(0xFF9A6700), Color(0xFF4D6B80))
    }
    CompositionLocalProvider(LocalSemantic provides semantic) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = type, content = content)
    }
}
