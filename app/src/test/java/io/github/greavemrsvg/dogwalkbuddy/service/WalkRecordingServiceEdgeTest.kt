package io.github.greavemrsvg.dogwalkbuddy.service

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.DogWalkBuddyApp
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.awaitCondition
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import java.time.Duration

/** Stop before any fix has arrived (AC30), with no network (AC31). */
@RunWith(AndroidJUnit4::class)
class WalkRecordingServiceEdgeTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val app = ApplicationProvider.getApplicationContext<DogWalkBuddyApp>()
    private lateinit var test: TestAppContainer
    private lateinit var service: ServiceController<WalkRecordingService>
    private var walkId = 0L

    @Before
    fun setUp() {
        test = TestAppContainer(temporaryFolder)
        app.container = test.container
    }

    @After
    fun tearDown() {
        test.close()
    }

    private fun intent(action: String) =
        Intent(app, WalkRecordingService::class.java).setAction(action).putExtra(WalkRecordingService.EXTRA_WALK_ID, walkId)

    @Test
    fun AC30_stop_with_no_fix_yet_saves_a_complete_walk_with_zero_distance_and_the_elapsed_time() {
        runBlocking { test.container.walkRecordingController.start() }
        walkId = test.launcher.started.single()
        service = Robolectric.buildService(WalkRecordingService::class.java, intent(WalkRecordingService.ACTION_START))
        service.create().startCommand(0, 1)
        awaitCondition("collecting") { test.walkSource.subscriptions == 1 }
        test.clock.advance(Duration.ofSeconds(42))

        service.withIntent(intent(WalkRecordingService.ACTION_STOP)).startCommand(0, 2)
        awaitCondition("saved and stopped") {
            runBlocking { test.container.walkRepository.walk(walkId) }?.status == WalkStatus.COMPLETE &&
                shadowOf(service.get()).isStoppedBySelf
        }

        val saved = runBlocking { test.container.walkRepository.walk(walkId) }!!
        assertEquals(0.0, saved.distanceM, 0.0)
        assertEquals(Duration.ofSeconds(42).toMillis(), saved.endUtcMs!! - saved.startUtcMs)
        assertTrue(runBlocking { test.container.walkRepository.routePoints(walkId) }.isEmpty())
        assertTrue(shadowOf(service.get()).isStoppedBySelf)
        assertNull(test.container.recordingStateHolder.state.value)
        assertEquals(0, test.stub.server.requestCount)
    }
}
