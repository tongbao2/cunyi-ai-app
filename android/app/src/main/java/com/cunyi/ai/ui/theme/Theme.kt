package com.cunyi.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 村医主题色：中医草木绿
val CunyiGreen = Color(0xFF2E7D5F)
val CunyiGreenDark = Color(0xFF1B5E3A)
val CunyiGreenLight = Color(0xFF4CAF82)
val CunyiGreenBackground = Color(0xFFF5F9F7)

private val LightColorScheme = lightColorScheme(
    primary = CunyiGreen,
    onPrimary = Color.White,
    primaryContainer = CunyiGreenLight,
    onPrimaryContainer = CunyiGreenDark,
    secondary = Color(0xFF6D9E84),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0F4F1),
    background = CunyiGreenBackground,
    onBackground = Color(0xFF1C1B1F),
    error = Color(0xFFBA1A1A)
)

private val DarkColorScheme = darkColorScheme(
    primary = CunyiGreenLight,
    onPrimary = CunyiGreenDark,
    primaryContainer = CunyiGreenDark,
    onPrimaryContainer = CunyiGreenLight,
    secondary = Color(0xFF8ABFA5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2C3E36),
    background = Color(0xFF111916),
    onBackground = Color(0xFFE6E1E5),
    error = Color(0xFFFFB4AB)
)

@Composable
fun CunyiAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CunyiGreen.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
