package io.github.greavemrsvg.dogwalkbuddy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import io.github.greavemrsvg.dogwalkbuddy.testutil.Wcag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AC51: the shared theme tokens meet WCAG 2.1 contrast in both themes. Text is at least 4.5:1. The rating, the
 * reason and Start/Stop are at least 7:1, so they read in bright sunlight. Borders are at least 3:1.
 */
class ContrastTest {
    private fun assertRatio(
        what: String,
        foreground: Color,
        background: Color,
        minimum: Double,
    ) {
        val ratio = Wcag.contrastRatio(foreground, background)
        assertTrue("$what has contrast $ratio, below $minimum", ratio >= minimum)
    }

    private fun assertTextContrast(
        scheme: ColorScheme,
        name: String,
    ) {
        assertRatio("$name body text", scheme.onSurface, scheme.surface, AA_TEXT)
        assertRatio("$name text on background", scheme.onBackground, scheme.background, AA_TEXT)
        assertRatio("$name secondary text", scheme.onSurfaceVariant, scheme.surface, AA_TEXT)
        assertRatio("$name error text", scheme.error, scheme.surface, AA_TEXT)
        assertRatio("$name link and outlined button text", scheme.primary, scheme.surface, AA_TEXT)
        assertRatio("$name filled button text", scheme.onPrimary, scheme.primary, AA_TEXT)
        assertRatio("$name error button text", scheme.onError, scheme.error, AA_TEXT)
        assertRatio("$name border", scheme.outline, scheme.surface, AA_NON_TEXT)
    }

    private fun assertKeyInformationContrast(
        scheme: ColorScheme,
        ratingColors: RatingColors,
        name: String,
    ) {
        assertRatio("$name reason text", scheme.onSurface, scheme.surface, KEY_TEXT)
        assertRatio("$name Go rating", ratingColors.go, scheme.surface, KEY_TEXT)
        assertRatio("$name Caution rating", ratingColors.caution, scheme.surface, KEY_TEXT)
        assertRatio("$name Avoid rating", ratingColors.avoid, scheme.surface, KEY_TEXT)
        assertRatio("$name Start button", scheme.onPrimary, scheme.primary, KEY_TEXT)
        assertRatio("$name Stop button", scheme.onError, scheme.error, KEY_TEXT)
    }

    @Test
    fun AC51_text_has_at_least_4_5_to_1_contrast_in_the_light_theme() = assertTextContrast(LightColors, "light")

    @Test
    fun AC51_text_has_at_least_4_5_to_1_contrast_in_the_dark_theme() = assertTextContrast(DarkColors, "dark")

    @Test
    fun AC51_rating_reason_and_start_stop_have_at_least_7_to_1_contrast_in_the_light_theme() =
        assertKeyInformationContrast(LightColors, LightRatingColors, "light")

    @Test
    fun AC51_rating_reason_and_start_stop_have_at_least_7_to_1_contrast_in_the_dark_theme() =
        assertKeyInformationContrast(DarkColors, DarkRatingColors, "dark")

    @Test
    fun AC53_the_three_ratings_use_three_different_colours_in_each_theme() {
        listOf(LightRatingColors, DarkRatingColors).forEach {
            assertTrue(setOf(it.go, it.caution, it.avoid).size == 3)
        }
    }

