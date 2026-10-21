package io.github.greavemrsvg.dogwalkbuddy.service

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.DogWalkBuddyApp
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticTrack
import io.github.greavemrsvg.dogwalkbuddy.testutil.TestAppContainer
import io.github.greavemrsvg.dogwalkbuddy.testutil.awaitCondition
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

/**
 * The service through Robolectric's service controller, with the real database and the real start and stop
 * intents. Only the location source and the clock are fakes (design, Test strategy: service tests).
 */
@RunWith(AndroidJUnit4::class)
class WalkRecordingServiceTest {
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

    /** What the Start tap does: the row and the session first, then the service is started with the walk id. */
    private fun startWalk() {
        walkId = runBlocking { test.container.walkRecordingController.start() }.let { test.launcher.started.single() }
        service = Robolectric.buildService(WalkRecordingService::class.java, intent(WalkRecordingService.ACTION_START))
        service.create().startCommand(0, 1)
        awaitCondition("the service is collecting fixes") { test.walkSource.subscriptions == 1 }
    }

    private fun stopWalk() {
        service.withIntent(intent(WalkRecordingService.ACTION_STOP)).startCommand(0, 2)
        awaitCondition("the walk is saved and the service has stopped") {
            row()?.status == WalkStatus.COMPLETE && shadowOf(service.get()).isStoppedBySelf
        }
    }

    private fun row() = runBlocking { test.container.walkRepository.walk(walkId) }

    private fun points() = runBlocking { test.container.walkRepository.routePoints(walkId) }

    @Test
    fun AC28_starting_shows_an_ongoing_notification_and_a_location_type_foreground_service() {
        startWalk()

        val shadow = shadowOf(service.get())
        val notification = shadow.lastForegroundNotification
        assertNotNull(notification)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals("Recording your walk", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(RecordingNotification.NOTIFICATION_ID, shadow.lastForegroundNotificationId)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION, service.get().foregroundServiceType)
        assertEquals(RecordingNotification.CHANNEL_ID, notification.channelId)
    }

    @Test
    fun AC28_the_notification_opens_the_app_and_has_no_stop_action() {
        startWalk()

        val notification = shadowOf(service.get()).lastForegroundNotification
        assertNotNull(notification.contentIntent)
        assertTrue(notification.actions.isNullOrEmpty())
    }

    @Test
    fun AC28_the_session_is_published_with_the_start_time_and_the_distance_updates_with_each_fix() {
        startWalk()
        val session = test.container.recordingStateHolder.state.value!!
        assertEquals(walkId, session.walkId)
        assertEquals(test.clock.instant(), session.startUtc)

        SyntheticTrack.north(fixes = 4, stepM = 11.0, accuracyM = 5.0, start = test.clock.instant()).forEach { test.walkSource.emit(it) }
        awaitCondition("four fixes stored") { points().size == 4 }
        awaitCondition("the distance is published") {
            test.container.recordingStateHolder.state.value!!
                .distanceM > 30.0
        }

        val distance =
            test.container.recordingStateHolder.state.value!!
                .distanceM
        assertEquals(33.0, distance, 1.0)
        assertEquals(distance, row()!!.distanceM, 0.0)
    }

    @Test
    fun AC29_elapsed_time_counts_ten_minutes_with_no_fixes_and_the_route_includes_that_period() {
        startWalk()
        val start = test.clock.instant()
        val track = SyntheticTrack.north(fixes = 2, stepM = 11.0, accuracyM = 5.0, start = start)
        test.walkSource.emit(track[0])
        awaitCondition("first fix stored") { points().size == 1 }

        // The screen is locked for 10 minutes. Then fixes arrive again from the walk during that period.
        test.clock.advance(Duration.ofMinutes(10))
        test.walkSource.emit(track[1].copy(timeUtc = start.plus(Duration.ofMinutes(9))))
        awaitCondition("second fix stored") { points().size == 2 }
        stopWalk()

        val saved = row()!!
        assertEquals(Duration.ofMinutes(10).toMillis(), saved.endUtcMs!! - saved.startUtcMs)
        assertEquals(start.plus(Duration.ofMinutes(9)).toEpochMilli(), points().last().timeUtcMs)
    }

    @Test
    fun AC30_stopping_saves_the_walk_ends_the_notification_and_removes_the_location_updates() {
        startWalk()
        SyntheticTrack.north(fixes = 3, stepM = 11.0, accuracyM = 5.0, start = test.clock.instant()).forEach { test.walkSource.emit(it) }
        awaitCondition("three fixes stored") { points().size == 3 }
        test.clock.advance(Duration.ofMinutes(30))

        stopWalk()

        val saved = row()!!
        assertEquals(WalkStatus.COMPLETE, saved.status)
        assertEquals(test.clock.instant().toEpochMilli(), saved.endUtcMs)
        assertTrue(saved.distanceM > 20.0)
        assertEquals(3, points().size)
        val shadow = shadowOf(service.get())
        assertTrue(shadow.isForegroundStopped)
        assertTrue(shadow.isStoppedBySelf)
        assertEquals(0, test.walkSource.subscriptions)
        assertNull(test.container.recordingStateHolder.state.value)
    }

