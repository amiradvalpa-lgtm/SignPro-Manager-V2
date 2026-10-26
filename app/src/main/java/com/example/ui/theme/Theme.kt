package com.example.ui.theme

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

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    primaryColorKey: String = "AMBER", // "AMBER", "BLUE", "CYAN", "EMERALD", "ROSE", "PURPLE"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val primaryColor = when (primaryColorKey) {
        "BLUE" -> PrimaryBlue
        "CYAN" -> PrimaryCyan
        "EMERALD" -> PrimaryEmerald
        "ROSE" -> PrimaryRose
        "PURPLE" -> PrimaryPurple
        else -> PrimaryAmber
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.Black,
            primaryContainer = primaryColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = primaryColor.copy(alpha = 0.8f),
            onSecondary = Color.Black,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurface.copy(alpha = 0.8f),
            outline = DarkOutline,
            outlineVariant = DarkOutline.copy(alpha = 0.5f)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = Color(0xFF1E293B),
            secondary = primaryColor.copy(alpha = 0.85f),
            onSecondary = Color.White,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurface.copy(alpha = 0.8f),
            outline = LightOutline,
            outlineVariant = LightOutline.copy(alpha = 0.5f)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    window.statusBarColor = colorScheme.surface.toArgb()
                    window.navigationBarColor = colorScheme.surface.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
                }
            } catch (_: Exception) {
                // Ignore any system insets / bar styling exceptions on Android 15 edge-to-edge
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
