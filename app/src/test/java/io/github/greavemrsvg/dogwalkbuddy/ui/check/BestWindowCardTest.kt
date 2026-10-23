package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
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
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHiddenFromAccessibility
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DarkRatingPalettes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LightRatingPalettes
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RatingPalettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * The best-window card and the 12-hour strip on Today, at the AC23 test clock 2026-10-19T14:20:00Z with the device
 * time zone UTC, so the current hour starts at 14:00 (AC22, AC23, AC58, AC61).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BestWindowCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var is24Hour = true
    private val formatter = BestWindowFormatter(context.resources, ZoneOffset.UTC, { is24Hour })

    private fun show(
        state: CheckUiState,
        night: Boolean = false,
        fontScale: Float = 1f,
    ) {
        if (night) RuntimeEnvironment.setQualifiers("+night")
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) {
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
    }

    private fun rated(
        window: BestWindow = FIRST_FIVE_HOURS,
        ratings: List<Rating> = ARTBOARD_RATINGS,
    ): CheckUiState.Rated {
        val hours = ratings.mapIndexed { index, rating -> HourRating(hourStart(index), rating, Reason.GOOD) }
        return CheckUiState.Rated(
            current = hours.first(),
            window = window,
            ageMinutes = null,
            usingDefaultSensitivity = false,
            hours = hours,
            factors = FactorGrid.cells(HourlyConditions(hourStart(0), 15.0, 0.0, 9.0, 100.0), PavementFactor.NotRated, Reason.GOOD),
            isCached = false,
            isOffline = false,
            dogName = null,
            sensitivity = HeatSensitivity.NORMAL,
        )
    }

    private fun bar(index: Int) = composeRule.onNodeWithTag(STRIP_BAR_TAG_PREFIX + index, useUnmergedTree = true)

    private fun label(index: Int) = composeRule.onNodeWithTag(STRIP_LABEL_TAG_PREFIX + index, useUnmergedTree = true)

    private fun labelText(index: Int): String =
        label(index)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .single()
            .text

    private fun markedColumns(): List<Int> =
        (0 until STRIP_COLUMNS).filter {
            composeRule.onAllNodes(hasTestTag(STRIP_MARKER_TAG_PREFIX + it), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

    private fun labelShown(index: Int): Boolean =
        composeRule.onAllNodes(hasTestTag(STRIP_LABEL_TAG_PREFIX + index), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun assertDpEquals(
        expected: Dp,
        actual: Dp,
        what: String,
    ) = assertTrue("$what: expected $expected, was $actual", abs(expected.value - actual.value) < TOLERANCE_DP)

    /** The labels shown, left to right, never overlap their neighbour and none is cut off (AC23). */
    private fun assertLabelsDoNotOverlap(indices: List<Int>) {
        indices.zipWithNext().forEach { (left, right) ->
            assertTrue("Labels $left and $right overlap", label(left).getBoundsInRoot().right <= label(right).getBoundsInRoot().left)
        }
        composeRule.assertNoTextTruncation()
    }

    private fun assertBarsMatch(
        ratings: List<Rating>,
        palettes: RatingPalettes,
    ) {
        ratings.forEachIndexed { index, rating ->
            val node = bar(index).assertExists()
            assertEquals(rating, node.fetchSemanticsNode().config[BarRating])
            assertDpEquals(barHeight(rating), node.getBoundsInRoot().height, "Bar $index height")
            val pixels = node.captureToImage().toPixelMap()
            assertEquals("Bar $index colour", palettes.of(rating).bar, pixels[pixels.width / 2, pixels.height / 2])
            if (index > 0) assertTrue("Bars are in hour order", bar(index - 1).getBoundsInRoot().right < node.getBoundsInRoot().left)
        }
        bar(ratings.size).assertDoesNotExist()
    }

    @Test
    fun UX_AC22_the_card_shows_the_heading_the_exact_window_text_and_a_chip_with_the_rating_word() {
        show(rated())

        composeRule.onNodeWithText("Best window, next 12 hours").assertIsDisplayed()
        composeRule.onNodeWithText("Now until 19:00", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Best available: Caution", useUnmergedTree = true).assertDoesNotExist()
        composeRule
            .onNodeWithTag(BEST_WINDOW_CHIP_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Go")
    }

    @Test
    fun UX_AC22_a_later_window_keeps_the_hyphen_range_with_no_spaces() {
        show(rated(window = BestWindow.Window(hourStart(2), hourStart(5), Rating.GO, startsNow = false)))

        composeRule.onNodeWithText("16:00-19:00", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(listOf(2, 3, 4), markedColumns())
        composeRule.onNodeWithTag(BEST_WINDOW_CHIP_TAG, useUnmergedTree = true).assertContentDescriptionEquals("Go")
    }

    @Test
    fun UX_AC22_a_caution_window_shows_best_available_and_a_caution_chip() {
        val ratings = List(12) { if (it < 3) Rating.CAUTION else Rating.AVOID }
        show(rated(window = BestWindow.Window(hourStart(0), hourStart(3), Rating.CAUTION, startsNow = true), ratings = ratings))

        composeRule.onNodeWithText("Best available: Caution", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Now until 17:00", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(BEST_WINDOW_CHIP_TAG, useUnmergedTree = true).assertContentDescriptionEquals("Caution")
    }

    @Test
    fun UX_AC22_no_good_time_has_no_chip_and_UX_AC23_no_marker() {
        show(rated(window = BestWindow.NoGoodTime, ratings = List(12) { Rating.AVOID }))

        composeRule.onNodeWithText("No good time in the next 12 hours", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(BEST_WINDOW_CHIP_TAG, useUnmergedTree = true).assertDoesNotExist()
        assertEquals(emptyList<Int>(), markedColumns())
        assertBarsMatch(List(12) { Rating.AVOID }, LightRatingPalettes)
    }

    @Test
    fun UX_AC23_in_24_hour_mode_there_are_12_bars_in_hour_order_with_two_digit_labels_and_a_marker_over_the_window() {
        is24Hour = true
        show(rated())

        assertBarsMatch(ARTBOARD_RATINGS, LightRatingPalettes)
        assertEquals(listOf("14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "00", "01"), (0 until 12).map(::labelText))
        assertLabelsDoNotOverlap((0 until 12).toList())
        // The marker sits over exactly the five window bars, 14:00 to 18:00, and over no other column.
        assertEquals((0..4).toList(), markedColumns())
        (0..4).forEach { index ->
            val segment = composeRule.onNodeWithTag(STRIP_MARKER_TAG_PREFIX + index, useUnmergedTree = true).getBoundsInRoot()
            val under = bar(index).getBoundsInRoot()
            assertDpEquals(under.left, segment.left, "Marker $index start")
            assertDpEquals(under.right, segment.right, "Marker $index end")
            assertDpEquals(3.dp, segment.height, "Marker $index height")
            assertTrue("The marker sits above the bars", segment.bottom <= under.top)
        }
        composeRule.assertHiddenFromAccessibility(HOUR_STRIP_TAG)
    }

    @Test
    fun UX_AC23_in_12_hour_mode_the_labels_read_2pm_to_1am_in_full_without_overlap() {
        is24Hour = false
        show(rated())

        assertEquals(
            listOf("2pm", "3pm", "4pm", "5pm", "6pm", "7pm", "8pm", "9pm", "10pm", "11pm", "12am", "1am"),
            (0 until 12).map(::labelText),
        )
        assertLabelsDoNotOverlap((0 until 12).toList())
        // The best-window text keeps its MVP 12-hour form, with a space (MVP AC16, MVP AC18).
        composeRule.onNodeWithText("Now until 7:00 pm", useUnmergedTree = true).assertIsDisplayed()
        composeRule.assertHiddenFromAccessibility(HOUR_STRIP_TAG)
    }

    @Test
    fun UX_AC23_fewer_than_12_hours_leave_the_last_columns_empty() {
        val ratings = ARTBOARD_RATINGS.take(3)
        show(rated(window = BestWindow.Window(hourStart(0), hourStart(2), Rating.GO, startsNow = true), ratings = ratings))

        assertBarsMatch(ratings, LightRatingPalettes)
        assertTrue(labelShown(2))
        assertTrue(!labelShown(3))
    }

    @Test
    fun UX_AC23_at_the_largest_font_the_strip_thins_its_labels_so_none_overlaps_or_is_cut() {
        is24Hour = false
        show(rated(), fontScale = 2.0f)

        val shown = (0 until 12).filter(::labelShown)
        assertTrue("Some labels are left out at 200 %: $shown", shown.size < 12)
        assertEquals(0, shown.first())
        assertLabelsDoNotOverlap(shown)
    }

    @Test
    fun UX_AC61_go_caution_and_avoid_differ_by_bar_height_word_and_icon() {
        show(rated())

        assertEquals(listOf(44.dp, 28.dp, 14.dp), Rating.entries.map { barHeight(it) })
        assertEquals(setOf("Go", "Caution", "Avoid"), Rating.entries.map { context.getString(it.labelRes()) }.toSet())
        assertEquals(
            3,
            Rating.entries
                .map { it.iconRes() }
                .toSet()
                .size,
        )
        // On screen, a Go, a Caution and an Avoid bar (columns 0, 5 and 7) have three different heights.
        assertEquals(3, listOf(0, 5, 7).map { bar(it).getBoundsInRoot().height }.toSet().size)
    }

    @Test
    fun UX_AC58_the_card_is_accessible_in_light_and_its_strip_is_hidden() = assertAccessible(night = false, LightRatingPalettes)

    @Test
    fun UX_AC58_the_card_is_accessible_in_dark_and_its_strip_is_hidden() = assertAccessible(night = true, DarkRatingPalettes)

    private fun assertAccessible(
        night: Boolean,
        palettes: RatingPalettes,
    ) {
        show(rated(), night = night)

        composeRule.assertAccessibleScreen()
        composeRule.assertHeadingsMarked("Best window, next 12 hours")
        composeRule.assertHiddenFromAccessibility(HOUR_STRIP_TAG)
        assertBarsMatch(ARTBOARD_RATINGS, palettes)
    }

    private companion object {
        val CURRENT_HOUR: Instant = Instant.parse("2026-10-19T14:00:00Z")
        const val HOUR_SECONDS = 3_600L
        const val TOLERANCE_DP = 0.5f

        // The Main artboard's pattern: Go for five hours, then Caution, Avoid and Go again.
        val ARTBOARD_RATINGS =
            listOf(
                Rating.GO,
                Rating.GO,
                Rating.GO,
                Rating.GO,
                Rating.GO,
                Rating.CAUTION,
                Rating.CAUTION,
                Rating.AVOID,
                Rating.AVOID,
                Rating.CAUTION,
                Rating.GO,
                Rating.GO,
            )

        fun hourStart(index: Int): Instant = CURRENT_HOUR.plusSeconds(index * HOUR_SECONDS)

        // 14:00 to 19:00, the first five hours, starting with the current hour.
        val FIRST_FIVE_HOURS = BestWindow.Window(CURRENT_HOUR, hourStart(5), Rating.GO, startsNow = true)
    }
}
