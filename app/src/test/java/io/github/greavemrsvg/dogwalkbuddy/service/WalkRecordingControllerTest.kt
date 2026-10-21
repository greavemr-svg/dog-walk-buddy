package io.github.greavemrsvg.dogwalkbuddy.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.data.repo.RecoverInterruptedWalkUseCase
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class WalkRecordingControllerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var test: TestAppContainer
    private val logged = mutableListOf<String>()

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
    }

    @After
    fun tearDown() {
        test.close()
    }

    private val holder get() = test.container.recordingStateHolder
    private val repository get() = test.container.walkRepository
    private val controller get() = test.container.walkRecordingController

    @Test
    fun AC28_start_stores_the_row_publishes_the_session_and_starts_the_service_with_the_walk_id() =
        runBlocking {
            val started = controller.start()

            assertTrue(started)
            val walk = repository.recordingWalks().single()
            assertEquals(listOf(walk.id), test.launcher.started)
            assertEquals(walk.id, holder.state.value!!.walkId)
            assertEquals(test.clock.instant(), holder.state.value!!.startUtc)
            assertEquals(WalkStatus.RECORDING, walk.status)
        }

    @Test
    fun a_second_start_while_recording_does_nothing() =
        runBlocking {
            controller.start()

            val second = controller.start()

            assertFalse(second)
            assertEquals(1, repository.recordingWalks().size)
            assertEquals(1, test.launcher.started.size)
        }

    @Test
    fun two_taps_at_the_same_time_start_one_walk() =
        runBlocking {
            val results = listOf(async { controller.start() }, async { controller.start() }).awaitAll()

            assertEquals(1, results.count { it })
            assertEquals(1, repository.recordingWalks().size)
        }

    @Test
    fun a_service_that_cannot_start_leaves_no_walk_and_no_session_and_logs_only_the_error_class() =
        runBlocking {
            test.launcher.failStart = true
            val failing =
                WalkRecordingController(
                    repository,
                    holder,
                    test.launcher,
                    RecoverInterruptedWalkUseCase(repository) { false },
                    logError = { logged += it },
                )

            val started = failing.start()

            assertFalse(started)
            assertTrue(repository.recordingWalks().isEmpty())
            assertNull(holder.state.value)
            assertEquals(listOf("IllegalStateException"), logged)
        }

    @Test
    fun AC30_stop_asks_the_service_to_stop_only_while_recording() {
        controller.stop()
        assertEquals(0, test.launcher.stopCount)

        runBlocking { controller.start() }
        controller.stop()

        assertEquals(1, test.launcher.stopCount)
    }

    @Test
    fun AC34_recovery_saves_a_walk_left_recording_by_an_earlier_process() =
        runBlocking {
            val walk = repository.startWalk()
            test.clock.advance(Duration.ofMinutes(5))

            val recovered = controller.recoverInterrupted()

            assertEquals(1, recovered)
            assertEquals(WalkStatus.COMPLETE, repository.walk(walk.id)!!.status)
        }

    @Test
    fun AC34_recovery_leaves_a_walk_that_is_recording_in_this_process() =
        runBlocking {
            controller.start()

            val recovered = controller.recoverInterrupted()

            assertEquals(0, recovered)
            assertEquals(WalkStatus.RECORDING, repository.recordingWalks().single().status)
        }
}
