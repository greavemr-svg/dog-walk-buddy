package io.github.greavemrsvg.dogwalkbuddy.domain.check

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** The 12-hour selection (AC17) at the local-time boundaries, and when the stored series starts in the past. */
class CheckRaterTimeBoundaryTest {
    private fun hours(
        count: Int,
        from: Instant,
    ) = (0 until count).map { HourlyConditions(from.plus(Duration.ofHours(it.toLong())), 15.0, 0.0, 5.0, 100.0) }

    @Test
    fun AC17_a_series_that_started_in_the_past_is_cut_to_start_at_the_hour_containing_now() {
        val start = Instant.parse("2026-10-19T08:00:00Z")
        val now = Instant.parse("2026-10-19T13:30:00Z")

        val selected = CheckRater.upcomingHours(hours(48, start), now)

        assertEquals(Instant.parse("2026-10-19T13:00:00Z"), selected.first().startUtc)
        assertEquals(12, selected.size)
    }

    @Test
    fun AC17_the_selection_is_not_disturbed_by_the_clocks_going_back_at_the_end_of_british_summer_time() {
        // 2026-10-25 01:00 UTC is when UK clocks go back: local 01:00 happens twice. Periods stay consecutive in UTC.
        val start = Instant.parse("2026-10-24T22:00:00Z")
        val now = Instant.parse("2026-10-25T00:20:00Z")

        val selected = CheckRater.upcomingHours(hours(48, start), now)

        assertEquals(12, selected.size)
        assertEquals(Instant.parse("2026-10-25T00:00:00Z"), selected.first().startUtc)
        assertEquals(Instant.parse("2026-10-25T11:00:00Z"), selected.last().startUtc)
    }

    @Test
    fun AC17_the_selection_is_not_disturbed_by_the_clocks_going_forward_in_spring() {
        val start = Instant.parse("2027-03-27T22:00:00Z")
        val now = Instant.parse("2027-03-28T00:59:59Z")

        val selected = CheckRater.upcomingHours(hours(48, start), now)

        assertEquals(12, selected.size)
        assertEquals(Instant.parse("2027-03-28T00:00:00Z"), selected.first().startUtc)
    }

    @Test
    fun AC17_a_window_that_runs_across_midnight_keeps_all_12_hours() {
        val now = Instant.parse("2026-12-31T23:10:00Z")

        val selected = CheckRater.upcomingHours(hours(48, Instant.parse("2026-12-31T20:00:00Z")), now)

        assertEquals(12, selected.size)
        assertEquals(Instant.parse("2027-01-01T10:00:00Z"), selected.last().startUtc)
    }

    @Test
    fun AC15_a_stored_series_that_ended_before_now_is_never_rated() {
        val start = Instant.parse("2026-10-19T00:00:00Z")
        val now = Instant.parse("2026-10-19T12:00:00Z")
        val data = CheckData.Available(hours(12, start), start, 51.51, -0.13, true)

        assertTrue(CheckRater.upcomingHours(data.hours, now).isEmpty())
        assertEquals(CheckOutcome.CantCheck, CheckRater.rate(data, HeatSensitivity.NORMAL, now))
    }
}
