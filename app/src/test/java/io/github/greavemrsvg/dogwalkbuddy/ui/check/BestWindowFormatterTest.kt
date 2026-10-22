package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class BestWindowFormatterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val london = ZoneId.of("Europe/London")

    // 2026-10-19 is in BST (UTC+1), so 09:00Z shows as 10:00.
    private fun at(hourLocal: Int) = Instant.parse("2026-10-19T00:00:00Z").plusSeconds((hourLocal - 1) * 3600L)

    private fun formatter(is24: Boolean = true) = BestWindowFormatter(context.resources, london) { is24 }

    @Test
    fun AC17_window_starting_now_reads_now_until_end() {
        val text = formatter().format(BestWindow.Window(at(10), at(12), Rating.GO, startsNow = true))

        assertEquals("Now until 12:00", text.text)
        assertNull(text.label)
    }

    @Test
    fun AC16_go_window_reads_start_to_end() {
        val text = formatter().format(BestWindow.Window(at(12), at(15), Rating.GO, startsNow = false))

        assertEquals("12:00-15:00", text.text)
        assertNull(text.label)
    }

    @Test
    fun AC18_caution_window_is_labelled_best_available() {
        val text = formatter().format(BestWindow.Window(at(15), at(17), Rating.CAUTION, startsNow = false))

        assertEquals("15:00-17:00", text.text)
        assertEquals("Best available: Caution", text.label)
    }

    @Test
    fun AC19_no_good_time_text() {
        val text = formatter().format(BestWindow.NoGoodTime)

        assertEquals("No good time in the next 12 hours", text.text)
        assertNull(text.label)
    }

    @Test
    fun window_times_follow_the_device_12_hour_format() {
        val text = formatter(is24 = false).format(BestWindow.Window(at(15), at(17), Rating.GO, startsNow = false))

        assertEquals("3:00 pm-5:00 pm", text.text.lowercase())
    }

    @Test
    fun AC1_to_AC14_every_reason_maps_to_its_exact_spec_text() {
        val expected =
            mapOf(
                Reason.GOOD to "Good conditions for a walk",
                Reason.WARM to "Warm: keep it short and bring water",
                Reason.TOO_HOT to "Too hot to walk safely",
                Reason.PAVEMENT_WARM to "Pavement may be hot: test it with the back of your hand",
                Reason.PAVEMENT_TOO_HOT to "Pavement likely too hot for paws",
                Reason.RAIN to "Rain expected",
                Reason.HEAVY_RAIN to "Heavy rain",
                Reason.WINDY to "Windy",
                Reason.DANGEROUS_WINDS to "Dangerous winds",
                Reason.FREEZING to "Freezing: watch for ice and grit",
                Reason.DATA_MISSING to "Some weather data isn't available",
            )

        assertEquals(Reason.entries.toSet(), expected.keys)
        expected.forEach { (reason, text) -> assertEquals(text, context.getString(reason.textRes())) }
    }
}
