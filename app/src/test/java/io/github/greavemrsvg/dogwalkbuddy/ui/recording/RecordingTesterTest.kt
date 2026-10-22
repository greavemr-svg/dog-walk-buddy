package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import android.content.res.Configuration
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.MainActivity
import io.github.greavemrsvg.dogwalkbuddy.service.RecordingSession
import io.github.greavemrsvg.dogwalkbuddy.testutil.Wcag
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.DogWalkBuddyTheme
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.RecordingPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/** Tester checks for Recording: Stop's colours in both states (AC37, MVP AC51), the spoken timer (AC34) and the bars (AC4). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RecordingTesterTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val start = Instant.parse("2026-10-19T19:05:00Z")

    private fun showContent(
        elapsed: Duration = Duration.ofMinutes(23).plusSeconds(41),
        stopping: Boolean = false,
    ) {
        composeRule.setContent {
            DogWalkBuddyTheme {
                RecordingContent(
                    session = RecordingSession(walkId = 1, startUtc = start, distanceM = 0.0),
                    now = start.plus(elapsed),
                    route = emptyList(),
                    startedFormat = StartedTimeFormat(ZoneOffset.UTC, true),
                    stopping = stopping,
                    showNotificationsHint = false,
                    onStop = {},
                    onBack = {},
                )
            }
        }
    }

    private fun stopPixels(): PixelMap =
        composeRule
            .onNodeWithTag(STOP_WALK_TAG)
            .performScrollTo()
            .captureToImage()
            .toPixelMap()

    private fun PixelMap.contains(colour: Color): Boolean = (0 until width).any { x -> (0 until height).any { y -> this[x, y] == colour } }

    private fun assertStopColours(
        background: Color,
        content: Color,
    ) {
        val pixels = stopPixels()
        val inset = with(composeRule.density) { 40.dp.roundToPx() }
        assertEquals("Stop fill", background, pixels[inset, pixels.height / 2])
        assertTrue("Stop label and icon are drawn in $content", pixels.contains(content))
    }

    @Test
    fun UX_AC37_stop_label_is_the_stop_foreground_on_the_stop_background_at_7_to_1() {
        showContent()

        composeRule.onNodeWithTag(STOP_WALK_TAG).assertHeightIsAtLeast(72.dp)
        assertStopColours(RecordingPalette.stopBackground, RecordingPalette.stopForeground)
        val ratio = Wcag.contrastRatio(RecordingPalette.stopForeground, RecordingPalette.stopBackground)
        assertTrue("Stop label contrast $ratio is at least 7:1", ratio >= 7.0)
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC37_stop_colours_are_the_same_when_the_system_is_dark() {
        showContent()

        assertStopColours(RecordingPalette.stopBackground, RecordingPalette.stopForeground)
    }

    @Test
    fun UX_AC37_while_saving_stop_stays_72dp_disabled_and_its_label_stays_readable() {
        showContent(stopping = true)

        composeRule
            .onNodeWithTag(STOP_WALK_TAG)
            .performScrollTo()
            .assertTextEquals("Saving your walk")
            .assertIsNotEnabled()
            .assertHeightIsAtLeast(72.dp)
        assertStopColours(RecordingPalette.card, RecordingPalette.onDarkMuted)
        val ratio = Wcag.contrastRatio(RecordingPalette.onDarkMuted, RecordingPalette.card)
        assertTrue("Saving label contrast $ratio is at least 7:1", ratio >= 7.0)
    }

    @Test
    fun UX_AC34_an_hour_two_minutes_and_five_seconds_reads_1_02_05_and_is_spoken_in_words() {
        showContent(elapsed = Duration.ofHours(1).plusMinutes(2).plusSeconds(5))

        composeRule
            .onNodeWithTag(TIMER_TAG, useUnmergedTree = true)
            .assertTextEquals("1:02:05")
            .assertContentDescriptionEquals("1 hour 2 minutes 5 seconds")
    }

    @Test
    fun UX_AC34_a_zero_timer_reads_0_00_00_and_is_spoken_as_0_seconds() {
        showContent(elapsed = Duration.ZERO)

        composeRule
            .onNodeWithTag(TIMER_TAG, useUnmergedTree = true)
            .assertTextEquals("0:00:00")
            .assertContentDescriptionEquals("0 seconds")
    }

    // Status and navigation bar icons, hosted in MainActivity as in RecordingSystemBarsTest.

    private var recordingShown by mutableStateOf(false)

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        composeRule.waitForIdle()
    }

    private fun startActivity(): ActivityController<MainActivity> {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.get().setContent { DogWalkBuddyTheme { if (recordingShown) RecordingSystemBars() } }
        idle()
        return controller
    }

    private fun barIcons(activity: MainActivity): Pair<Boolean, Boolean> {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        return controller.isAppearanceLightStatusBars to controller.isAppearanceLightNavigationBars
    }

    private fun assertBarsRestoredAfterRecording() {
        val controller = startActivity()
        val activity = controller.get()
        val before = barIcons(activity)

        recordingShown = true
        idle()
        assertEquals("Recording has light status and navigation bar icons", false to false, barIcons(activity))

        recordingShown = false
        idle()
        assertEquals("leaving Recording restores both bar icon styles", before, barIcons(activity))

        recordingShown = true
        idle()
        assertEquals("returning to Recording makes the icons light again", false to false, barIcons(activity))
        controller.pause().stop().destroy()
    }

    @Test
    fun UX_AC4_in_system_light_both_bar_icon_styles_come_back_after_leaving_recording() {
        assertBarsRestoredAfterRecording()
    }

    @Test
    @Config(qualifiers = "+night")
    fun UX_AC4_in_system_dark_both_bar_icon_styles_come_back_after_leaving_recording() {
        assertBarsRestoredAfterRecording()
    }

    @Test
    fun UX_AC4_leaving_recording_after_a_switch_to_dark_gives_the_dark_theme_bar_style() {
        val controller = startActivity()
        val activity = controller.get()
        recordingShown = true
        idle()

        val configuration = Configuration(activity.resources.configuration)
        configuration.uiMode =
            (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        controller.configurationChange(configuration)
        idle()
        recordingShown = false
        idle()

        assertEquals("dark theme keeps light status bar icons after leaving", false, barIcons(activity).first)
        controller.pause().stop().destroy()
    }
}
