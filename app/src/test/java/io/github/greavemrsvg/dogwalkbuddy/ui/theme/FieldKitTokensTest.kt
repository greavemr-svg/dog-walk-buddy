package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Field Kit colour roles equal the UX pack's `tokens.json` values exactly (AC1-AC4), and the Material 3
 * scheme is derived from them as ADR 0007 maps it. Expected values are copied from `tokens.json` (2026-10-21).
 */
class FieldKitTokensTest {
    private fun hex(color: Color): String = "#%06X".format(color.toArgb() and RGB_MASK)

    private fun roles(colors: FieldKitColors): Map<String, Color> =
        mapOf(
            "background" to colors.background,
            "surface" to colors.surface,
            "surfaceMuted" to colors.surfaceMuted,
            "primaryContainer" to colors.primaryContainer,
            "navIndicator" to colors.navIndicator,
            "onSurface" to colors.onSurface,
            "onSurfaceVariant" to colors.onSurfaceVariant,
            "textTertiary" to colors.textTertiary,
            "iconMuted" to colors.iconMuted,
            "outline" to colors.outline,
            "outlineVariant" to colors.outlineVariant,
            "divider" to colors.divider,
            "primary" to colors.primary,
            "onPrimary" to colors.onPrimary,
            "primaryPressed" to colors.primaryPressed,
            "error" to colors.error,
            "onError" to colors.onError,
            "errorText" to colors.errorText,
            "errorOutline" to colors.errorOutline,
        )

    private fun assertRoles(
        expected: Map<String, String>,
        colors: FieldKitColors,
    ) {
        val actual = roles(colors)
        assertEquals(expected.keys, actual.keys)
        expected.forEach { (role, value) ->
            assertEquals("role $role", value, hex(actual.getValue(role)))
            assertEquals("role $role is opaque", 1f, actual.getValue(role).alpha)
        }
    }

    @Test
    fun UX_AC1_every_light_role_has_exactly_its_token_value() {
        assertRoles(
            mapOf(
                "background" to "#F2F5F9",
                "surface" to "#FFFFFF",
                "surfaceMuted" to "#E5EBF2",
                "primaryContainer" to "#E1EBF7",
                "navIndicator" to "#DCE8F7",
                "onSurface" to "#0F1B2A",
                "onSurfaceVariant" to "#3E4A5A",
                "textTertiary" to "#4A5668",
                "iconMuted" to "#5A6678",
                "outline" to "#CDD7E3",
                "outlineVariant" to "#DFE6EE",
                "divider" to "#EBF0F5",
                "primary" to "#1B4F8F",
                "onPrimary" to "#FFFFFF",
                "primaryPressed" to "#0F2E57",
                "error" to "#9E2A17",
                "onError" to "#FFFFFF",
                "errorText" to "#8E2414",
                "errorOutline" to "#E2B4AB",
            ),
            LightFieldKitColors,
        )
    }

    @Test
    fun UX_AC1_the_light_scrim_is_rgba_8_16_28_at_0_55() {
        val scrim = LightFieldKitColors.scrim

        assertEquals("#08101C", hex(scrim))
        assertEquals(LIGHT_SCRIM_ALPHA, (scrim.toArgb() ushr ALPHA_SHIFT))
    }

    @Test
    fun UX_AC2_every_dark_role_has_exactly_its_token_value() {
        assertRoles(
            mapOf(
                "background" to "#0B1622",
                "surface" to "#132233",
                "surfaceMuted" to "#1A2A3D",
                "primaryContainer" to "#1E3A5F",
                "navIndicator" to "#1E3A5F",
                "onSurface" to "#F1F4F8",
                "onSurfaceVariant" to "#C8D3E0",
                "textTertiary" to "#A9B6C6",
                "iconMuted" to "#94A3B8",
                "outline" to "#4A5E78",
                "outlineVariant" to "#22364D",
                "divider" to "#1D2E43",
                "primary" to "#8CC2FF",
                "onPrimary" to "#0B2747",
                "primaryPressed" to "#D0E4FF",
                "error" to "#FFB4AB",
                "onError" to "#690005",
                "errorText" to "#FFB4AB",
                "errorOutline" to "#7A3328",
            ),
            DarkFieldKitColors,
        )
    }

