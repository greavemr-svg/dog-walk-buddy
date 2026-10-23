package io.github.greavemrsvg.dogwalkbuddy.ui.walkdetail

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WalkSummary
import io.github.greavemrsvg.dogwalkbuddy.testutil.FAKE_MAP_TAG
import io.github.greavemrsvg.dogwalkbuddy.testutil.FakeRouteMap
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.history.WalkDateFormats
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Clock
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
                        clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC),
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

    private fun assertAttributionKeepsMapLibreControlsClear(fontScale: Float) {
        show(loaded(), fontScale = fontScale)

        val map = composeRule.onNodeWithTag(WALK_MAP_AREA_TAG).getUnclippedBoundsInRoot()
        val strip = composeRule.onNodeWithTag(WALK_MAP_ATTRIBUTION_TAG).getUnclippedBoundsInRoot()
        // MapLibre draws its logo and "i" button at the bottom-start; the app strip must stay in the end part.
        val mapWidth = map.right - map.left
        assertTrue("attribution must start after 40% of the map width", strip.left - map.left >= mapWidth * 0.4f - 1.dp)
        assertEquals("attribution must end at the map's end edge", map.right.value, strip.right.value, 0.5f)
        composeRule.onNodeWithText("© OpenStreetMap contributors", substring = true).assertIsDisplayed()
        composeRule.assertNoTextTruncation()
    }

    @Test
    fun AC38_the_attribution_is_at_the_end_so_the_MapLibre_logo_and_button_stay_visible() {
        assertAttributionKeepsMapLibreControlsClear(fontScale = 1f)
    }

    @Test
    fun AC52_the_attribution_at_the_largest_font_size_still_leaves_the_MapLibre_controls_clear() {
        assertAttributionKeepsMapLibreControlsClear(fontScale = 2.0f)
    }

    @Test
    fun AC38_the_attribution_text_is_unchanged_and_uses_at_most_60_percent_of_the_map_width() {
        show(loaded())

        val map = composeRule.onNodeWithTag(WALK_MAP_AREA_TAG).getUnclippedBoundsInRoot()
        val strip = composeRule.onNodeWithTag(WALK_MAP_ATTRIBUTION_TAG).getUnclippedBoundsInRoot()
        composeRule.onNodeWithText("© OpenStreetMap contributors · © OpenMapTiles").assertIsDisplayed()
        val mapWidth = map.right - map.left
        assertTrue("attribution strip must be at most 60% wide", strip.right - strip.left <= mapWidth * 0.6f + 1.dp)
    }

    @Test
    fun AC52_the_attribution_at_the_largest_font_size_stays_inside_the_map_bottom_edge() {
        show(loaded(), fontScale = 2.0f)

        val map = composeRule.onNodeWithTag(WALK_MAP_AREA_TAG).getUnclippedBoundsInRoot()
        val strip = composeRule.onNodeWithTag(WALK_MAP_ATTRIBUTION_TAG).getUnclippedBoundsInRoot()
        assertEquals("attribution must sit on the map's bottom edge", map.bottom.value, strip.bottom.value, 0.5f)
        assertTrue("attribution must not leave the map at the top", strip.top >= map.top)
    }

    @Test
    fun AC38_the_date_duration_and_distance_are_shown_in_the_standard_formats() {
        show(loaded())

        composeRule.onNodeWithText("Monday 19 October").assertIsDisplayed()
        composeRule.onNodeWithText("09:15 – 10:30").assertIsDisplayed()
        composeRule.onNode(hasText("Duration") and hasText("1:15")).assertIsDisplayed()
        composeRule.onNode(hasText("Distance") and hasText("2.0 mi")).assertIsDisplayed()
    }

    @Test
    fun AC39_offline_the_map_is_not_created_and_the_message_is_shown_with_duration_distance_and_date() {
        show(loaded(mapAvailable = false))

        composeRule.onNodeWithText("Map not available offline").assertIsDisplayed()
        assertEquals(0, map.drawCount)
        assertNull(map.lastRoute)
        composeRule.onNodeWithText("Monday 19 October").assertIsDisplayed()
        composeRule.onNodeWithText("09:15 – 10:30").assertIsDisplayed()
        composeRule.onNode(hasText("Duration") and hasText("1:15")).assertIsDisplayed()
        composeRule.onNode(hasText("Distance") and hasText("2.0 mi")).assertIsDisplayed()
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

        composeRule.onNodeWithText("Walk details").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodes(hasText("Duration", substring = true)).fetchSemanticsNodes().size)
    }

    @Test
    fun the_back_control_goes_back() {
        show(loaded())

        composeRule.onNodeWithContentDescription("Back").performClick()

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

    // UX redesign (task 7): the edge-to-edge map, the sheet and the delete flow.

    private val deletion = mutableStateOf(WalkDeletionState())
    private var deleteRequests = 0
    private var deleteConfirms = 0
    private var deleteCancels = 0

    private fun showDetail(
        state: WalkDetailUiState = loaded(),
        clock: Clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC),
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
                        clock = clock,
                        deletion = deletion.value,
                        onRequestDelete = {
                            deleteRequests++
                            deletion.value = WalkDeletionState(confirming = true)
                        },
                        onConfirmDelete = {
                            deleteConfirms++
                            deletion.value = WalkDeletionState()
                        },
                        onCancelDelete = {
                            deleteCancels++
                            deletion.value = WalkDeletionState()
                        },
                        onDismissDeleteFailure = { deletion.value = WalkDeletionState() },
                    )
                }
            }
        }
    }

    @Test
    fun UX_AC45_the_map_fills_the_top_edge_to_edge_with_a_floating_back_of_at_least_48dp() {
        showDetail()

        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val area = composeRule.onNodeWithTag(WALK_MAP_AREA_TAG).getUnclippedBoundsInRoot()
        assertEquals(root.top.value, area.top.value, 0.5f)
        assertEquals(root.left.value, area.left.value, 0.5f)
        assertEquals(root.right.value, area.right.value, 0.5f)
        composeRule.onNodeWithTag(FAKE_MAP_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("© OpenStreetMap contributors", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("© OpenMapTiles", substring = true).assertIsDisplayed()
    }

    @Test
    fun UX_AC45_the_sheet_overlaps_the_map_by_28dp_and_the_attribution_stays_above_it() {
        showDetail()

        val area = composeRule.onNodeWithTag(WALK_MAP_AREA_TAG).getUnclippedBoundsInRoot()
        val sheet = composeRule.onNodeWithTag(WALK_DETAIL_SHEET_TAG).getUnclippedBoundsInRoot()
        val strip = composeRule.onNodeWithTag(WALK_MAP_ATTRIBUTION_TAG).getUnclippedBoundsInRoot()
        assertEquals(470f - 28f, sheet.top.value, 0.5f)
        // The drawn map ends where the sheet starts, so MapLibre's logo and the attribution are never covered.
        assertEquals(sheet.top.value, area.bottom.value, 0.5f)
        assertTrue("attribution must be above the sheet", strip.bottom <= sheet.top)
    }

    @Test
    fun UX_AC46_the_sheet_shows_the_date_heading_time_range_two_cards_and_delete() {
        showDetail()

        composeRule.onNodeWithText("Monday 19 October").assertIsDisplayed()
        composeRule.assertHeadingsMarked("Monday 19 October")
        composeRule.onNodeWithText("09:15 – 10:30").assertIsDisplayed()
        composeRule.onNode(hasText("Duration") and hasText("1:15")).assertIsDisplayed()
        composeRule.onNode(hasText("Distance") and hasText("2.0 mi")).assertIsDisplayed()
        // Each card is one node: its caption is not a separate node in the merged tree.
        assertEquals(1, composeRule.onAllNodes(hasText("Duration")).fetchSemanticsNodes().size)
        assertEquals(1, composeRule.onAllNodes(hasText("Distance")).fetchSemanticsNodes().size)
        composeRule
            .onNodeWithText("Delete this walk")
            .assertIsDisplayed()
            .assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("Walk details").assertDoesNotExist()
    }

    @Test
    fun UX_AC46_a_walk_from_another_year_shows_the_year_in_the_heading() {
        showDetail(clock = Clock.fixed(Instant.parse("2027-01-05T12:00:00Z"), ZoneOffset.UTC))

        composeRule.onNodeWithText("Monday 19 October 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Monday 19 October").assertDoesNotExist()
    }

    @Test
    fun UX_AC47_offline_the_map_area_says_so_with_the_sub_line_and_the_facts_still_show() {
        showDetail(loaded(mapAvailable = false))

        composeRule.onNodeWithText("Map not available offline").assertIsDisplayed()
        composeRule.onNodeWithText("Your route is saved. It'll show here next time you're online.").assertIsDisplayed()
        assertEquals(0, map.drawCount)
        composeRule.onNodeWithText("Monday 19 October").assertIsDisplayed()
        composeRule.onNode(hasText("Duration") and hasText("1:15")).assertIsDisplayed()
        composeRule.onNode(hasText("Distance") and hasText("2.0 mi")).assertIsDisplayed()
    }

    @Test
    fun UX_AC48_delete_asks_in_a_sheet_with_the_exact_texts_and_buttons_of_at_least_48dp() {
        showDetail()

        composeRule.onNodeWithText("Delete this walk").performClick()

        assertEquals(1, deleteRequests)
        composeRule.onNodeWithText("Delete this walk?").assertIsDisplayed()
        composeRule
            .onNodeWithText("The walk on Mon 19 Oct 2026 at 09:15 and its route will be removed from this phone. This can't be undone.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Delete walk").assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        composeRule.onNodeWithText("Cancel").assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
        assertEquals(0, deleteConfirms)
    }

    @Test
    fun UX_AC48_confirming_deletes_and_cancel_changes_nothing() {
        showDetail()

        composeRule.onNodeWithText("Delete this walk").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()
        assertEquals(1, deleteCancels)
        assertEquals(0, deleteConfirms)
        composeRule.onNodeWithText("Delete this walk?").assertDoesNotExist()

        composeRule.onNodeWithText("Delete this walk").performClick()
        composeRule.onNodeWithText("Delete walk").performClick()
        assertEquals(1, deleteConfirms)
        assertEquals(0, backs)
    }

    @Test
    fun UX_AC48_once_the_walk_is_deleted_the_screen_returns_to_walks() {
        showDetail()

        composeRule.runOnIdle { deletion.value = WalkDeletionState(deleted = true) }
        composeRule.waitForIdle()

        assertEquals(1, backs)
    }

    @Test
    fun UX_AC48_a_failed_delete_shows_the_existing_message() {
        deletion.value = WalkDeletionState(failed = true)
        showDetail()

        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertIsDisplayed()
        assertEquals(0, backs)
    }

    @Test
    fun UX_AC49_no_route_not_found_and_load_failed_show_the_exact_existing_texts() {
        showDetail(loaded(route = emptyList()))
        composeRule.onNodeWithText("No route was recorded for this walk").assertIsDisplayed()
        composeRule.onNodeWithText("Monday 19 October").assertIsDisplayed()

        composeRule.runOnIdle { current.value = WalkDetailUiState.NotFound }
        composeRule.onNodeWithText("This walk can't be found. It may have been deleted.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()

        composeRule.runOnIdle { current.value = WalkDetailUiState.LoadFailed }
        composeRule.onNodeWithText("Couldn't load this walk.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    private fun assertDetailAndSheetAccessible() {
        showDetail()
        composeRule.assertAccessibleScreen()
        composeRule.onNodeWithText("Delete this walk").performClick()
        composeRule.onNodeWithText("Delete this walk?").assertIsDisplayed()
        composeRule.assertAccessibleScreen()
    }

    @Test
    fun UX_AC58_the_walk_detail_and_its_delete_sheet_are_accessible_in_light() {
        assertDetailAndSheetAccessible()
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC58_the_walk_detail_and_its_delete_sheet_are_accessible_in_dark() {
        assertDetailAndSheetAccessible()
    }

    @Test
    fun UX_AC60_a_at_200_percent_the_detail_is_not_truncated_and_scrolls_to_delete() {
        showDetail(fontScale = 2f)

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithText("Delete this walk").performScrollTo().assertIsDisplayed()
        composeRule.assertNoTextTruncation()
    }

    @Test
    fun UX_AC60_a_at_200_percent_offline_the_message_and_sub_line_are_not_truncated() {
        showDetail(loaded(mapAvailable = false), fontScale = 2f)

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithText("Your route is saved. It'll show here next time you're online.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Delete this walk").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC45_over_the_light_map_the_status_bar_icons_are_dark_and_restored_afterwards() {
        var hostView: View? = null
        current.value = WalkDetailUiState.Loading
        composeRule.setContent {
            hostView = LocalView.current
            DogWalkBuddyTheme {
                WalkDetailContent(
                    state = current.value,
                    routeMap = map,
                    formats = formats,
                    onBack = {},
                    onMapLoadFailed = {},
                )
            }
        }

        fun lightStatusBars(): Boolean {
            val view = hostView!!
            val window = (view.context as ComponentActivity).window
            return WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars
        }
        val before = composeRule.runOnIdle { lightStatusBars() }
        assertFalse("night starts with light icons", before)

        composeRule.runOnIdle { current.value = loaded() }
        assertTrue("dark icons over the map", composeRule.runOnIdle { lightStatusBars() })

        composeRule.runOnIdle { current.value = loaded(mapAvailable = false) }
        assertEquals("restored once the map is gone", before, composeRule.runOnIdle { lightStatusBars() })
    }
}
