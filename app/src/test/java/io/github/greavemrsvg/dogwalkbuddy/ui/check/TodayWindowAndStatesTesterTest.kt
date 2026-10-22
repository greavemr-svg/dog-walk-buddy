package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorGrid
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.PavementFactor
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import io.github.greavemrsvg.dogwalkbuddy.ui.common.PERMISSION_ILLUSTRATION_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DarkFieldKitColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.FieldKitColors
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LightFieldKitColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * Tester checks for task 4 (AC22, AC23, AC29, AC30, AC58): the exact hyphen range, the chip's rating in the card's
 * spoken text, the strip labels in the formatter's zone, and that the drawn decorations (the Can't check dashed
 * border and the location illustration) really paint from theme tokens.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TodayWindowAndStatesTesterTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun show(
        state: CheckUiState,
        zone: ZoneId = ZoneOffset.UTC,
        is24Hour: Boolean = true,
    ) {
        val formatter = BestWindowFormatter(context.resources, zone, { is24Hour })
        composeRule.setContent {
            DogWalkBuddyTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CheckCard(
                        state = state,
                        windowFormatter = formatter,
                        onRefresh = {},
                        onGrantPermission = {},
                        onOpenAbout = {},
                        onOpenProfile = {},
                    )
                }
            }
        }
    }

    private fun rated(
        firstHour: Instant,
        window: BestWindow,
        ratings: List<Rating> = List(12) { Rating.GO },
    ): CheckUiState.Rated {
        val hours = ratings.mapIndexed { index, rating -> HourRating(firstHour.plusSeconds(index * HOUR_SECONDS), rating, Reason.GOOD) }
        return CheckUiState.Rated(
            current = hours.first(),
            window = window,
            ageMinutes = null,
            usingDefaultSensitivity = false,
            hours = hours,
            factors = FactorGrid.cells(HourlyConditions(firstHour, 15.0, 0.0, 9.0, 100.0), PavementFactor.NotRated, Reason.GOOD),
            isCached = false,
            isOffline = false,
            dogName = null,
            sensitivity = HeatSensitivity.NORMAL,
        )
    }

    private fun labelText(index: Int): String =
        composeRule
            .onNodeWithTag(STRIP_LABEL_TAG_PREFIX + index, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .single()
            .text

    /** The merged node TalkBack reads for the best-window card: its texts and its content descriptions. */
    private fun cardAnnouncement(): Pair<List<String>, List<String>> {
        val node = composeRule.onNodeWithText("Best window, next 12 hours").fetchSemanticsNode()
        val texts =
            node.config
                .getOrNull(SemanticsProperties.Text)
                .orEmpty()
                .map { it.text }
        val descriptions = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        return texts to descriptions
    }

    private fun assertClose(
        what: String,
        expected: Color,
        actual: Color,
    ) {
        val close =
            abs(expected.red - actual.red) < COLOUR_TOLERANCE &&
                abs(expected.green - actual.green) < COLOUR_TOLERANCE &&
                abs(expected.blue - actual.blue) < COLOUR_TOLERANCE
        assertTrue("$what: expected $expected, was $actual", close)
    }

    private fun isClose(
        a: Color,
        b: Color,
    ) = abs(a.red - b.red) < COLOUR_TOLERANCE && abs(a.green - b.green) < COLOUR_TOLERANCE && abs(a.blue - b.blue) < COLOUR_TOLERANCE

    // AC22

    @Test
    fun UX_AC22_a_later_window_reads_exactly_12_00_15_00_with_a_hyphen_and_no_spaces() {
        val first = Instant.parse("2026-10-19T11:00:00Z")
        val window = BestWindow.Window(first.plusSeconds(HOUR_SECONDS), first.plusSeconds(4 * HOUR_SECONDS), Rating.GO, startsNow = false)
        show(rated(first, window))

        composeRule.onNodeWithText("12:00-15:00", useUnmergedTree = true).assertExists()
        listOf("12:00 - 15:00", "12:00–15:00", "12:00 – 15:00").forEach {
            composeRule.onNodeWithText(it, useUnmergedTree = true, substring = true).assertDoesNotExist()
        }
    }

    @Test
    fun UX_AC22_the_chip_rating_word_is_spoken_in_the_card_announcement_but_is_not_a_text_node() {
        val first = Instant.parse("2026-10-19T14:00:00Z")
        show(rated(first, BestWindow.Window(first, first.plusSeconds(5 * HOUR_SECONDS), Rating.GO, startsNow = true)))

        val (texts, descriptions) = cardAnnouncement()
        assertEquals(listOf("Best window, next 12 hours", "Now until 19:00"), texts)
        assertEquals(listOf("Go"), descriptions)
        // The strip's labels are hidden, so none of them is part of what is read for the card.
        assertFalse(texts.any { it == "14" || it == "15" })
        // The chip draws its word: on the card there is no "Go" text node (the hero's rating word stays the only one).
        val goTexts =
            composeRule
                .onAllNodes(hasText("Go") and hasAnyAncestor(hasTestTag(BEST_WINDOW_CARD_TAG)), useUnmergedTree = true)
                .fetchSemanticsNodes()
        assertTrue(goTexts.isEmpty())
        composeRule.onNodeWithTag(BEST_WINDOW_CHIP_TAG, useUnmergedTree = true).assertExists()
    }

    @Test
    fun UX_AC22_a_caution_window_announces_best_available_and_caution() {
        val first = Instant.parse("2026-10-19T14:00:00Z")
        val ratings = List(12) { if (it < 3) Rating.CAUTION else Rating.AVOID }
        show(rated(first, BestWindow.Window(first, first.plusSeconds(3 * HOUR_SECONDS), Rating.CAUTION, startsNow = true), ratings))

        val (texts, descriptions) = cardAnnouncement()
        assertEquals(listOf("Best window, next 12 hours", "Best available: Caution", "Now until 17:00"), texts)
        assertEquals(listOf("Caution"), descriptions)
    }

    @Test
    fun UX_AC22_no_good_time_announces_no_rating_word() {
        val first = Instant.parse("2026-10-19T14:00:00Z")
        show(rated(first, BestWindow.NoGoodTime, List(12) { Rating.AVOID }))

        val (texts, descriptions) = cardAnnouncement()
        assertEquals(listOf("Best window, next 12 hours", "No good time in the next 12 hours"), texts)
        assertTrue(descriptions.isEmpty())
        composeRule.onNodeWithText("Best available: Caution", useUnmergedTree = true).assertDoesNotExist()
    }

    // AC23

    @Test
    fun UX_AC23_the_strip_labels_use_the_device_zone_in_24_hour_mode() {
        // 13:00 UTC is 14:00 in London on 19 October (BST), so the strip reads as the AC23 example.
        val first = Instant.parse("2026-10-19T13:00:00Z")
        show(
            rated(first, BestWindow.Window(first, first.plusSeconds(2 * HOUR_SECONDS), Rating.GO, startsNow = true)),
            ZoneId.of("Europe/London"),
        )

        assertEquals(
            listOf("14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "00", "01"),
            (0 until 12).map(::labelText),
        )
    }

    @Test
    fun UX_AC23_the_strip_labels_use_the_device_zone_in_12_hour_mode() {
        val first = Instant.parse("2026-10-19T13:00:00Z")
        show(
            rated(first, BestWindow.Window(first, first.plusSeconds(2 * HOUR_SECONDS), Rating.GO, startsNow = true)),
            ZoneId.of("Europe/London"),
            is24Hour = false,
        )

        assertEquals(
            listOf("2pm", "3pm", "4pm", "5pm", "6pm", "7pm", "8pm", "9pm", "10pm", "11pm", "12am", "1am"),
            (0 until 12).map(::labelText),
        )
    }

    @Test
    fun UX_AC23_noon_on_the_strip_reads_12pm() {
        val first = Instant.parse("2026-10-19T11:00:00Z")
        show(rated(first, BestWindow.Window(first, first.plusSeconds(2 * HOUR_SECONDS), Rating.GO, startsNow = true)), is24Hour = false)

        assertEquals(
            listOf("11am", "12pm", "1pm", "2pm", "3pm", "4pm", "5pm", "6pm", "7pm", "8pm", "9pm", "10pm"),
            (0 until 12).map(::labelText),
        )
    }

    // AC29 and AC30: the drawn decorations paint from theme tokens.

    @Test
    fun UX_AC29_the_cant_check_card_has_a_dashed_outline_border_in_light() = assertDashedBorder(night = false, LightFieldKitColors)

    @Test
    fun UX_AC29_the_cant_check_card_has_a_dashed_outline_border_in_dark() = assertDashedBorder(night = true, DarkFieldKitColors)

    private fun assertDashedBorder(
        night: Boolean,
        colors: FieldKitColors,
    ) {
        val host = TodayStateHost(composeRule)
        host.show(CheckUiState.CantCheck, night = night)

        val pixels = composeRule.onNodeWithTag(CANT_CHECK_CARD_TAG).captureToImage().toPixelMap()
        // The 2 dp stroke is centred 1 dp in from the edge (3 px at xxhdpi). Sample the straight top edge only.
        val y = 3
        val row = (pixels.width / 4 until pixels.width * 3 / 4).map { pixels[it, y] }
        assertTrue("The border has outline-coloured dashes", row.any { isClose(it, colors.outline) })
        assertTrue("The border has gaps between dashes", row.any { isClose(it, colors.surface) })
    }

    @Test
    fun UX_AC30_the_location_illustration_draws_the_badge_and_rings_in_primary_over_primary_container_in_light() =
        assertIllustration(night = false, LightFieldKitColors)

    @Test
    fun UX_AC30_the_location_illustration_draws_the_badge_and_rings_in_primary_over_primary_container_in_dark() =
        assertIllustration(night = true, DarkFieldKitColors)

    private fun assertIllustration(
        night: Boolean,
        colors: FieldKitColors,
    ) {
        val host = TodayStateHost(composeRule)
        host.show(CheckUiState.NeedsLocationPermission, night = night)

        val pixels = composeRule.onNodeWithTag(PERMISSION_ILLUSTRATION_TAG, useUnmergedTree = true).captureToImage().toPixelMap()
        val density = context.resources.displayMetrics.density
        val cx = pixels.width / 2
        val cy = pixels.height / 2
        // Inside the 52 dp badge, away from its icon's centre stroke: a point 20 dp left of centre is the badge fill.
        assertClose("Badge", colors.primary, pixels[cx - (20 * density).toInt(), cy])
        // Between the badge (26 dp radius) and the inner circle's edge (56 dp): primary at 18 % over primaryContainer.
        assertClose(
            "Inner circle fill",
            colors.primary.copy(alpha = 0.18f).compositeOver(colors.primaryContainer),
            pixels[
                cx +
                    (40 * density).toInt(), cy,
            ],
        )
        // Well outside the inner circle and between the rings: the plain container.
        assertClose("Background", colors.primaryContainer, pixels[cx + (80 * density).toInt(), cy])
    }

    private companion object {
        const val HOUR_SECONDS = 3_600L
        const val COLOUR_TOLERANCE = 0.03f
    }
}