    @Test
    fun UX_AC2_the_dark_scrim_is_black_at_0_6() {
        val scrim = DarkFieldKitColors.scrim

        assertEquals("#000000", hex(scrim))
        assertEquals(DARK_SCRIM_ALPHA, (scrim.toArgb() ushr ALPHA_SHIFT))
    }

    @Test
    fun UX_AC40_the_summary_caption_is_the_pack_colour_in_light_and_primary_container_in_dark() {
        assertEquals("#CFE0F5", hex(LightFieldKitColors.summaryCaption))
        assertEquals(DarkFieldKitColors.primaryContainer, DarkFieldKitColors.summaryCaption)
    }

    @Test
    fun UX_AC1_and_UX_AC2_control_borders_use_the_icon_muted_role() {
        assertEquals(LightFieldKitColors.iconMuted, LightFieldKitColors.controlBorder)
        assertEquals(DarkFieldKitColors.iconMuted, DarkFieldKitColors.controlBorder)
    }

    private fun palette(palette: RatingPalette): Map<String, String?> =
        mapOf(
            "container" to hex(palette.container),
            "onContainer" to hex(palette.onContainer),
            "onContainerMuted" to hex(palette.onContainerMuted),
            "badge" to hex(palette.badge),
            "onBadge" to hex(palette.onBadge),
            "divider" to hex(palette.divider),
            "bar" to hex(palette.bar),
            "driverHighlight.bg" to palette.driverHighlight?.background?.let(::hex),
            "driverHighlight.ring" to palette.driverHighlight?.ring?.let(::hex),
        )

    private fun expected(
        container: String,
        onContainer: String,
        onContainerMuted: String,
        badge: String,
        onBadge: String,
        divider: String,
        bar: String,
        highlightBackground: String? = null,
        highlightRing: String? = null,
    ): Map<String, String?> =
        mapOf(
            "container" to container,
            "onContainer" to onContainer,
            "onContainerMuted" to onContainerMuted,
            "badge" to badge,
            "onBadge" to onBadge,
            "divider" to divider,
            "bar" to bar,
            "driverHighlight.bg" to highlightBackground,
            "driverHighlight.ring" to highlightRing,
        )

    @Test
    fun UX_AC3_the_light_rating_palettes_equal_color_rating() {
        assertEquals(
            expected("#D8F0DF", "#0B3B21", "#1F4D33", "#13723F", "#FFFFFF", "#A9D6B6", "#13723F"),
            palette(LightRatingPalettes.go),
        )
        assertEquals(
            expected("#FDECC8", "#4A2C00", "#5A3A0A", "#8A5300", "#FFFFFF", "#E9C887", "#C27A06", "#FFF7E6", "#8A5300"),
            palette(LightRatingPalettes.caution),
        )
        assertEquals(
            expected("#F9DAD4", "#561008", "#6A2017", "#9E2A17", "#FFFFFF", "#EDB1A6", "#C2361F", "#FFF1EE", "#9E2A17"),
            palette(LightRatingPalettes.avoid),
        )
    }

    @Test
    fun UX_AC3_the_dark_rating_palettes_equal_color_dark_rating() {
        assertEquals(
            expected("#12301F", "#C4EDCF", "#9FD3AE", "#2E8A50", "#FFFFFF", "#2B5A3B", "#5CC283"),
            palette(DarkRatingPalettes.go),
        )
        assertEquals(
            expected("#3A2A0A", "#FFDDA3", "#EBC27E", "#9A6510", "#FFFFFF", "#6A4D1A", "#F0A83A", "#4A3712", "#FFDDA3"),
            palette(DarkRatingPalettes.caution),
        )
        assertEquals(
            expected("#3E1712", "#FFC9C0", "#F0A79B", "#B33A26", "#FFFFFF", "#6E2E25", "#FF7A66", "#4F1F18", "#FFC9C0"),
            palette(DarkRatingPalettes.avoid),
        )
    }

