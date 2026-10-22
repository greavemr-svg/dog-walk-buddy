package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The Material 3 scheme of the Recording screen, built from [RecordingPalette] only. It does not depend on the
 * system theme, so Recording looks the same in light and dark (AC4).
 */
val RecordingColorScheme: ColorScheme =
    darkColorScheme(
        primary = RecordingPalette.stopBackground,
        onPrimary = RecordingPalette.stopForeground,
        primaryContainer = RecordingPalette.card,
        onPrimaryContainer = RecordingPalette.onDark,
        secondary = RecordingPalette.route,
        onSecondary = RecordingPalette.stopForeground,
        secondaryContainer = RecordingPalette.card,
        onSecondaryContainer = RecordingPalette.onDark,
        tertiary = RecordingPalette.route,
        onTertiary = RecordingPalette.stopForeground,
        background = RecordingPalette.background,
        onBackground = RecordingPalette.onDark,
        surface = RecordingPalette.background,
        onSurface = RecordingPalette.onDark,
        surfaceVariant = RecordingPalette.card,
        onSurfaceVariant = RecordingPalette.onDarkMuted,
        surfaceTint = RecordingPalette.background,
        inverseSurface = RecordingPalette.onDark,
        inverseOnSurface = RecordingPalette.background,
        // Control borders need 3:1, which the decorative panel border does not give.
        outline = RecordingPalette.onDarkMuted,
        outlineVariant = RecordingPalette.mapPanelBorder,
        surfaceBright = RecordingPalette.card,
        surfaceDim = RecordingPalette.background,
        surfaceContainer = RecordingPalette.card,
        surfaceContainerHigh = RecordingPalette.card,
        surfaceContainerHighest = RecordingPalette.card,
        surfaceContainerLow = RecordingPalette.card,
        surfaceContainerLowest = RecordingPalette.background,
    )

/** Wraps the Recording screen in its always-dark palette, whatever the system theme (AC4, ADR 0007). */
@Composable
fun RecordingTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRecordingPalette provides RecordingPalette) {
        MaterialTheme(
            colorScheme = RecordingColorScheme,
            typography = FieldKitMaterialTypography,
            shapes = FieldKitMaterialShapes,
            content = content,
        )
    }
}
