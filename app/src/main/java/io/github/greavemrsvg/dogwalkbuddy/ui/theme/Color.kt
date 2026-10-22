package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The Field Kit colour roles (ADR 0007). Each property is named as its token in the UX pack's `tokens.json`
 * (`color.light` / `color.dark`) and holds exactly its value (AC1, AC2). `summaryCaption` is derived (AC40).
 */
@Immutable
data class FieldKitColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val primaryContainer: Color,
    val navIndicator: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val textTertiary: Color,
    val iconMuted: Color,
    val outline: Color,
    val outlineVariant: Color,
    val divider: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryPressed: Color,
    val error: Color,
    val onError: Color,
    val errorText: Color,
    val errorOutline: Color,
    val scrim: Color,
    /** Captions and the week range on the this-week card, whose fill is [primary] (AC40). */
    val summaryCaption: Color,
) {
    /**
     * Borders that identify a control (text inputs, outlined buttons). The token [outline] is below 3:1 on
     * [surface], so control borders use [iconMuted] instead (ADR 0007, MVP AC51).
     */
    val controlBorder: Color get() = iconMuted
}

val LightFieldKitColors =
    FieldKitColors(
        background = Color(0xFFF2F5F9),
        surface = Color(0xFFFFFFFF),
        surfaceMuted = Color(0xFFE5EBF2),
        primaryContainer = Color(0xFFE1EBF7),
        navIndicator = Color(0xFFDCE8F7),
        onSurface = Color(0xFF0F1B2A),
        onSurfaceVariant = Color(0xFF3E4A5A),
        textTertiary = Color(0xFF4A5668),
        iconMuted = Color(0xFF5A6678),
        outline = Color(0xFFCDD7E3),
        outlineVariant = Color(0xFFDFE6EE),
        divider = Color(0xFFEBF0F5),
        primary = Color(0xFF1B4F8F),
        onPrimary = Color(0xFFFFFFFF),
        primaryPressed = Color(0xFF0F2E57),
        error = Color(0xFF9E2A17),
        onError = Color(0xFFFFFFFF),
        errorText = Color(0xFF8E2414),
        errorOutline = Color(0xFFE2B4AB),
        // rgba(8,16,28,0.55): 0.55 x 255 = 140.
        scrim = Color(red = 8, green = 16, blue = 28, alpha = 140),
        // Pack section 4.5.
        summaryCaption = Color(0xFFCFE0F5),
    )

