package io.github.greavemrsvg.dogwalkbuddy.ui.walkdetail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WalkSummary
import io.github.greavemrsvg.dogwalkbuddy.testutil.FAKE_MAP_TAG
import io.github.greavemrsvg.dogwalkbuddy.testutil.FakeRouteMap
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.history.WalkDateFormats
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneOffset

/** The walk detail screen with a fake map (AC38, AC39, AC50, AC51, AC52). Coordinates are synthetic. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class WalkDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val map = FakeRouteMap()
    private var backs = 0
    private var mapFailures = 0
    private val current = mutableStateOf<WalkDetailUiState>(WalkDetailUiState.Loading)
    private val formats = WalkDateFormats(ZoneOffset.UTC, true)
    private val route = listOf(LatLon(51.50, -0.12), LatLon(51.51, -0.10), LatLon(51.49, -0.14))
    private val walk =
        WalkSummary(
            id = 7,
            startUtc = Instant.parse("2026-10-19T09:15:00Z"),
            durationSeconds = 4500,
            distanceM = 3218.688,
        )

    private fun show(
        state: WalkDetailUiState,
        fontScale: Float = 1f,
    ) {
        current.value = state
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) {
                    WalkDetailContent(
                        state = current.value,
                        routeMap = map,
                        formats = formats,
                        onBack = { backs++ },
                        onMapLoadFailed = { mapFailures++ },
                    )
                }
            }
        }
    }

    private fun loaded(
        mapAvailable: Boolean = true,
        route: List<LatLon> = this.route,
    ) = WalkDetailUiState.Loaded(walk, route, mapAvailable)

    @Test
    fun AC38_the_route_is_given_to_the_map_and_the_bounds_fit_the_whole_route() {
        show(loaded())

        composeRule.onNodeWithTag(FAKE_MAP_TAG).assertIsDisplayed()
        assertEquals(route, map.lastRoute)
        val bounds = map.lastBounds!!
        assertEquals(51.49, bounds.south, 1e-9)
        assertEquals(51.51, bounds.north, 1e-9)
        assertEquals(-0.14, bounds.west, 1e-9)
        assertEquals(-0.10, bounds.east, 1e-9)
    }

    @Test
    fun AC38_the_map_shows_both_required_attribution_texts() {
        show(loaded())

        composeRule.onNodeWithText("© OpenStreetMap contributors", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("© OpenMapTiles", substring = true).assertIsDisplayed()
    }

    @Test
    fun AC38_the_date_duration_and_distance_are_shown_in_the_standard_formats() {
        show(loaded())

        composeRule.onNodeWithText("Mon 19 Oct 2026 at 09:15").assertIsDisplayed()
        composeRule.onNodeWithText("Duration 1:15").assertIsDisplayed()
        composeRule.onNodeWithText("Distance 2.0 mi").assertIsDisplayed()
    }

    @Test
    fun AC39_offline_the_map_is_not_created_and_the_message_is_shown_with_duration_distance_and_date() {
        show(loaded(mapAvailable = false))

        composeRule.onNodeWithText("Map not available offline").assertIsDisplayed()
        assertEquals(0, map.drawCount)
        assertNull(map.lastRoute)
        composeRule.onNodeWithText("Mon 19 Oct 2026 at 09:15").assertIsDisplayed()
        composeRule.onNodeWithText("Duration 1:15").assertIsDisplayed()
        composeRule.onNodeWithText("Distance 2.0 mi").assertIsDisplayed()
        composeRule.onNodeWithText("© OpenStreetMap contributors", substring = true).assertDoesNotExist()
    }

    @Test
    fun AC39_when_the_map_reports_a_failure_the_screen_is_told() {
        show(loaded())

        composeRule.runOnIdle { map.failLoading() }

        assertEquals(1, mapFailures)
    }

    @Test
    fun AC39_after_a_failure_the_message_replaces_the_map() {
        show(loaded(mapAvailable = true))
        composeRule.onNodeWithTag(FAKE_MAP_TAG).assertIsDisplayed()

        composeRule.runOnIdle { map.failLoading() }
        // The screen is told, as the real ViewModel would, and then shows the message in place of the map.
        composeRule.runOnIdle { current.value = loaded(mapAvailable = false) }

        composeRule.onNodeWithText("Map not available offline").assertIsDisplayed()
    }

    @Test
    fun a_walk_with_no_route_says_so_and_does_not_create_a_map() {
        show(loaded(route = emptyList()))

        composeRule.onNodeWithText("No route was recorded for this walk").assertIsDisplayed()
        composeRule.onNodeWithText("Map not available offline").assertDoesNotExist()
        assertEquals(0, map.drawCount)
    }

    @Test
    fun a_walk_that_cannot_be_found_says_so() {
        show(WalkDetailUiState.NotFound)

        composeRule.onNodeWithText("This walk can't be found. It may have been deleted.").assertIsDisplayed()
    }

    @Test
    fun a_walk_that_fails_to_load_says_so() {
        show(WalkDetailUiState.LoadFailed)

        composeRule.onNodeWithText("Couldn't load this walk.").assertIsDisplayed()
    }

    @Test
    fun while_loading_only_the_heading_and_back_are_shown() {
        show(WalkDetailUiState.Loading)

        composeRule.onNodeWithText("Walk details").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodes(hasText("Duration", substring = true)).fetchSemanticsNodes().size)
    }

    @Test
    fun the_back_control_goes_back() {
        show(loaded())

        composeRule.onNodeWithText("Back").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun AC51_the_back_control_is_labelled_and_at_least_48dp() {
        show(loaded())

        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC50_the_map_has_a_spoken_description() {
        show(loaded())

        composeRule
            .onNodeWithTag(FAKE_MAP_TAG)
            .assertExists()
        composeRule
            .onAllNodes(
                androidx.compose.ui.test
                    .hasContentDescription("Map of the route you walked"),
            ).fetchSemanticsNodes()
            .let { assertEquals(1, it.size) }
    }

    @Test
    fun AC52_the_screen_at_the_largest_font_size_is_not_truncated_with_the_map() {
        show(loaded(), fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
    }

    @Test
    fun AC52_the_screen_at_the_largest_font_size_is_not_truncated_offline() {
        show(loaded(mapAvailable = false), fontScale = 2.0f)

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithText("Map not available offline").assertExists()
    }
}
