package io.github.greavemrsvg.dogwalkbuddy.ui.check

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** The strip's hour labels follow the device time format (AC23, owner decisions "Follow phone setting" and `"2pm"`). */
class HourLabelFormatTest {
    private val hours = (0L until 12L).map { CURRENT_HOUR.plusSeconds(it * HOUR_SECONDS) }

    @Test
    fun UX_AC23_in_24_hour_mode_the_labels_are_two_digit_hours_from_the_current_hour() {
        val labels = HourLabelFormat(ZoneOffset.UTC, is24Hour = true)

        assertEquals(
            listOf("14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "00", "01"),
            hours.map(labels::format),
        )
    }

    @Test
    fun UX_AC23_in_12_hour_mode_the_labels_are_the_hour_and_am_or_pm_with_no_space() {
        val labels = HourLabelFormat(ZoneOffset.UTC, is24Hour = false)

        assertEquals(
            listOf("2pm", "3pm", "4pm", "5pm", "6pm", "7pm", "8pm", "9pm", "10pm", "11pm", "12am", "1am"),
            hours.map(labels::format),
        )
    }

    @Test
    fun UX_AC23_noon_reads_12pm_and_morning_hours_have_no_leading_zero() {
        val labels = HourLabelFormat(ZoneOffset.UTC, is24Hour = false)

        assertEquals("12pm", labels.format(Instant.parse("2026-10-19T12:00:00Z")))
        assertEquals("9am", labels.format(Instant.parse("2026-10-19T09:00:00Z")))
        assertEquals("09", HourLabelFormat(ZoneOffset.UTC, is24Hour = true).format(Instant.parse("2026-10-19T09:00:00Z")))
    }

    @Test
    fun UX_AC23_labels_are_in_the_device_time_zone() {
        // 13:00 UTC is 14:00 in London on 19 October (BST).
        val london = ZoneId.of("Europe/London")

        assertEquals("14", HourLabelFormat(london, is24Hour = true).format(Instant.parse("2026-10-19T13:00:00Z")))
        assertEquals("2pm", HourLabelFormat(london, is24Hour = false).format(Instant.parse("2026-10-19T13:00:00Z")))
    }

    private companion object {
        val CURRENT_HOUR: Instant = Instant.parse("2026-10-19T14:00:00Z")
        const val HOUR_SECONDS = 3_600L
    }
}
