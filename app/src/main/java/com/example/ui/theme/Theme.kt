package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = BgDark,
    primaryContainer = BgCardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = AccentPurple,
    onSecondary = TextPrimary,
    secondaryContainer = BgCard,
    onSecondaryContainer = TextSecondary,
    tertiary = AccentGold,
    onTertiary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = BorderHighlight,
    error = AccentRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
