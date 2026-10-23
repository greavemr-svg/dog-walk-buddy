package io.github.greavemrsvg.dogwalkbuddy.ui.history

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DayDot
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DayDotState
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WalkSummary
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WeeklyTotals
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.awaitCondition
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHeadingsMarked
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertHiddenFromAccessibility
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertTouchTargetIsAtLeast
import io.github.greavemrsvg.dogwalkbuddy.ui.common.CONFIRM_SHEET_ICON_TAG
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** The history screen with a real in-memory database. The test clock is Monday 19 Oct 2026, 14:20 UTC. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h1500dp-xxhdpi")
class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer
    private val walkRow = Regex("""^\w{3} \d+ \w{3}( \d{4})? at """)

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
    }

    @After
    fun tearDown() {
        test.close()
    }

    private fun addWalk(
        start: String,
        minutes: Long,
        metres: Double,
    ): Long =
        runBlocking {
            val startAt = Instant.parse(start)
            test.database.walkDao().insertWalk(
                WalkEntity(
                    startUtcMs = startAt.toEpochMilli(),
                    endUtcMs = startAt.plus(Duration.ofMinutes(minutes)).toEpochMilli(),
                    status = WalkStatus.COMPLETE,
                    distanceM = metres,
                ),
            )
        }

    private fun show(
        fontScale: Float = 1f,
        debugActions: List<DebugAction> = emptyList(),
    ) {
        val viewModel = HistoryViewModel(test.container.walkRepository, test.clock, ZoneOffset.UTC)
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(fontScale) {
                    HistoryRoute(
                        viewModel = viewModel,
                        zoneId = ZoneOffset.UTC,
                        is24Hour = true,
                        debugActions = debugActions,
                    )
                }
            }
        }
        // The list arrives asynchronously from Room: wait for it to settle on the saved walks.
        val expected = savedIds().size
        awaitCondition("history shows $expected walks") {
            composeRule.waitForIdle()
            expected == 0 && hasNode("No walks yet") || expected > 0 && walkLines().size == expected
        }
    }

    private fun hasNode(text: String) = composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    /** The walk rows in screen order, as spoken: "date at time, duration, distance". */
    private fun walkLines(): List<String> =
        composeRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(", ") }
            .filter { walkRow.containsMatchIn(it) }

    /** Opens the "More" menu of the row whose date is [rowDate] (OD2). */
    private fun openMoreOf(rowDate: String) {
        composeRule.onNode(hasContentDescription("More") and hasAnyAncestor(hasText(rowDate))).performClick()
    }

    private fun savedIds(): List<Long> {
        val repository = test.container.walkRepository
        val walks = runBlocking { repository.observeSummaries().first() }
        return walks.map { it.id }
    }

    @Test
    fun AC35_with_no_walks_the_empty_state_reads_No_walks_yet() {
        show()

        composeRule.onNodeWithText("No walks yet").assertIsDisplayed()
    }

    @Test
    fun AC35_walks_are_listed_newest_first_with_date_start_time_duration_and_distance() {
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-19T09:15:00Z", 75, 3218.688)
        addWalk("2026-10-15T17:30:00Z", 20, 804.672)

        show()

        assertEquals(
            listOf(
                "Mon 19 Oct at 09:15, 1:15, 2.0 mi",
                "Thu 15 Oct at 17:30, 0:20, 0.5 mi",
                "Mon 12 Oct at 08:00, 0:30, 1.0 mi",
            ),
            walkLines(),
        )
    }

    @Test
    fun AC36_each_week_shows_its_dates_and_totals() {
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-18T23:30:00Z", 20, 804.672)
        addWalk("2026-10-19T00:10:00Z", 10, 482.8032)

        show()

        composeRule.onNodeWithText("Mon 12 – Sun 18 Oct").assertIsDisplayed()
        composeRule.onNodeWithText("2 walks · 0:50 · 1.5 mi").assertIsDisplayed()
        composeRule.onNode(hasText("Mon 19 – Sun 25 Oct") and isHeading()).assertIsDisplayed()
        composeRule
            .onNodeWithTag(THIS_WEEK_TAG)
            .assertTextEquals("This week", "Mon 19 – Sun 25 Oct", "1", "walk", "0:10", "h:mm", "0.3", "miles")
            .assertContentDescriptionEquals("This week: 1 walk, 10 minutes, 0.3 miles")
    }

    @Test
    fun AC37_this_week_shows_zero_when_the_current_week_has_no_walks() {
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)

        show()

        composeRule
            .onNodeWithTag(THIS_WEEK_TAG)
            .assertTextEquals("This week", "Mon 19 – Sun 25 Oct", "0", "walks", "0:00", "h:mm", "0.0", "miles")
            .assertContentDescriptionEquals("This week: 0 walks, 0 minutes, 0.0 miles")
    }

    @Test
    fun AC30_a_walk_that_has_just_been_stopped_appears_at_the_top_of_the_open_history() {
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()

        runBlocking {
            val walk = test.container.walkRepository.startWalk()
            test.clock.advance(Duration.ofMinutes(25))
            test.container.walkRepository.finishWalk(walk.id)
        }

        awaitCondition("the new walk is listed first") {
            composeRule.waitForIdle()
            walkLines().size == 2
        }
        assertEquals("Mon 19 Oct at 14:20, 0:25, 0.0 mi", walkLines().first())
    }

    @Test
    fun AC40_cancelling_the_confirmation_changes_nothing() {
        val id = addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()

        openMoreOf("Mon 12 Oct")
        composeRule.onNodeWithContentDescription("Delete walk on Mon 12 Oct 2026 at 08:00").performClick()
        composeRule.onNodeWithText("Delete this walk?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Delete this walk?").assertDoesNotExist()
        assertEquals(1, walkLines().size)
        assertEquals(listOf(id), savedIds())
    }

    @Test
    fun AC40_confirming_removes_the_walk_and_the_weekly_totals_no_longer_include_it() {
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        val keep = addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()
        composeRule
            .onNodeWithTag(THIS_WEEK_TAG)
            .assertTextEquals("This week", "Mon 19 – Sun 25 Oct", "1", "walk", "0:30", "h:mm", "1.0", "miles")
            .assertContentDescriptionEquals("This week: 1 walk, 30 minutes, 1.0 miles")

        openMoreOf("Mon 19 Oct")
        composeRule.onNodeWithContentDescription("Delete walk on Mon 19 Oct 2026 at 08:00").performClick()
        composeRule.onNodeWithText("Delete walk").performClick()

        awaitCondition("the walk is removed") {
            composeRule.waitForIdle()
            walkLines().size == 1
        }
        composeRule
            .onNodeWithTag(THIS_WEEK_TAG)
            .assertTextEquals("This week", "Mon 19 – Sun 25 Oct", "0", "walks", "0:00", "h:mm", "0.0", "miles")
            .assertContentDescriptionEquals("This week: 0 walks, 0 minutes, 0.0 miles")
        assertEquals(listOf(keep), savedIds())
    }

    @Test
    fun AC40_deleting_the_last_walk_shows_the_empty_state() {
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        show()

        openMoreOf("Mon 19 Oct")
        composeRule.onNodeWithContentDescription("Delete walk on Mon 19 Oct 2026 at 08:00").performClick()
        composeRule.onNodeWithText("Delete walk").performClick()

        awaitCondition("the empty state") {
            composeRule.waitForIdle()
            hasNode("No walks yet")
        }
    }

    @Test
    fun AC50_AC51_every_control_has_a_spoken_label_and_is_at_least_48dp() {
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()

        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC50_each_delete_control_names_the_walk_it_deletes() {
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()

        openMoreOf("Mon 19 Oct")
        composeRule.onNodeWithContentDescription("Delete walk on Mon 19 Oct 2026 at 08:00").assertIsDisplayed()
        openMoreOf("Mon 12 Oct")
        composeRule.onNodeWithContentDescription("Delete walk on Mon 12 Oct 2026 at 08:00").assertIsDisplayed()
    }

    @Test
    fun AC52_nothing_is_truncated_at_the_largest_font_size() {
        addWalk("2026-10-19T08:00:00Z", 135, 16093.44)
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show(fontScale = 2f)

        composeRule.assertNoTextTruncation()
    }

    @Test
    fun the_More_menu_is_hidden_when_there_are_no_debug_actions() {
        show()

        composeRule.onNodeWithText("More").assertDoesNotExist()
    }

    @Test
    fun a_debug_action_in_the_More_menu_runs_when_chosen() {
        var ran = false
        show(debugActions = listOf(DebugAction("Do it") { ran = true }))

        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Do it").performClick()

        assertTrue(ran)
    }

    private fun contentState(
        count: Int,
        deleteFailed: Boolean = false,
        loadFailed: Boolean = false,
    ): HistoryUiState {
        val start = Instant.parse("2026-10-19T08:00:00Z")
        val walks = (0 until count).map { WalkSummary(it + 1L, start.minus(Duration.ofHours(it.toLong())), 1800, 1609.344) }
        return HistoryUiState(
            isLoaded = true,
            thisWeek = WeeklyTotals.thisWeek(walks, ZoneOffset.UTC, start),
            weeks = WeeklyTotals.group(walks, ZoneOffset.UTC),
            deleteFailed = deleteFailed,
            loadFailed = loadFailed,
        )
    }

    private fun showContent(
        state: MutableState<HistoryUiState>,
        onDismissDeleteFailure: () -> Unit = {},
    ) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                HistoryContent(
                    state = state.value,
                    formats = WalkDateFormats(ZoneOffset.UTC, true),
                    onRequestDelete = {},
                    onConfirmDelete = {},
                    onCancelDelete = {},
                    onDismissDeleteFailure = onDismissDeleteFailure,
                )
            }
        }
    }

    @Test
    fun AC40_a_delete_that_fails_is_announced_over_a_scrolled_list_and_can_be_dismissed() {
        val state = mutableStateOf(contentState(count = 40))
        var dismissals = 0
        showContent(state) {
            dismissals++
            state.value = state.value.copy(deleteFailed = false)
        }
        composeRule.onNodeWithTag(HISTORY_LIST_TAG).performScrollToIndex(30)
        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertDoesNotExist()

        state.value = state.value.copy(deleteFailed = true)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertIsDisplayed()
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
            .assertCountEquals(1)
            .assertCountEquals(1)
        composeRule.onNodeWithText("OK").performClick()
        composeRule.waitForIdle()

        assertEquals(1, dismissals)
        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertDoesNotExist()
    }

    @Test
    fun AC40_the_failure_message_goes_away_when_the_failure_is_cleared_elsewhere() {
        val state = mutableStateOf(contentState(count = 1, deleteFailed = true))
        showContent(state)
        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertIsDisplayed()

        state.value = state.value.copy(deleteFailed = false)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertDoesNotExist()
    }

    @Test
    fun a_history_that_could_not_be_loaded_shows_the_error_and_no_zero_summary() {
        val state = mutableStateOf(contentState(count = 0, loadFailed = true))
        showContent(state)

        composeRule.onNodeWithText("Couldn't load your walks.").assertIsDisplayed()
        composeRule.onNodeWithTag(THIS_WEEK_TAG).assertDoesNotExist()
    }

    @Test
    fun AC40_a_second_failure_after_dismissal_announces_the_message_again() {
        val state = mutableStateOf(contentState(count = 2, deleteFailed = true))
        var dismissals = 0
        showContent(state) {
            dismissals++
            state.value = state.value.copy(deleteFailed = false)
        }
        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertDoesNotExist()

        state.value = state.value.copy(deleteFailed = true)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertIsDisplayed()
        assertEquals(1, dismissals)
    }

    @Test
    fun AC40_no_failure_message_is_shown_when_nothing_failed() {
        val state = mutableStateOf(contentState(count = 2))
        showContent(state)

        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertDoesNotExist()
        composeRule.onNodeWithText("OK").assertDoesNotExist()
    }

    @Test
    fun AC50_AC51_the_failure_snackbar_action_is_labelled_and_at_least_48dp_at_200_percent_font() {
        val state = mutableStateOf(contentState(count = 3, deleteFailed = true))
        composeRule.setContent {
            DogWalkBuddyTheme {
                WithFontScale(2f) {
                    HistoryContent(
                        state = state.value,
                        formats = WalkDateFormats(ZoneOffset.UTC, true),
                        onRequestDelete = {},
                        onConfirmDelete = {},
                        onCancelDelete = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertIsDisplayed()
        composeRule.onNodeWithText("OK").assertIsDisplayed()
        composeRule.assertAccessibleScreen()
    }

    @Test
    fun AC40_the_confirmation_dialog_and_the_failure_message_are_not_shown_together() {
        val walk = WalkSummary(1, Instant.parse("2026-10-19T08:00:00Z"), 1800, 1609.344)
        val state = mutableStateOf(contentState(count = 1).copy(pendingDelete = walk))
        showContent(state)
        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertDoesNotExist()

        // The view model closes the confirmation as it reports the failure.
        state.value = state.value.copy(pendingDelete = null, deleteFailed = true)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Couldn't delete the walk. Try again.").assertIsDisplayed()
    }

    // ---- UX redesign, task 6 (Walks) ----

    private fun addSpecWalks() {
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-18T23:30:00Z", 20, 804.672)
        addWalk("2026-10-19T00:10:00Z", 10, 482.8032)
    }

    private fun headingTop(text: String): Float =
        composeRule
            .onNode(hasText(text) and isHeading())
            .fetchSemanticsNode()
            .boundsInRoot.top

    private fun row(
        rowDate: String,
        time: String,
    ) = composeRule.onNode(hasClickAction() and hasText(rowDate) and hasText(time))

    private fun assertWalksTab(
        newestRange: String,
        olderRange: String,
        rowDate: String,
        spoken: String,
    ) {
        composeRule.assertHeadingsMarked("Walks", newestRange, olderRange)
        composeRule.onNodeWithText("Back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        assertTrue("newest week first", headingTop(newestRange) < headingTop(olderRange))
        composeRule.onNodeWithText("1 walk · 0:10 · 0.3 mi").assertIsDisplayed()
        composeRule.onNodeWithText("2 walks · 0:50 · 1.5 mi").assertIsDisplayed()
        val newest = row(rowDate, "at 00:10")
        newest.assertTextEquals(rowDate, "at 00:10", "0.3 mi", "0:10")
        newest.assertContentDescriptionEquals(spoken)
        newest.assertHasClickAction()
        newest.assertHeightIsAtLeast(64.dp)
        assertEquals(1, composeRule.onAllNodes(hasClickAction() and hasText(rowDate) and hasText("at 00:10")).fetchSemanticsNodes().size)
    }

    @Test
    fun UX_AC39_the_walks_tab_shows_week_sections_newest_first_with_D7_dates_and_totals() {
        addSpecWalks()
        show()

        assertWalksTab(
            newestRange = "Mon 19 – Sun 25 Oct",
            olderRange = "Mon 12 – Sun 18 Oct",
            rowDate = "Mon 19 Oct",
            spoken = "Mon 19 Oct at 00:10, 0:10, 0.3 mi",
        )
        assertEquals(3, walkLines().size)
    }

    @Test
    fun UX_AC39_with_the_clock_in_2027_the_dates_and_a_year_spanning_week_show_the_year() {
        addSpecWalks()
        addWalk("2026-12-30T10:00:00Z", 20, 804.672)
        test.clock.advance(Duration.ofDays(365))
        show()

        composeRule.onNode(hasText("Mon 28 Dec 2026 – Sun 3 Jan 2027") and isHeading()).assertExists()
        assertWalksTab(
            newestRange = "Mon 19 – Sun 25 Oct 2026",
            olderRange = "Mon 12 – Sun 18 Oct 2026",
            rowDate = "Mon 19 Oct 2026",
            spoken = "Mon 19 Oct 2026 at 00:10, 0:10, 0.3 mi",
        )
    }

    private fun assertZeroThisWeekCard() {
        composeRule
            .onNodeWithTag(THIS_WEEK_TAG)
            .assertTextEquals("This week", "Mon 19 – Sun 25 Oct", "0", "walks", "0:00", "h:mm", "0.0", "miles")
            .assertContentDescriptionEquals("This week: 0 walks, 0 minutes, 0.0 miles")
    }

    @Test
    fun UX_AC40_the_this_week_card_shows_zero_stats_with_captions_and_is_spoken_as_one_summary() {
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()

        assertZeroThisWeekCard()
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC40_the_this_week_card_reads_the_same_in_dark() {
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()

        assertZeroThisWeekCard()
    }

    @Test
    fun UX_AC40_three_walks_are_spoken_with_hours_and_minutes() {
        addWalk("2026-10-19T06:00:00Z", 45, 3218.688)
        addWalk("2026-10-19T09:00:00Z", 30, 2414.016)
        addWalk("2026-10-19T12:00:00Z", 10, 1126.5408)
        show()

        composeRule
            .onNodeWithTag(THIS_WEEK_TAG)
            .assertTextEquals("This week", "Mon 19 – Sun 25 Oct", "3", "walks", "1:25", "h:mm", "4.2", "miles")
            .assertContentDescriptionEquals("This week: 3 walks, 1 hour 25 minutes, 4.2 miles")
    }

    @Test
    fun UX_AC40_one_walk_this_week_uses_the_singular_caption() {
        addWalk("2026-10-19T06:00:00Z", 45, 3218.688)
        show()

        composeRule
            .onNodeWithTag(THIS_WEEK_TAG)
            .assertTextEquals("This week", "Mon 19 – Sun 25 Oct", "1", "walk", "0:45", "h:mm", "2.0", "miles")
            .assertContentDescriptionEquals("This week: 1 walk, 45 minutes, 2.0 miles")
    }

    private fun dot(index: Int): DayDot =
        composeRule
            .onNodeWithTag(WEEK_DOT_TAG_PREFIX + index, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[DayDotKey]

    private fun assertThursdayDots() {
        assertEquals(
            listOf(
                DayDotState.WALKED,
                DayDotState.PAST_NO_WALK,
                DayDotState.WALKED,
                DayDotState.PAST_NO_WALK,
                DayDotState.FUTURE,
                DayDotState.FUTURE,
                DayDotState.FUTURE,
            ),
            (0..6).map { dot(it).state },
        )
        assertEquals(listOf(3), (0..6).filter { dot(it).isToday })
        assertEquals(LocalDate.of(2026, 10, 19), dot(0).date)
        assertEquals(LocalDate.of(2026, 10, 25), dot(6).date)
        composeRule.assertHiddenFromAccessibility(WEEK_DOTS_TAG)
    }

    @Test
    fun UX_AC41_the_dots_show_walked_past_today_and_future_days_and_are_hidden_from_accessibility() {
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-21T08:00:00Z", 30, 1609.344)
        test.clock.advance(Duration.ofDays(3))
        show()

        assertThursdayDots()
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC41_the_dots_show_the_same_states_in_dark() {
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-21T08:00:00Z", 30, 1609.344)
        test.clock.advance(Duration.ofDays(3))
        show()

        assertThursdayDots()
    }

    @Test
    fun UX_AC42_with_no_walks_the_empty_state_has_the_exact_text_and_a_decorative_icon() {
        show()

        composeRule.onNodeWithText("No walks yet").assertIsDisplayed()
        composeRule.onNodeWithTag(HISTORY_EMPTY_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.assertHiddenFromAccessibility(HISTORY_EMPTY_ICON_TAG)
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC43_a_load_failure_shows_the_exact_text_in_dark() {
        showContent(mutableStateOf(contentState(count = 0, loadFailed = true)))

        composeRule.onNodeWithText("Couldn't load your walks.").assertIsDisplayed()
        composeRule.onNodeWithTag(THIS_WEEK_TAG).assertDoesNotExist()
        composeRule.assertHeadingsMarked("Walks")
    }

    @Test
    fun UX_AC44_delete_is_in_the_rows_More_menu_and_the_sheet_keeps_the_MVP_texts() {
        val id = addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        show()
        composeRule.onNodeWithText("Delete").assertDoesNotExist()

        openMoreOf("Mon 19 Oct")
        composeRule
            .onNodeWithContentDescription("Delete walk on Mon 19 Oct 2026 at 08:00")
            .assertTextEquals("Delete")
            .assertTouchTargetIsAtLeast(composeRule, 48.dp, 48.dp)
            .performClick()

        composeRule.onNodeWithText("Delete this walk?").assertIsDisplayed()
        composeRule.assertHeadingsMarked("Delete this walk?")
        composeRule
            .onNodeWithText("The walk on Mon 19 Oct 2026 at 08:00 and its route will be removed from this phone. This can't be undone.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Delete walk").assertHeightIsAtLeast(56.dp)
        composeRule.onNodeWithText("Cancel").assertHeightIsAtLeast(56.dp)
        composeRule.assertHiddenFromAccessibility(CONFIRM_SHEET_ICON_TAG)
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Delete this walk?").assertDoesNotExist()
        assertEquals(listOf(id), savedIds())
    }

    private fun assertTabAndSheetAccessible(rowDate: String) {
        composeRule.assertAccessibleScreen()
        openMoreOf(rowDate)
        composeRule.onNodeWithContentDescription("Delete walk on $rowDate 2026 at 08:00").performClick()
        composeRule.onNodeWithText("Delete this walk?").assertIsDisplayed()
        composeRule.assertAccessibleScreen()
    }

    @Test
    fun UX_AC58_the_walks_tab_and_the_delete_sheet_are_accessible_in_light() {
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()

        assertTabAndSheetAccessible("Mon 19 Oct")
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC58_the_walks_tab_and_the_delete_sheet_are_accessible_in_dark() {
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()

        assertTabAndSheetAccessible("Mon 12 Oct")
    }

    @Test
    fun UX_AC60_at_200_percent_the_walks_tab_is_not_truncated_and_scrolls() {
        addSpecWalks()
        addWalk("2026-10-20T08:00:00Z", 735, 160934.4)
        show(fontScale = 2f)

        composeRule.assertNoTextTruncation()
        composeRule.onNodeWithTag(HISTORY_LIST_TAG).performScrollToIndex(5)
        composeRule.onNodeWithText("Mon 12 – Sun 18 Oct").assertIsDisplayed()
        composeRule.assertNoTextTruncation()
    }

    /** Tester: the visible texts of every walk row, in screen order (walkLines() reads the spoken label). */
    private fun visibleRows(): List<List<String>> =
        composeRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } }
            .filter { texts -> texts.firstOrNull()?.let { Regex("""^\w{3} \d+ \w{3}( \d{4})?$""").matches(it) } == true }

    @Test
    fun UX_AC39_every_row_shows_date_time_distance_and_duration_in_order_and_one_target_each() {
        addSpecWalks()
        show()

        assertEquals(
            listOf(
                listOf("Mon 19 Oct", "at 00:10", "0.3 mi", "0:10"),
                listOf("Sun 18 Oct", "at 23:30", "0.5 mi", "0:20"),
                listOf("Mon 12 Oct", "at 08:00", "1.0 mi", "0:30"),
            ),
            visibleRows(),
        )
        assertEquals(
            listOf(
                "Mon 19 Oct at 00:10, 0:10, 0.3 mi",
                "Sun 18 Oct at 23:30, 0:20, 0.5 mi",
                "Mon 12 Oct at 08:00, 0:30, 1.0 mi",
            ),
            walkLines(),
        )
        listOf("Mon 19 Oct" to "at 00:10", "Sun 18 Oct" to "at 23:30", "Mon 12 Oct" to "at 08:00").forEach { (date, time) ->
            row(date, time).assertHeightIsAtLeast(64.dp)
        }
    }

    @Test
    fun UX_AC39_with_the_clock_in_2027_every_row_keeps_the_year_in_text_and_speech() {
        addSpecWalks()
        test.clock.advance(Duration.ofDays(365))
        show()

        assertEquals(
            listOf(
                listOf("Mon 19 Oct 2026", "at 00:10", "0.3 mi", "0:10"),
                listOf("Sun 18 Oct 2026", "at 23:30", "0.5 mi", "0:20"),
                listOf("Mon 12 Oct 2026", "at 08:00", "1.0 mi", "0:30"),
            ),
            visibleRows(),
        )
        assertEquals(
            listOf(
                "Mon 19 Oct 2026 at 00:10, 0:10, 0.3 mi",
                "Sun 18 Oct 2026 at 23:30, 0:20, 0.5 mi",
                "Mon 12 Oct 2026 at 08:00, 0:30, 1.0 mi",
            ),
            walkLines(),
        )
    }

    @Test
    fun UX_AC44_confirming_delete_from_the_More_menu_removes_only_that_walk_and_its_row() {
        val keep = addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        show()

        openMoreOf("Mon 19 Oct")
        composeRule.onNodeWithContentDescription("Delete walk on Mon 19 Oct 2026 at 08:00").performClick()
        composeRule.onNodeWithText("Delete walk").performClick()

        awaitCondition("the deleted row is gone") {
            composeRule.waitForIdle()
            walkLines() == listOf("Mon 12 Oct at 08:00, 0:30, 1.0 mi")
        }
        composeRule.onNodeWithText("Delete this walk?").assertDoesNotExist()
        assertEquals(listOf(keep), savedIds())
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC60_at_200_percent_the_empty_state_is_not_truncated_in_dark() {
        show(fontScale = 2f)

        composeRule.assertNoTextTruncation()
    }
}
