package io.github.greavemrsvg.dogwalkbuddy.ui.check

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class BestWindowFormatterEdgeCasesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val london = ZoneId.of("Europe/London")

    // 2026-10-19 is BST (UTC+1).
    private fun at(hourLocal: Int) = Instant.parse("2026-10-19T00:00:00Z").plusSeconds((hourLocal - 1) * 3600L)

    private fun formatter(is24: Boolean) = BestWindowFormatter(context.resources, london) { is24 }

    @Test
    fun AC17_now_until_follows_12_hour_format() {
        val t = formatter(false).format(BestWindow.Window(at(10), at(12), Rating.GO, startsNow = true))
        assertEquals("now until 12:00 pm", t.text.lowercase())
    }

    @Test
    fun AC16_range_across_noon_in_12_hour_format() {
        val t = formatter(false).format(BestWindow.Window(at(11), at(13), Rating.GO, startsNow = false))
        assertEquals("11:00 am-1:00 pm", t.text.lowercase())
    }

    @Test
    fun AC16_24_hour_format_is_zero_padded() {
        val t = formatter(true).format(BestWindow.Window(at(7), at(9), Rating.GO, startsNow = false))
        assertEquals("07:00-09:00", t.text)
    }

    @Test
    fun AC16_window_ending_at_midnight_is_shown_in_local_time() {
        val t = formatter(true).format(BestWindow.Window(at(22), at(24), Rating.GO, startsNow = false))
        assertEquals("22:00-00:00", t.text)
    }

    @Test
    fun AC18_caution_window_starting_now_has_label_and_now_text() {
        val t = formatter(true).format(BestWindow.Window(at(10), at(12), Rating.CAUTION, startsNow = true))
        assertEquals("Now until 12:00", t.text)
        assertEquals("Best available: Caution", t.label)
    }

    @Test
    fun AC16_local_zone_is_applied_not_utc() {
        val t = formatter(true).format(BestWindow.Window(at(15), at(17), Rating.GO, startsNow = false))
        assertEquals("15:00-17:00", t.text)
    }
}