val DarkFieldKitColors =
    FieldKitColors(
        background = Color(0xFF0B1622),
        surface = Color(0xFF132233),
        surfaceMuted = Color(0xFF1A2A3D),
        primaryContainer = Color(0xFF1E3A5F),
        navIndicator = Color(0xFF1E3A5F),
        onSurface = Color(0xFFF1F4F8),
        onSurfaceVariant = Color(0xFFC8D3E0),
        textTertiary = Color(0xFFA9B6C6),
        iconMuted = Color(0xFF94A3B8),
        outline = Color(0xFF4A5E78),
        outlineVariant = Color(0xFF22364D),
        divider = Color(0xFF1D2E43),
        primary = Color(0xFF8CC2FF),
        onPrimary = Color(0xFF0B2747),
        primaryPressed = Color(0xFFD0E4FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorText = Color(0xFFFFB4AB),
        errorOutline = Color(0xFF7A3328),
        // rgba(0,0,0,0.6): 0.6 x 255 = 153.
        scrim = Color(red = 0, green = 0, blue = 0, alpha = 153),
        // The pack's #CFE0F5 is about 1.4:1 on the dark primary, so the primaryContainer role is used (AC40).
        summaryCaption = Color(0xFF1E3A5F),
    )

/** The factor-cell highlight behind the driving factor of a Caution or Avoid result (AC17). */
@Immutable
data class DriverHighlight(
    val background: Color,
    val ring: Color,
)

/** One rating's colours, exactly `color.rating` (light) or `color.dark.rating` (dark) in `tokens.json` (AC3). */
@Immutable
data class RatingPalette(
    val container: Color,
    val onContainer: Color,
    val onContainerMuted: Color,
    val badge: Color,
    val onBadge: Color,
    val divider: Color,
    val bar: Color,
    /** Null for Go: a Go result never highlights a factor cell. */
    val driverHighlight: DriverHighlight?,
)

@Immutable
data class RatingPalettes(
    val go: RatingPalette,
    val caution: RatingPalette,
    val avoid: RatingPalette,
)

val LightRatingPalettes =
    RatingPalettes(
        go =
            RatingPalette(
                container = Color(0xFFD8F0DF),
                onContainer = Color(0xFF0B3B21),
                onContainerMuted = Color(0xFF1F4D33),
                badge = Color(0xFF13723F),
                onBadge = Color(0xFFFFFFFF),
                divider = Color(0xFFA9D6B6),
                bar = Color(0xFF13723F),
                driverHighlight = null,
            ),
        caution =
            RatingPalette(
                container = Color(0xFFFDECC8),
                onContainer = Color(0xFF4A2C00),
                onContainerMuted = Color(0xFF5A3A0A),
                badge = Color(0xFF8A5300),
                onBadge = Color(0xFFFFFFFF),
                divider = Color(0xFFE9C887),
                bar = Color(0xFFC27A06),
                driverHighlight = DriverHighlight(background = Color(0xFFFFF7E6), ring = Color(0xFF8A5300)),
            ),
        avoid =
            RatingPalette(
                container = Color(0xFFF9DAD4),
                onContainer = Color(0xFF561008),
                onContainerMuted = Color(0xFF6A2017),
                badge = Color(0xFF9E2A17),
                onBadge = Color(0xFFFFFFFF),
                divider = Color(0xFFEDB1A6),
                bar = Color(0xFFC2361F),
                driverHighlight = DriverHighlight(background = Color(0xFFFFF1EE), ring = Color(0xFF9E2A17)),
            ),
    )

val DarkRatingPalettes =
    RatingPalettes(
        go =
            RatingPalette(
                container = Color(0xFF12301F),
                onContainer = Color(0xFFC4EDCF),
                onContainerMuted = Color(0xFF9FD3AE),
                badge = Color(0xFF2E8A50),
                onBadge = Color(0xFFFFFFFF),
                divider = Color(0xFF2B5A3B),
                bar = Color(0xFF5CC283),
                driverHighlight = null,
            ),
        caution =
            RatingPalette(
                container = Color(0xFF3A2A0A),
                onContainer = Color(0xFFFFDDA3),
                onContainerMuted = Color(0xFFEBC27E),
                badge = Color(0xFF9A6510),
                onBadge = Color(0xFFFFFFFF),
                divider = Color(0xFF6A4D1A),
                bar = Color(0xFFF0A83A),
                driverHighlight = DriverHighlight(background = Color(0xFF4A3712), ring = Color(0xFFFFDDA3)),
            ),
        avoid =
            RatingPalette(
                container = Color(0xFF3E1712),
                onContainer = Color(0xFFFFC9C0),
                onContainerMuted = Color(0xFFF0A79B),
                badge = Color(0xFFB33A26),
                onBadge = Color(0xFFFFFFFF),
                divider = Color(0xFF6E2E25),
                bar = Color(0xFFFF7A66),
                driverHighlight = DriverHighlight(background = Color(0xFF4F1F18), ring = Color(0xFFFFC9C0)),
            ),
    )

/** The always-dark Recording palette, exactly `color.recording` in `tokens.json`, in both system themes (AC4). */
@Immutable
data class RecordingColors(
    val background: Color,
    val card: Color,
    val mapPanel: Color,
    val mapPanelBorder: Color,
    val onDark: Color,
    val onDarkMuted: Color,
    val route: Color,
    val routeCasing: Color,
    val liveDot: Color,
    val stopBackground: Color,
    val stopForeground: Color,
)

val RecordingPalette =
    RecordingColors(
        background = Color(0xFF0B1622),
        card = Color(0xFF1A2A3D),
        mapPanel = Color(0xFF132233),
        mapPanelBorder = Color(0xFF22364D),
        onDark = Color(0xFFFFFFFF),
        onDarkMuted = Color(0xFFC8D3E0),
        route = Color(0xFF8CC2FF),
        routeCasing = Color(0xFF2D4A6B),
        liveDot = Color(0xFFFF6B57),
        stopBackground = Color(0xFFF2F5F9),
        stopForeground = Color(0xFF0B1622),
    )

/** Material 3 roles derived from the Field Kit roles, the same role-for-role swap in light and dark (ADR 0007). */
private fun FieldKitColors.toColorScheme(dark: Boolean): ColorScheme {
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onSurface,
        inversePrimary = primaryContainer,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = navIndicator,
        onSecondaryContainer = primaryPressed,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = primaryContainer,
        onTertiaryContainer = onSurface,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        error = error,
        onError = onError,
        // Accessibility override: Material draws control borders with outline, which needs 3:1 (MVP AC51).
        outline = controlBorder,
        outlineVariant = outlineVariant,
        // The sheet applies its own alpha to the scrim.
        scrim = scrim.copy(alpha = 1f),
        surfaceBright = surface,
        surfaceDim = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        surfaceContainerLow = surface,
        surfaceContainerLowest = surface,
    )
}

/** The Material 3 scheme of the light theme. Text and border contrast is checked by `ContrastTest` (MVP AC51, AC6). */
val LightColors: ColorScheme = LightFieldKitColors.toColorScheme(dark = false)

/** The Material 3 scheme of the dark theme. */
val DarkColors: ColorScheme = DarkFieldKitColors.toColorScheme(dark = true)

/** The colour of the rating label and icon. Colour is never the only difference (AC53): text and icon differ too. */
data class RatingColors(
    val go: Color,
    val caution: Color,
    val avoid: Color,
)

/** Rating-coloured text on a plain surface uses each rating's `onContainer` (ADR 0007). */
private fun RatingPalettes.toRatingColors() = RatingColors(go = go.onContainer, caution = caution.onContainer, avoid = avoid.onContainer)

val LightRatingColors = LightRatingPalettes.toRatingColors()

val DarkRatingColors = DarkRatingPalettes.toRatingColors()

val LocalRatingColors = staticCompositionLocalOf { LightRatingColors }

val LocalFieldKitColors = staticCompositionLocalOf { LightFieldKitColors }

val LocalRatingPalettes = staticCompositionLocalOf { LightRatingPalettes }

val LocalRecordingPalette = staticCompositionLocalOf { RecordingPalette }
