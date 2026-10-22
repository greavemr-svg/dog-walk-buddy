package io.github.greavemrsvg.dogwalkbuddy.ui.check

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

/**
 * Tester edge cases for the strip's hour labels (AC23, owner decisions "Follow phone setting" and `"2pm"`): midnight
 * and noon on their own, the labels do not depend on the phone's language, and the zone is the device's.
 */
class HourLabelFormatEdgeTest {
    private val defaultLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(defaultLocale)

    private fun twelveHour(zone: ZoneId = ZoneOffset.UTC) = HourLabelFormat(zone, is24Hour = false)

    private fun twentyFourHour(zone: ZoneId = ZoneOffset.UTC) = HourLabelFormat(zone, is24Hour = true)

    @Test
    fun UX_AC23_midnight_reads_12am_in_12_hour_mode_and_00_in_24_hour_mode() {
        val midnight = Instant.parse("2026-10-20T00:00:00Z")

        assertEquals("12am", twelveHour().format(midnight))
        assertEquals("00", twentyFourHour().format(midnight))
    }

    @Test
    fun UX_AC23_noon_reads_12pm_and_the_hours_either_side_read_11am_and_1pm() {
        assertEquals("11am", twelveHour().format(Instant.parse("2026-10-19T11:00:00Z")))
        assertEquals("12pm", twelveHour().format(Instant.parse("2026-10-19T12:00:00Z")))
        assertEquals("1pm", twelveHour().format(Instant.parse("2026-10-19T13:00:00Z")))
        assertEquals("12", twentyFourHour().format(Instant.parse("2026-10-19T12:00:00Z")))
    }

    @Test
    fun UX_AC23_a_full_day_in_12_hour_mode_has_no_space_no_minutes_no_leading_zero_and_lowercase_am_pm() {
        val start = Instant.parse("2026-10-19T00:00:00Z")
        val labels = (0L until 24L).map { twelveHour().format(start.plusSeconds(it * 3_600L)) }

        val expected =
            listOf("12am") + (1..11).map { "${it}am" } + listOf("12pm") + (1..11).map { "${it}pm" }
        assertEquals(expected, labels)
    }

    @Test
    fun UX_AC23_the_labels_do_not_change_with_the_phone_language() {
        val twoPm = Instant.parse("2026-10-19T14:00:00Z")
        listOf(Locale.US, Locale.GERMANY, Locale.JAPAN, Locale.forLanguageTag("ar-EG")).forEach { locale ->
            Locale.setDefault(locale)
            assertEquals("12-hour in $locale", "2pm", twelveHour().format(twoPm))
            assertEquals("24-hour in $locale", "14", twentyFourHour().format(twoPm))
        }
    }

    @Test
    fun UX_AC23_the_label_is_the_device_zone_hour_not_the_utc_hour() {
        val twoPmUtc = Instant.parse("2026-10-19T14:00:00Z")

        assertEquals("10am", twelveHour(ZoneId.of("America/New_York")).format(twoPmUtc))
        assertEquals("10", twentyFourHour(ZoneId.of("America/New_York")).format(twoPmUtc))
        assertEquals("11pm", twelveHour(ZoneId.of("Asia/Tokyo")).format(twoPmUtc))
        assertEquals("23", twentyFourHour(ZoneId.of("Asia/Tokyo")).format(twoPmUtc))
    }

    @Test
    fun UX_AC23_across_the_autumn_clock_change_each_label_is_the_local_start_of_its_hour() {
        // UK clocks go back at 01:00 UTC on 25 October 2026 (02:00 BST becomes 01:00 GMT).
        val london = ZoneId.of("Europe/London")
        val start = Instant.parse("2026-10-24T23:00:00Z")
        val labels = (0L until 4L).map { twentyFourHour(london).format(start.plusSeconds(it * 3_600L)) }

        assertEquals(listOf("00", "01", "01", "02"), labels)
    }
}
