package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat

/**
 * Recording is always dark (AC4, AC33), so its status and navigation bar icons are light whatever the system theme.
 * `enableEdgeToEdge` picks the icon style from the system theme, and `MainActivity` calls it again on a theme
 * switch, so the style is set again whenever the night mode changes. Leaving Recording calls `enableEdgeToEdge`,
 * which restores the style that follows the system theme.
 */
@Composable
fun RecordingSystemBars() {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val uiMode = LocalConfiguration.current.uiMode
    DisposableEffect(activity, uiMode) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose { activity.enableEdgeToEdge() }
    }
}
