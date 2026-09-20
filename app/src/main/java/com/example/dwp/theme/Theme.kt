package com.example.dwp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavySecondary,
    onPrimaryContainer = Color.White,
    secondary = Steel,
    onSecondary = Color.White,
    secondaryContainer = SteelLight,
    onSecondaryContainer = NavyPrimary,
    background = HullBackground,
    onBackground = TextDark,
    surface = SurfaceCard,
    onSurface = TextDark,
    surfaceVariant = SteelLight,
    onSurfaceVariant = TextMuted,
    outline = BorderLine,
    error = RedAlert,
    onError = Color.White,
    errorContainer = RedAlertBg,
    onErrorContainer = RedAlert
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FB2C2),
    onPrimary = NavyPrimary,
    primaryContainer = NavySecondary,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFC9D6E0),
    onSecondary = NavyPrimary,
    background = Color(0xFF0A1926),
    onBackground = Color(0xFFEDF1F4),
    surface = Color(0xFF0F2438),
    onSurface = Color(0xFFEDF1F4),
    surfaceVariant = Color(0xFF16324C),
    onSurfaceVariant = Color(0xFFC9D6E0),
    outline = Color(0xFF2C4157),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF680003)
)

@Composable
fun DwpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