    @Test
    fun UX_AC3_go_has_no_driver_highlight_because_a_go_result_highlights_no_factor() {
        assertNull(LightRatingPalettes.go.driverHighlight)
        assertNull(DarkRatingPalettes.go.driverHighlight)
    }

    @Test
    fun UX_AC3_rating_text_colours_on_plain_surfaces_are_each_ratings_on_container() {
        assertEquals(
            RatingColors(
                go = LightRatingPalettes.go.onContainer,
                caution = LightRatingPalettes.caution.onContainer,
                avoid = LightRatingPalettes.avoid.onContainer,
            ),
            LightRatingColors,
        )
        assertEquals(
            RatingColors(
                go = DarkRatingPalettes.go.onContainer,
                caution = DarkRatingPalettes.caution.onContainer,
                avoid = DarkRatingPalettes.avoid.onContainer,
            ),
            DarkRatingColors,
        )
    }

    @Test
    fun UX_AC3_rating_bars_are_44_28_and_14_dp_high() {
        assertEquals(listOf(44.dp, 28.dp, 14.dp), listOf(FieldKitSizes.barGo, FieldKitSizes.barCaution, FieldKitSizes.barAvoid))
    }

    @Test
    fun UX_AC4_the_recording_palette_equals_color_recording() {
        val actual =
            with(RecordingPalette) {
                mapOf(
                    "background" to background,
                    "card" to card,
                    "mapPanel" to mapPanel,
                    "mapPanel.border" to mapPanelBorder,
                    "onDark" to onDark,
                    "onDarkMuted" to onDarkMuted,
                    "route" to route,
                    "route.casing" to routeCasing,
                    "liveDot" to liveDot,
                    "stopButton.bg" to stopBackground,
                    "stopButton.fg" to stopForeground,
                ).mapValues { hex(it.value) }
            }

        assertEquals(
            mapOf(
                "background" to "#0B1622",
                "card" to "#1A2A3D",
                "mapPanel" to "#132233",
                "mapPanel.border" to "#22364D",
                "onDark" to "#FFFFFF",
                "onDarkMuted" to "#C8D3E0",
                "route" to "#8CC2FF",
                "route.casing" to "#2D4A6B",
                "liveDot" to "#FF6B57",
                "stopButton.bg" to "#F2F5F9",
                "stopButton.fg" to "#0B1622",
            ),
            actual,
        )
    }

    @Test
    fun UX_AC4_the_recording_material_scheme_is_built_from_the_recording_palette_only() {
        with(RecordingColorScheme) {
            assertEquals(RecordingPalette.background, background)
            assertEquals(RecordingPalette.onDark, onBackground)
            assertEquals(RecordingPalette.background, surface)
            assertEquals(RecordingPalette.onDark, onSurface)
            assertEquals(RecordingPalette.card, surfaceVariant)
            assertEquals(RecordingPalette.onDarkMuted, onSurfaceVariant)
            assertEquals(RecordingPalette.stopBackground, primary)
            assertEquals(RecordingPalette.stopForeground, onPrimary)
            assertEquals(RecordingPalette.onDarkMuted, outline)
            assertEquals(RecordingPalette.mapPanelBorder, outlineVariant)
        }
    }

