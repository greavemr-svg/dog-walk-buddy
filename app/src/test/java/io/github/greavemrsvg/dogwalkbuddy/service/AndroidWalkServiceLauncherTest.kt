package io.github.greavemrsvg.dogwalkbuddy.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AndroidWalkServiceLauncherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val launcher = AndroidWalkServiceLauncher(context)

    @Test
    fun AC28_start_launches_the_recording_service_with_the_walk_id_as_a_foreground_service() {
        launcher.start(42)

        val intent = shadowOf(context as android.app.Application).nextStartedService
        assertNotNull(intent)
        assertEquals(WalkRecordingService::class.java.name, intent.component!!.className)
        assertEquals(WalkRecordingService.ACTION_START, intent.action)
        assertEquals(42L, intent.getLongExtra(WalkRecordingService.EXTRA_WALK_ID, -1))
    }

    @Test
    fun AC30_stop_sends_the_stop_action_to_the_recording_service() {
        launcher.stop()

        val intent = shadowOf(context as android.app.Application).nextStartedService
        assertEquals(WalkRecordingService::class.java.name, intent.component!!.className)
        assertEquals(WalkRecordingService.ACTION_STOP, intent.action)
    }
}
