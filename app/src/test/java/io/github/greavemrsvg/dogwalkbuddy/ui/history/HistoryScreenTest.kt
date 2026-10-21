package io.github.greavemrsvg.dogwalkbuddy.ui.history

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WalkSummary
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.WeeklyTotals
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.awaitCondition
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.WithFontScale
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertAccessibleScreen
import io.github.greavemrsvg.dogwalkbuddy.ui.a11y.assertNoTextTruncation
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
    private var backs = 0
    private val walkRow = Regex("""^\w{3} \d+ \w{3} \d{4} at """)

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
                        onBack = { backs++ },
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

    /** The walk rows in screen order, as "date at time, duration, distance". */
    private fun walkLines(): List<String> =
        composeRule
            .onAllNodes(hasText(" at ", substring = true))
            .fetchSemanticsNodes()
            .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString(", ") { it.text } }
            .filter { walkRow.containsMatchIn(it) }

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
                "Mon 19 Oct 2026 at 09:15, 1:15, 2.0 mi",
                "Thu 15 Oct 2026 at 17:30, 0:20, 0.5 mi",
                "Mon 12 Oct 2026 at 08:00, 0:30, 1.0 mi",
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

        composeRule.onNodeWithText("Mon 12 Oct 2026 to Sun 18 Oct 2026").assertIsDisplayed()
        composeRule.onNodeWithText("2 walks, 0:50, 1.5 mi").assertIsDisplayed()
        composeRule.onNodeWithText("Mon 19 Oct 2026 to Sun 25 Oct 2026").assertIsDisplayed()
        composeRule.onNodeWithTag(THIS_WEEK_TAG).assertTextEquals("This week", "1 walk, 0:10, 0.3 mi")
    }

    @Test
    fun AC37_this_week_shows_zero_when_the_current_week_has_no_walks() {
        addWalk("2026-10-12T08:00:00Z", 30, 1609.344)

        show()

        composeRule.onNodeWithTag(THIS_WEEK_TAG).assertTextEquals("This week", "0 walks, 0:00, 0.0 mi")
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
        assertEquals("Mon 19 Oct 2026 at 14:20, 0:25, 0.0 mi", walkLines().first())
    }

    @Test
    fun AC40_cancelling_the_confirmation_changes_nothing() {
        val id = addWalk("2026-10-12T08:00:00Z", 30, 1609.344)
        show()

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
        composeRule.onNodeWithTag(THIS_WEEK_TAG).assertTextEquals("This week", "1 walk, 0:30, 1.0 mi")

        composeRule.onNodeWithContentDescription("Delete walk on Mon 19 Oct 2026 at 08:00").performClick()
        composeRule.onNodeWithText("Delete walk").performClick()

        awaitCondition("the walk is removed") {
            composeRule.waitForIdle()
            walkLines().size == 1
        }
        composeRule.onNodeWithTag(THIS_WEEK_TAG).assertTextEquals("This week", "0 walks, 0:00, 0.0 mi")
        assertEquals(listOf(keep), savedIds())
    }

    @Test
    fun AC40_deleting_the_last_walk_shows_the_empty_state() {
        addWalk("2026-10-19T08:00:00Z", 30, 1609.344)
        show()

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

        composeRule.onNodeWithContentDescription("Delete walk on Mon 19 Oct 2026 at 08:00").assertIsDisplayed()
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
    fun back_returns_to_the_previous_screen() {
        show()

        composeRule.onNodeWithText("Back").performClick()

        assertEquals(1, backs)
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
                    onBack = {},
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
}
