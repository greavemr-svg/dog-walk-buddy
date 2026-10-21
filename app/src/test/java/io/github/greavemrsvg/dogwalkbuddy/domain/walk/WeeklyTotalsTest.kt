package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WeeklyTotalsTest {
    private val london = ZoneId.of("Europe/London")

    private fun walk(
        id: Long,
        local: String,
        minutes: Long,
        metres: Double,
        zone: ZoneId = london,
    ) = WalkSummary(id, ZonedDateTime.of(java.time.LocalDateTime.parse(local), zone).toInstant(), minutes * 60, metres)

    private val miles1 = 1609.344
    private val spec =
        listOf(
            walk(1, "2026-10-12T08:00", 30, miles1),
            walk(2, "2026-10-18T23:30", 20, miles1 / 2),
            walk(3, "2026-10-19T00:10", 10, miles1 * 0.3),
        )

    @Test
    fun AC36_walks_are_grouped_by_the_local_week_they_started_in_with_their_totals() {
        val weeks = WeeklyTotals.group(spec, london)

        assertEquals(2, weeks.size)
        assertEquals(LocalDate.of(2026, 10, 19), weeks[0].weekStart)
        assertEquals(LocalDate.of(2026, 10, 25), weeks[0].weekEnd)
        assertEquals(1, weeks[0].count)
        assertEquals("0:10", DurationFormat.hoursMinutes(weeks[0].totalSeconds))
        assertEquals("0.3", DistanceFormat.miles(weeks[0].totalM))
        assertEquals(LocalDate.of(2026, 10, 12), weeks[1].weekStart)
        assertEquals(LocalDate.of(2026, 10, 18), weeks[1].weekEnd)
        assertEquals(listOf(2L, 1L), weeks[1].walks.map { it.id })
        assertEquals(2, weeks[1].count)
        assertEquals("0:50", DurationFormat.hoursMinutes(weeks[1].totalSeconds))
        assertEquals("1.5", DistanceFormat.miles(weeks[1].totalM))
    }

    @Test
    fun AC36_the_week_is_decided_by_local_time_not_utc() {
        // 00:10 BST on Monday 19 Oct is 23:10 UTC on Sunday 18 Oct.
        val monday = spec[2]
        assertEquals(LocalDate.of(2026, 10, 18), monday.startUtc.atZone(ZoneId.of("UTC")).toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 19), WeeklyTotals.weekStartOf(monday.startUtc, london))
    }

    @Test
    fun AC36_the_week_that_has_the_clocks_going_back_still_runs_monday_to_sunday() {
        val walks =
            listOf(
                walk(1, "2026-10-24T23:30", 10, 100.0), // Saturday, BST
                walk(2, "2026-10-25T00:30", 10, 100.0), // Sunday, the repeated hour
                walk(3, "2026-10-25T23:30", 10, 100.0), // Sunday, GMT
                walk(4, "2026-10-26T00:10", 10, 100.0), // Monday, GMT
            )

        val weeks = WeeklyTotals.group(walks, london)

        assertEquals(listOf(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 19)), weeks.map { it.weekStart })
        assertEquals(listOf(1, 3), weeks.map { it.count })
    }

    @Test
    fun AC36_the_spring_clock_change_week_keeps_monday_and_sunday_walks_in_it() {
        val walks = listOf(walk(1, "2026-03-23T00:10", 10, 1.0), walk(2, "2026-03-29T23:30", 10, 1.0))

        val weeks = WeeklyTotals.group(walks, london)

        assertEquals(1, weeks.size)
        assertEquals(LocalDate.of(2026, 3, 23), weeks[0].weekStart)
        assertEquals(2, weeks[0].count)
    }

    @Test
    fun AC36_no_walks_gives_no_week_groups() {
        assertTrue(WeeklyTotals.group(emptyList(), london).isEmpty())
    }

    @Test
    fun AC37_this_week_with_no_walks_is_present_and_zero() {
        val now = Instant.parse("2026-10-21T10:00:00Z")

        val week = WeeklyTotals.thisWeek(spec.take(2), london, now)

        assertEquals(LocalDate.of(2026, 10, 19), week.weekStart)
        assertEquals(0, week.count)
        assertEquals("0:00", DurationFormat.hoursMinutes(week.totalSeconds))
        assertEquals("0.0", DistanceFormat.miles(week.totalM))
    }

    @Test
    fun AC37_this_week_sums_only_the_walks_that_started_in_the_current_week() {
        val now = Instant.parse("2026-10-21T10:00:00Z")

        val week = WeeklyTotals.thisWeek(spec, london, now)

        assertEquals(listOf(3L), week.walks.map { it.id })
        assertEquals(1, week.count)
        assertEquals(600L, week.totalSeconds)
    }
}
