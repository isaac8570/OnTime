package com.OnTime.ontime.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkNeonColorScheme = darkColorScheme(
    primary = ElectricBlue,
    secondary = CyberPink,
    background = NearBlack,
    surface = DarkGray,
    onPrimary = NearBlack,
    onSecondary = NearBlack,
    onBackground = White,
    onSurface = White,
    surfaceVariant = DarkGray,
    onSurfaceVariant = LightGray
)

@Composable
fun OnTimeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkNeonColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
