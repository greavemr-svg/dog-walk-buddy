package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.awaitCondition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartWalkViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer
    private lateinit var viewModel: StartWalkViewModel
    private val events = mutableListOf<Unit>()
    private lateinit var collector: Job

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
        viewModel =
            StartWalkViewModel(
                controller = test.container.walkRecordingController,
                holder = test.container.recordingStateHolder,
                hasFinePermission = test.container.hasFineLocationPermission,
            )
        collector =
            CoroutineScope(Dispatchers.Unconfined).launch(start = CoroutineStart.UNDISPATCHED) {
                viewModel.started.collect { events += it }
            }
    }

    @After
    fun tearDown() {
        collector.cancel()
        test.close()
    }

    private fun recordingWalks() = runBlocking { test.container.walkRepository.recordingWalks() }

    @Test
    fun the_control_starts_idle() {
        assertEquals(StartWalkUiState.Idle, viewModel.state.value)
    }

    @Test
    fun AC33_without_precise_location_the_tap_shows_the_explanation_and_starts_nothing() {
        test.fineGranted = false

        val goAhead = viewModel.onStartTapped()

        assertFalse(goAhead)
        awaitCondition("the explanation state") { viewModel.state.value == StartWalkUiState.NeedsPreciseLocation }
        assertTrue(recordingWalks().isEmpty())
        assertTrue(test.launcher.started.isEmpty())
    }

    @Test
    fun AC28_with_precise_location_the_tap_may_go_ahead_and_start_records_and_reports() {
        val goAhead = viewModel.onStartTapped()
        viewModel.start()
        awaitCondition("the walk is started") { events.size == 1 }

        assertTrue(goAhead)
        assertEquals(1, recordingWalks().size)
        assertEquals(1, test.launcher.started.size)
        awaitCondition("the in-progress state") { viewModel.state.value == StartWalkUiState.InProgress }
    }

    @Test
    fun a_start_that_fails_reports_nothing() {
        test.launcher.failStart = true

        viewModel.start()
        awaitCondition("the failed start is cleaned up") { test.launcher.started.isEmpty() && recordingWalks().isEmpty() }

        assertTrue(events.isEmpty())
        assertEquals(StartWalkUiState.Idle, viewModel.state.value)
    }

    @Test
    fun AC33_a_refused_request_that_can_be_asked_again_keeps_the_explanation() {
        assertFalse(viewModel.onPreciseLocationResult(granted = false, canAskAgain = true))

        awaitCondition("the explanation state") { viewModel.state.value == StartWalkUiState.NeedsPreciseLocation }
    }

    @Test
    fun AC33_a_refusal_the_system_will_not_ask_again_points_to_settings() {
        assertFalse(viewModel.onPreciseLocationResult(granted = false, canAskAgain = false))

        awaitCondition("the blocked state") { viewModel.state.value == StartWalkUiState.PreciseLocationBlocked }
    }

    @Test
    fun a_granted_request_clears_the_explanation_and_lets_the_start_go_ahead() {
        test.fineGranted = false
        viewModel.onStartTapped()
        awaitCondition("the explanation state") { viewModel.state.value == StartWalkUiState.NeedsPreciseLocation }

        assertTrue(viewModel.onPreciseLocationResult(granted = true, canAskAgain = true))

        awaitCondition("idle again") { viewModel.state.value == StartWalkUiState.Idle }
    }

    @Test
    fun a_permission_granted_in_settings_clears_the_explanation_when_the_screen_starts_again() {
        test.fineGranted = false
        viewModel.onStartTapped()
        awaitCondition("the explanation state") { viewModel.state.value == StartWalkUiState.NeedsPreciseLocation }
        test.fineGranted = true

        viewModel.onShown()

        awaitCondition("idle again") { viewModel.state.value == StartWalkUiState.Idle }
    }

    @Test
    fun the_explanation_stays_while_the_permission_is_still_missing_when_the_screen_starts_again() {
        test.fineGranted = false
        viewModel.onStartTapped()

        viewModel.onShown()

        awaitCondition("the explanation state") { viewModel.state.value == StartWalkUiState.NeedsPreciseLocation }
    }

    @Test
    fun a_walk_in_progress_shows_the_in_progress_state_whatever_the_explanation_says() {
        test.container.recordingStateHolder.begin(7, test.clock.instant())

        awaitCondition("the in-progress state") { viewModel.state.value == StartWalkUiState.InProgress }
    }
}