    /** The Field Kit text pairs of the design's contrast table (AC6), for one theme. */
    private fun assertFieldKitTextPairs(
        colors: FieldKitColors,
        name: String,
    ) = with(colors) {
        assertRatio("$name onSurface on surface", onSurface, surface, KEY_TEXT)
        assertRatio("$name onSurface on background", onSurface, background, AA_TEXT)
        assertRatio("$name onSurface on surfaceMuted", onSurface, surfaceMuted, AA_TEXT)
        assertRatio("$name onSurface on primaryContainer", onSurface, primaryContainer, AA_TEXT)
        assertRatio("$name onSurfaceVariant on surface", onSurfaceVariant, surface, AA_TEXT)
        assertRatio("$name onSurfaceVariant on background", onSurfaceVariant, background, AA_TEXT)
        assertRatio("$name onSurfaceVariant on surfaceMuted", onSurfaceVariant, surfaceMuted, AA_TEXT)
        assertRatio("$name onSurfaceVariant on primaryContainer", onSurfaceVariant, primaryContainer, AA_TEXT)
        assertRatio("$name textTertiary on surface", textTertiary, surface, AA_TEXT)
        assertRatio("$name textTertiary on background", textTertiary, background, AA_TEXT)
        assertRatio("$name textTertiary on surfaceMuted", textTertiary, surfaceMuted, AA_TEXT)
        assertRatio("$name primary on surface", primary, surface, AA_TEXT)
        assertRatio("$name primary on background", primary, background, AA_TEXT)
        assertRatio("$name primary on primaryContainer", primary, primaryContainer, AA_TEXT)
        assertRatio("$name onPrimary on primary", onPrimary, primary, KEY_TEXT)
        assertRatio("$name primaryPressed on surface", primaryPressed, surface, AA_TEXT)
        assertRatio("$name error on surface", error, surface, AA_TEXT)
        assertRatio("$name onError on error", onError, error, KEY_TEXT)
        assertRatio("$name surface on onSurface", surface, onSurface, AA_TEXT)
        assertRatio("$name controlBorder on surface", controlBorder, surface, AA_NON_TEXT)
    }

    /** The rating container, highlight, badge and plain-surface pairs (AC6), for one theme. */
    private fun assertRatingPairs(
        palettes: RatingPalettes,
        ratingColors: RatingColors,
        surface: Color,
        name: String,
    ) {
        listOf("Go" to palettes.go, "Caution" to palettes.caution, "Avoid" to palettes.avoid).forEach { (rating, palette) ->
            assertRatio("$name $rating word and reason on container", palette.onContainer, palette.container, KEY_TEXT)
            assertRatio("$name $rating muted text on container", palette.onContainerMuted, palette.container, AA_TEXT)
            assertRatio("$name $rating icon on badge", palette.onBadge, palette.badge, AA_NON_TEXT)
            palette.driverHighlight?.let {
                assertRatio("$name $rating text on driver highlight", palette.onContainer, it.background, AA_TEXT)
                assertRatio("$name $rating muted text on driver highlight", palette.onContainerMuted, it.background, AA_TEXT)
            }
        }
        assertKeyRatingWordsOnSurface(ratingColors, surface, name)
    }

    private fun assertKeyRatingWordsOnSurface(
        ratingColors: RatingColors,
        surface: Color,
        name: String,
    ) {
        assertRatio("$name Go word on surface", ratingColors.go, surface, KEY_TEXT)
        assertRatio("$name Caution word on surface", ratingColors.caution, surface, KEY_TEXT)
        assertRatio("$name Avoid word on surface", ratingColors.avoid, surface, KEY_TEXT)
    }

    @Test
    fun UX_AC6_every_light_text_pair_meets_its_floor() = assertFieldKitTextPairs(LightFieldKitColors, "light")

    @Test
    fun UX_AC6_every_dark_text_pair_meets_its_floor() = assertFieldKitTextPairs(DarkFieldKitColors, "dark")

    @Test
    fun UX_AC6_light_rating_words_reasons_and_icons_meet_their_floors() =
        assertRatingPairs(LightRatingPalettes, LightRatingColors, LightFieldKitColors.surface, "light")

    @Test
    fun UX_AC6_dark_rating_words_reasons_and_icons_meet_their_floors() =
        assertRatingPairs(DarkRatingPalettes, DarkRatingColors, DarkFieldKitColors.surface, "dark")

    @Test
    fun UX_AC6_recording_text_pairs_and_the_stop_label_meet_their_floors() =
        with(RecordingPalette) {
            assertRatio("recording onDark on background", onDark, background, AA_TEXT)
            assertRatio("recording onDark on card", onDark, card, AA_TEXT)
            assertRatio("recording onDark on mapPanel", onDark, mapPanel, AA_TEXT)
            assertRatio("recording onDarkMuted on background", onDarkMuted, background, AA_TEXT)
            assertRatio("recording onDarkMuted on card", onDarkMuted, card, AA_TEXT)
            assertRatio("recording Stop label", stopForeground, stopBackground, KEY_TEXT)
        }

