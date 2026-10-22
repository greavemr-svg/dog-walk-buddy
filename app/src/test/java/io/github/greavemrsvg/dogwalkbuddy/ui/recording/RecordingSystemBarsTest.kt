package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import android.content.res.Configuration
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.MainActivity
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Recording is always dark (AC4, AC33), so its status and navigation bar icons are light whatever the system theme,
 * including after a theme switch while it is shown. Leaving it restores the icon style that follows the theme.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RecordingSystemBarsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var recordingShown by mutableStateOf(false)

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_MILLIS))
        composeRule.waitForIdle()
    }

    private fun start(): ActivityController<MainActivity> {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.get().setContent { DogWalkBuddyTheme { if (recordingShown) RecordingSystemBars() } }
        idle()
        return controller
    }

    private fun switchNightMode(
        controller: ActivityController<MainActivity>,
        night: Boolean,
    ) {
        val configuration = Configuration(controller.get().resources.configuration)
        val mode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
        controller.configurationChange(configuration)
        idle()
    }

    private fun showRecording(shown: Boolean) {
        recordingShown = shown
        idle()
    }

    private fun lightIcons(activity: MainActivity): Pair<Boolean, Boolean> {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        return controller.isAppearanceLightStatusBars to controller.isAppearanceLightNavigationBars
    }

    private fun assertLightBarIcons(
        activity: MainActivity,
        what: String,
    ) {
        val (status, navigation) = lightIcons(activity)
        assertFalse("$what: status bar icons are light", status)
        assertFalse("$what: navigation bar icons are light", navigation)
    }

    @Test
    fun UX_AC4_with_the_system_light_recording_has_light_bar_icons_and_leaving_restores_dark_icons() {
        val controller = start()
        val activity = controller.get()
        assertTrue("Today in light has dark status bar icons", lightIcons(activity).first)

        showRecording(true)
        assertLightBarIcons(activity, "Recording in light")

        showRecording(false)
        assertTrue("leaving Recording restores dark status bar icons", lightIcons(activity).first)
        controller.pause().stop().destroy()
    }

    @Test
    fun UX_AC4_recording_keeps_light_bar_icons_through_theme_switches_while_it_is_shown() {
        val controller = start()
        val activity = controller.get()
        showRecording(true)

        switchNightMode(controller, night = true)
        assertLightBarIcons(activity, "Recording after a switch to dark")

        switchNightMode(controller, night = false)
        assertLightBarIcons(activity, "Recording after a switch back to light")

        showRecording(false)
        assertTrue("leaving Recording in light restores dark status bar icons", lightIcons(activity).first)
        controller.pause().stop().destroy()
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC4_with_the_system_dark_recording_has_light_bar_icons_and_leaving_keeps_the_dark_theme_style() {
        val controller = start()
        val activity = controller.get()

        showRecording(true)
        assertLightBarIcons(activity, "Recording in dark")

        showRecording(false)
        assertFalse("Today in dark keeps light status bar icons", lightIcons(activity).first)
        controller.pause().stop().destroy()
    }

    private companion object {
        const val FRAME_MILLIS = 100L
    }
}
