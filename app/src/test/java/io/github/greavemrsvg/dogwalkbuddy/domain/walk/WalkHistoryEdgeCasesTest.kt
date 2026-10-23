package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class WalkHistoryEdgeCasesTest {
    private val london = ZoneId.of("Europe/London")

    private fun at(local: String) = ZonedDateTime.of(LocalDateTime.parse(local), london).toInstant()

    private fun walk(
        id: Long,
        start: Instant,
        minutes: Long = 10,
        metres: Double = 100.0,
    ) = WalkSummary(id, start, minutes * 60, metres)

    @Test
    fun AC35_distance_rounds_to_one_decimal_place_around_0_05_miles() {
        assertEquals("0.0", DistanceFormat.miles(80.0))
        assertEquals("0.1", DistanceFormat.miles(81.0))
        assertEquals("1.0", DistanceFormat.miles(1609.344 * 0.96))
        assertEquals("1.0", DistanceFormat.miles(1609.344 * 1.04))
        assertEquals("0.0", DistanceFormat.miles(Double.MIN_VALUE))
    }

    @Test
    fun AC35_duration_floors_at_the_minute_and_hour_boundaries() {
        assertEquals("0:59", DurationFormat.hoursMinutes(3599))
        assertEquals("1:00", DurationFormat.hoursMinutes(3600))
        assertEquals("0:01", DurationFormat.hoursMinutes(60))
        assertEquals("0:01", DurationFormat.hoursMinutes(119))
    }

    @Test
    fun AC36_monday_midnight_london_starts_the_new_week_and_the_second_before_ends_the_old_one() {
        assertEquals(LocalDate.of(2026, 10, 12), WeeklyTotals.weekStartOf(at("2026-10-18T23:59:59"), london))
        assertEquals(LocalDate.of(2026, 10, 19), WeeklyTotals.weekStartOf(at("2026-10-19T00:00:00"), london))
        assertEquals(LocalDate.of(2026, 10, 19), WeeklyTotals.weekStartOf(at("2026-10-25T23:59:59"), london))
    }

    @Test
    fun AC36_in_summer_time_00_30_monday_is_still_sunday_in_utc_but_counts_in_the_new_local_week() {
        val start = at("2026-10-19T00:30:00")
        assertEquals(LocalDate.of(2026, 10, 18), start.atZone(ZoneOffset.UTC).toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 19), WeeklyTotals.weekStartOf(start, london))
    }

    @Test
    fun AC36_a_walk_spanning_midnight_sunday_to_monday_counts_in_the_week_it_started() {
        val groups = WeeklyTotals.group(listOf(walk(1, at("2026-10-18T23:50:00"), minutes = 30)), london)

        assertEquals(1, groups.size)
        assertEquals(LocalDate.of(2026, 10, 12), groups[0].weekStart)
        assertEquals(30 * 60L, groups[0].totalSeconds)
        assertEquals(LocalDate.of(2026, 10, 18), groups[0].weekEnd)
    }

    @Test
    fun AC36_walks_with_the_same_start_time_are_all_kept_and_totalled() {
        val t = at("2026-10-14T10:00:00")
        val groups = WeeklyTotals.group(listOf(walk(1, t, 5, 100.0), walk(2, t, 7, 200.0)), london)

        assertEquals(1, groups.size)
        assertEquals(2, groups[0].count)
        assertEquals(12 * 60L, groups[0].totalSeconds)
        assertEquals(300.0, groups[0].totalM, 0.0)
        assertEquals(listOf(1L, 2L), groups[0].walks.map { it.id })
    }

    @Test
    fun AC36_sunday_after_the_spring_clock_change_belongs_to_that_weeks_monday() {
        // 29 Mar 2026 is the Sunday the clocks go forward; 23:30 UTC is 00:30 BST on Monday 30 Mar.
        val start = Instant.parse("2026-03-29T23:30:00Z")
        assertEquals(LocalDate.of(2026, 3, 30), WeeklyTotals.weekStartOf(start, london))
        assertEquals(LocalDate.of(2026, 3, 23), WeeklyTotals.weekStartOf(start, ZoneOffset.UTC))
    }

    @Test
    fun AC35_five_hundred_walks_group_newest_first_with_correct_totals() {
        val base = at("2026-10-19T12:00:00")
        val walks = (1..500).map { walk(it.toLong(), base.minusSeconds(it * 8 * 3600L), 10, 100.0) }

        val groups = WeeklyTotals.group(walks.shuffled(java.util.Random(1)), london)

        assertEquals(500, groups.sumOf { it.count })
        assertEquals(500 * 600L, groups.sumOf { it.totalSeconds })
        assertEquals(groups.map { it.weekStart }.sortedDescending(), groups.map { it.weekStart })
        groups.forEach { g -> assertEquals(g.walks.map { it.startUtc }.sortedDescending(), g.walks.map { it.startUtc }) }
    }
}