    @Test
    fun UX_AC6_recording_error_roles_are_the_dark_field_kit_error_roles_and_meet_their_floors() {
        assertEquals(DarkFieldKitColors.error, RecordingColorScheme.error)
        assertEquals(DarkFieldKitColors.onError, RecordingColorScheme.onError)
        assertEquals(DarkFieldKitColors.errorOutline, RecordingColorScheme.errorContainer)
        assertEquals(DarkFieldKitColors.errorText, RecordingColorScheme.onErrorContainer)
        with(RecordingColorScheme) {
            assertRatio("recording error on background", error, background, AA_TEXT)
            assertRatio("recording error on card", error, surfaceContainer, AA_TEXT)
            assertRatio("recording onError on error", onError, error, KEY_TEXT)
            assertRatio("recording onErrorContainer on errorContainer", onErrorContainer, errorContainer, AA_TEXT)
        }
    }

    @Test
    fun UX_AC6_the_material_schemes_meet_the_mvp_floors_with_the_field_kit_mapping() {
        assertTextContrast(LightColors, "light")
        assertTextContrast(DarkColors, "dark")
        assertTextContrast(RecordingColorScheme, "recording")
        assertRatio("recording Stop button", RecordingColorScheme.onPrimary, RecordingColorScheme.primary, KEY_TEXT)
    }

    @Test
    fun UX_AC40_summary_captions_on_the_primary_fill_meet_4_5_to_1_in_light_and_dark() {
        assertRatio("light summaryCaption on primary", LightFieldKitColors.summaryCaption, LightFieldKitColors.primary, AA_TEXT)
        assertRatio("dark summaryCaption on primary", DarkFieldKitColors.summaryCaption, DarkFieldKitColors.primary, AA_TEXT)
    }

    @Test
    fun UX_AC67_error_text_meets_4_5_to_1_on_surface_and_background_in_light_and_dark() {
        listOf("light" to LightFieldKitColors, "dark" to DarkFieldKitColors).forEach { (name, colors) ->
            assertRatio("$name errorText on surface", colors.errorText, colors.surface, AA_TEXT)
            assertRatio("$name errorText on background", colors.errorText, colors.background, AA_TEXT)
        }
    }

    @Test
    fun UX_AC6_caution_and_avoid_driver_highlight_pairs_are_checked_in_light_and_dark() {
        // assertRatingPairs skips a palette without a highlight, so the four highlights must exist for their pairs to run.
        listOf("light" to LightRatingPalettes, "dark" to DarkRatingPalettes).forEach { (name, palettes) ->
            listOf("Caution" to palettes.caution, "Avoid" to palettes.avoid).forEach { (rating, palette) ->
                val highlight = checkNotNull(palette.driverHighlight) { "$name $rating has no driver highlight" }
                assertRatio("$name $rating text on driver highlight", palette.onContainer, highlight.background, AA_TEXT)
                assertRatio("$name $rating muted text on driver highlight", palette.onContainerMuted, highlight.background, AA_TEXT)
            }
        }
    }

    @Test
    fun UX_AC6_the_contrast_check_fails_a_pair_below_its_floor() {
        // Known WCAG values: black on white is 21:1; #777777 on white is about 4.48:1, just under the text floor.
        assertEquals(21.0, Wcag.contrastRatio(Color.Black, Color.White), 0.001)
        assertThrows(AssertionError::class.java) {
            assertRatio("grey on white", Color(0xFF777777), Color.White, AA_TEXT)
        }
        assertThrows(AssertionError::class.java) {
            assertRatio(
                "light summary caption on surface",
                LightFieldKitColors.summaryCaption,
                LightFieldKitColors.surface,
                AA_TEXT,
            )
        }
    }

    private companion object {
        const val AA_TEXT = 4.5
        const val AA_NON_TEXT = 3.0
        const val KEY_TEXT = 7.0
    }
}