    private fun assertMaterialMapping(colors: FieldKitColors) {
        val scheme = if (colors == LightFieldKitColors) LightColors else DarkColors
        assertEquals(colors.primary, scheme.primary)
        assertEquals(colors.onPrimary, scheme.onPrimary)
        assertEquals(colors.primaryContainer, scheme.primaryContainer)
        assertEquals(colors.onSurface, scheme.onPrimaryContainer)
        assertEquals(colors.navIndicator, scheme.secondaryContainer)
        assertEquals(colors.primaryPressed, scheme.onSecondaryContainer)
        assertEquals(colors.background, scheme.background)
        assertEquals(colors.onSurface, scheme.onBackground)
        assertEquals(colors.surface, scheme.surface)
        assertEquals(colors.onSurface, scheme.onSurface)
        listOf(
            scheme.surfaceContainerLowest,
            scheme.surfaceContainerLow,
            scheme.surfaceContainer,
            scheme.surfaceContainerHigh,
            scheme.surfaceContainerHighest,
            scheme.surfaceBright,
            scheme.surfaceDim,
        ).forEach { assertEquals(colors.surface, it) }
        assertEquals(colors.surfaceMuted, scheme.surfaceVariant)
        assertEquals(colors.onSurfaceVariant, scheme.onSurfaceVariant)
        assertEquals(colors.iconMuted, scheme.outline)
        assertEquals(colors.outlineVariant, scheme.outlineVariant)
        assertEquals(colors.error, scheme.error)
        assertEquals(colors.onError, scheme.onError)
        assertEquals(colors.scrim.copy(alpha = 1f), scheme.scrim)
        assertEquals(colors.primary, scheme.secondary)
        assertEquals(colors.primary, scheme.tertiary)
        assertEquals(colors.onSurface, scheme.inverseSurface)
    }

    @Test
    fun UX_AC1_the_light_material_scheme_is_derived_from_the_light_roles() = assertMaterialMapping(LightFieldKitColors)

    @Test
    fun UX_AC2_the_dark_material_scheme_is_derived_from_the_dark_roles() = assertMaterialMapping(DarkFieldKitColors)

    private fun cornerOf(shape: RoundedCornerShape): Dp {
        val pixels = shape.topStart.toPx(Size(LARGE_SIDE, LARGE_SIDE), Density(1f))
        return pixels.dp
    }

    @Test
    fun UX_AC66_shapes_spacing_and_sizes_match_the_radius_space_and_size_tokens() {
        assertEquals(
            listOf(28.dp, 22.dp, 30.dp, 22.dp, 18.dp, 16.dp, 14.dp, 24.dp),
            with(FieldKitShapes) {
                listOf(hero, card, sheet, startButton, button, input, iconTile, bestWindowCard).map(::cornerOf)
            },
        )
        assertEquals(0f, FieldKitShapes.sheet.bottomStart.toPx(Size(LARGE_SIDE, LARGE_SIDE), Density(1f)))
        assertEquals(LARGE_SIDE / 2, FieldKitShapes.pill.topStart.toPx(Size(LARGE_SIDE, LARGE_SIDE), Density(1f)))
        assertEquals(
            listOf(FieldKitShapes.input, FieldKitShapes.button, FieldKitShapes.card, FieldKitShapes.hero),
            with(FieldKitMaterialShapes) { listOf(small, medium, large, extraLarge) },
        )
        assertEquals(
            listOf(16.dp, 20.dp, 18.dp, 22.dp, 12.dp, 18.dp, 14.dp),
            with(FieldKitSpacing) { listOf(gutter, headerSide, cardPadding, heroPadding, stackGap, sectionGap, rowGap) },
        )
        assertEquals(
            listOf(48.dp, 72.dp, 76.dp, 56.dp, 60.dp, 52.dp, 48.dp, 60.dp, 78.dp, 60.dp, 32.dp, 64.dp, 88.dp, 22.dp, 18.dp, 14.dp),
            with(FieldKitSizes) {
                listOf(
                    minTouchTarget,
                    startStopHeight,
                    stopHeight,
                    primaryButtonHeight,
                    permissionButtonHeight,
                    inputHeight,
                    iconButton,
                    ratingBadge,
                    bottomNavHeight,
                    navIndicatorWidth,
                    navIndicatorHeight,
                    listRowMinHeight,
                    avatar,
                    iconNav,
                    iconInline,
                    iconSmall,
                )
            },
        )
    }

    private companion object {
        const val RGB_MASK = 0xFFFFFF
        const val ALPHA_SHIFT = 24
        const val LIGHT_SCRIM_ALPHA = 140
        const val DARK_SCRIM_ALPHA = 153
        const val LARGE_SIDE = 1000f
    }
}