    @Test
    fun AC31_recording_and_stopping_make_no_network_request() {
        startWalk()
        SyntheticTrack.north(fixes = 3, stepM = 11.0, accuracyM = 5.0, start = test.clock.instant()).forEach { test.walkSource.emit(it) }
        awaitCondition("three fixes stored") { points().size == 3 }

        stopWalk()

        assertEquals(0, test.stub.server.requestCount)
    }

    @Test
    fun AC32_a_2_km_track_through_the_service_is_saved_within_2_percent() {
        startWalk()
        val track = SyntheticTrack.north(fixes = 400, stepM = 5.0, accuracyM = 8.0, start = test.clock.instant(), noiseM = 0.5)

        track.forEach { test.walkSource.emit(it) }
        awaitCondition("400 fixes stored") { points().size == 400 }
        stopWalk()

        assertEquals(2000.0, row()!!.distanceM, 40.0)
    }

    @Test
    fun fixes_with_poor_accuracy_or_old_times_are_not_stored() {
        startWalk()
        val track = SyntheticTrack.north(fixes = 3, stepM = 11.0, accuracyM = 5.0, start = test.clock.instant())
        test.walkSource.emit(track[0])
        test.walkSource.emit(track[1].copy(accuracyM = 60.0))
        test.walkSource.emit(track[0].copy(latitude = track[0].latitude + 0.001))
        test.walkSource.emit(track[2])
        awaitCondition("two good fixes stored") { points().size == 2 }

        stopWalk()

        assertEquals(2, points().size)
    }

    @Test
    fun a_start_for_a_walk_that_does_not_exist_stops_the_service_and_clears_the_session() {
        test.container.recordingStateHolder.begin(99, test.clock.instant())
        walkId = 99
        service = Robolectric.buildService(WalkRecordingService::class.java, intent(WalkRecordingService.ACTION_START))

        service.create().startCommand(0, 1)
        awaitCondition("the session is cleared") { test.container.recordingStateHolder.state.value == null }

        assertTrue(shadowOf(service.get()).isStoppedBySelf)
    }

    private fun startRefusedBy(error: Exception) {
        walkId = runBlocking { test.container.walkRecordingController.start() }.let { test.launcher.started.single() }
        service = Robolectric.buildService(WalkRecordingService::class.java, intent(WalkRecordingService.ACTION_START))
        service.create()
        shadowOf(service.get()).setThrowInStartForeground(error)

        service.startCommand(0, 1)
        awaitCondition("the walk is discarded and the service has stopped") {
            row() == null && shadowOf(service.get()).isStoppedBySelf
        }
    }

    @Test
    fun AC33_a_start_the_system_refuses_for_missing_permission_discards_the_walk_and_stops() {
        startRefusedBy(SecurityException("no location permission"))

        assertNull(test.container.recordingStateHolder.state.value)
        assertTrue(shadowOf(service.get()).isStoppedBySelf)
        assertEquals(0, test.walkSource.subscriptions)
    }

    @Test
    fun a_foreground_start_the_system_refuses_discards_the_walk_and_stops() {
        startRefusedBy(IllegalStateException("not allowed"))

        assertNull(test.container.recordingStateHolder.state.value)
        assertTrue(shadowOf(service.get()).isStoppedBySelf)
    }

    @Test
    fun a_start_without_a_known_action_stops_the_service() {
        service = Robolectric.buildService(WalkRecordingService::class.java, Intent(app, WalkRecordingService::class.java))

        service.create().startCommand(0, 1)

        assertTrue(shadowOf(service.get()).isStoppedBySelf)
        assertFalse(test.container.recordingStateHolder.isRecording)
    }

    @Test
    fun the_service_asks_not_to_be_restarted_by_the_system() {
        val plain = Intent(app, WalkRecordingService::class.java)
        service = Robolectric.buildService(WalkRecordingService::class.java, plain)
        service.create()

        val result = service.get().onStartCommand(plain, 0, 1)

        assertEquals(android.app.Service.START_NOT_STICKY, result)
    }

    @Test
    fun destroying_the_service_clears_the_session_and_leaves_the_walk_for_recovery() {
        startWalk()

        service.destroy()

        assertNull(test.container.recordingStateHolder.state.value)
        assertEquals(WalkStatus.RECORDING, row()!!.status)
    }
}
