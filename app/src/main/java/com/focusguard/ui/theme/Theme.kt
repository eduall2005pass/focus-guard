package com.focusguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GuardGreen,
    onPrimary = DarkBackground,
    primaryContainer = GuardGreenContainer,
    onPrimaryContainer = GuardGreenText,
    secondary = AccentBlue,
    onSecondary = TextPrimary,
    secondaryContainer = AccentBlueContainer,
    onSecondaryContainer = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = GuardRed,
    errorContainer = GuardRedContainer,
    onError = TextPrimary,
    onErrorContainer = GuardRedText
)

@Composable
fun FocusGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
