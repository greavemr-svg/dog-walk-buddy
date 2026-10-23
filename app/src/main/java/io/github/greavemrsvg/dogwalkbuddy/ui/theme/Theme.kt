package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/** Minimum touch target (AC51). */
val MinTouchTarget = 48.dp

/** Minimum height of the full-width Start walk and Stop controls (AC51). */
val StartStopMinHeight = 72.dp

/**
 * The app theme. It follows the system theme, with no in-app setting (AC7). `MainActivity` handles `uiMode`
 * changes itself, so a switch only recomposes.
 */
@Composable
fun DogWalkBuddyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalFieldKitColors provides if (dark) DarkFieldKitColors else LightFieldKitColors,
        LocalRatingPalettes provides if (dark) DarkRatingPalettes else LightRatingPalettes,
        LocalRatingColors provides if (dark) DarkRatingColors else LightRatingColors,
        LocalFieldKitTypography provides DefaultFieldKitTypography,
        LocalRecordingPalette provides RecordingPalette,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = FieldKitMaterialTypography,
            shapes = FieldKitMaterialShapes,
            content = content,
        )
    }
}

/** Reads the Field Kit theme, for example `FieldKit.colors.textTertiary`. */
object FieldKit {
    val colors: FieldKitColors
        @Composable @ReadOnlyComposable
        get() = LocalFieldKitColors.current

    val ratingPalettes: RatingPalettes
        @Composable @ReadOnlyComposable
        get() = LocalRatingPalettes.current

    val typography: FieldKitTypography
        @Composable @ReadOnlyComposable
        get() = LocalFieldKitTypography.current

    val recording: RecordingColors
        @Composable @ReadOnlyComposable
        get() = LocalRecordingPalette.current
}
