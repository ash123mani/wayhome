package com.wayhome.presentation.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun scheme(c: WayHomeColors): ColorScheme = lightColorScheme(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.accentSoft,
    onPrimaryContainer = c.onAccentSoft,
    secondary = c.dusk,
    onSecondary = c.onAccent,
    background = c.background,
    onBackground = c.onSurface,
    surface = c.surface,
    onSurface = c.onSurface,
    surfaceVariant = c.surfaceMuted,
    onSurfaceVariant = c.onSurfaceVariant,
    outline = c.outlineStrong,
    outlineVariant = c.outline,
    error = c.sea,
    onError = c.onAccent
)

private fun darkScheme(c: WayHomeColors): ColorScheme = darkColorScheme(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.accentSoft,
    onPrimaryContainer = c.onAccentSoft,
    secondary = c.dusk,
    onSecondary = c.onGlow,
    background = c.background,
    onBackground = c.onSurface,
    surface = c.surface,
    onSurface = c.onSurface,
    surfaceVariant = c.surfaceMuted,
    onSurfaceVariant = c.onSurfaceVariant,
    outline = c.outlineStrong,
    outlineVariant = c.outline,
    error = c.sea,
    onError = c.onGlow
)

@Composable
fun WayHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) WayHomeDarkColors else WayHomeLightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(LocalWayHomeColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme(colors) else scheme(colors),
            typography = WayHomeType,
            shapes = WayHomeShapes,
            content = content
        )
    }
}

/** Shorthand: `WayHome.colors.accent` inside any composable. */
object WayHome {
    val colors: WayHomeColors
        @Composable get() = LocalWayHomeColors.current
    val isDark: Boolean
        @Composable get() = LocalWayHomeColors.current.background.luminanceIsDark()
}

private fun androidx.compose.ui.graphics.Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f
