package com.aurora.player.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AuroraDarkColorScheme = darkColorScheme(
    primary = AuroraOrange,
    onPrimary = AuroraBlack,
    primaryContainer = AuroraDarkSurfaceVariant,
    onPrimaryContainer = AuroraOrange,
    secondary = AuroraOrangeGlow,
    onSecondary = AuroraBlack,
    secondaryContainer = AuroraDarkSurfaceVariant,
    onSecondaryContainer = AuroraOrangeGlow,
    background = AuroraDarkBackground,
    onBackground = AuroraTextPrimary,
    surface = AuroraDarkSurface,
    onSurface = AuroraTextPrimary,
    surfaceVariant = AuroraDarkSurfaceVariant,
    onSurfaceVariant = AuroraTextSecondary,
    outline = AuroraBorder,
    error = AuroraError
)

@Composable
fun AuroraPlayerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = AuroraDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
