package io.github.greavemrsvg.dogwalkbuddy.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** D7 and D8 dates (AC39, AC46): no year in the current year, the year otherwise, both years across a new year. */
class WalkDateTextTest {
    private val range = "%1\$s – %2\$s"

    private fun text(
        now: String = "2026-10-19T14:20:00Z",
        zone: ZoneId = ZoneOffset.UTC,
        is24Hour: Boolean = true,
    ) = WalkDateText(zone, Clock.fixed(Instant.parse(now), ZoneOffset.UTC), is24Hour, range)

    private val walk = Instant.parse("2026-10-19T09:15:00Z")

    @Test
    fun UX_AC39_a_row_date_in_the_current_year_has_no_year() {
        assertEquals("Mon 19 Oct", text().rowDate(walk))
    }

    @Test
    fun UX_AC39_a_row_date_in_another_year_shows_the_year() {
        assertEquals("Mon 19 Oct 2026", text(now = "2027-01-02T10:00:00Z").rowDate(walk))
    }

    @Test
    fun UX_AC46_the_full_date_has_no_year_in_the_current_year_and_the_year_otherwise() {
        assertEquals("Monday 19 October", text().fullDate(walk))
        assertEquals("Monday 19 October 2026", text(now = "2027-10-19T10:00:00Z").fullDate(walk))
    }

    @Test
    fun UX_AC39_a_week_in_one_month_names_the_month_once() {
        assertEquals("Mon 19 – Sun 25 Oct", text().weekRange(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25)))
    }

    @Test
    fun UX_AC39_a_week_across_months_names_both_months() {
        assertEquals("Mon 26 Oct – Sun 1 Nov", text().weekRange(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun UX_AC39_a_week_in_an_earlier_year_shows_the_year_once_at_the_end() {
        val later = text(now = "2027-03-01T10:00:00Z")
        assertEquals("Mon 12 – Sun 18 Oct 2026", later.weekRange(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)))
        assertEquals("Mon 26 Oct – Sun 1 Nov 2026", later.weekRange(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun UX_AC39_a_week_spanning_two_years_always_shows_both_years() {
        val dec = LocalDate.of(2026, 12, 28)
        val jan = LocalDate.of(2027, 1, 3)
        assertEquals("Mon 28 Dec 2026 – Sun 3 Jan 2027", text().weekRange(dec, jan))
        assertEquals("Mon 28 Dec 2026 – Sun 3 Jan 2027", text(now = "2027-01-02T10:00:00Z").weekRange(dec, jan))
    }

    @Test
    fun UX_AC39_the_current_year_is_the_year_of_the_local_date_in_the_device_zone() {
        // 31 Dec 2026 23:30 UTC is already 1 Jan 2027 in Auckland, so a 2026 walk shows its year there but not in UTC.
        val nz = text(now = "2026-12-31T23:30:00Z", zone = ZoneId.of("Pacific/Auckland"))
        assertEquals("Mon 19 Oct 2026", nz.rowDate(walk))
        assertEquals("Mon 19 Oct", text(now = "2026-12-31T23:30:00Z").rowDate(walk))
    }

    @Test
    fun UX_AC39_times_follow_the_device_12_or_24_hour_format() {
        val end = Instant.parse("2026-10-19T22:30:00Z")
        assertEquals("09:15", text().time(walk))
        assertEquals("9:15 am", text(is24Hour = false).time(walk))
        assertEquals("09:15 – 22:30", text().timeRange(walk, end))
        assertEquals("9:15 am – 10:30 pm", text(is24Hour = false).timeRange(walk, end))
    }
}
