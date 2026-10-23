package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DayDotState.FUTURE
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DayDotState.PAST_NO_WALK
import io.github.greavemrsvg.dogwalkbuddy.domain.walk.DayDotState.WALKED
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** The this-week card's day dots (AC41). */
class WeekDotsTest {
    private fun walk(
        id: Long,
        start: String,
    ) = WalkSummary(id, Instant.parse(start), 1800, 1609.344)

    private val thursday = Instant.parse("2026-10-22T12:00:00Z")

    @Test
    fun UX_AC41_monday_and_wednesday_walked_and_today_thursday_gives_the_seven_states() {
        val dots =
            WeeklyTotals.dayDots(
                listOf(walk(1, "2026-10-19T08:00:00Z"), walk(2, "2026-10-21T18:00:00Z"), walk(3, "2026-10-21T07:00:00Z")),
                ZoneOffset.UTC,
                thursday,
            )

        assertEquals(listOf(WALKED, PAST_NO_WALK, WALKED, PAST_NO_WALK, FUTURE, FUTURE, FUTURE), dots.map { it.state })
        assertEquals(listOf(false, false, false, true, false, false, false), dots.map { it.isToday })
        assertEquals((19..25).map { LocalDate.of(2026, 10, it) }, dots.map { it.date })
    }

    @Test
    fun UX_AC41_a_walk_today_shows_today_as_walked() {
        val dots = WeeklyTotals.dayDots(listOf(walk(1, "2026-10-22T07:00:00Z")), ZoneOffset.UTC, thursday)

        assertEquals(WALKED, dots[3].state)
        assertEquals(true, dots[3].isToday)
    }

    @Test
    fun UX_AC41_walks_outside_the_current_week_are_ignored() {
        val dots =
            WeeklyTotals.dayDots(
                listOf(walk(1, "2026-10-18T23:59:00Z"), walk(2, "2026-10-26T00:00:00Z")),
                ZoneOffset.UTC,
                thursday,
            )

        assertEquals(listOf(PAST_NO_WALK, PAST_NO_WALK, PAST_NO_WALK, PAST_NO_WALK, FUTURE, FUTURE, FUTURE), dots.map { it.state })
    }

    @Test
    fun UX_AC41_the_day_of_a_walk_is_its_local_day_in_the_device_zone() {
        // 23:30 UTC on Monday is Tuesday morning in Berlin.
        val dots = WeeklyTotals.dayDots(listOf(walk(1, "2026-10-19T23:30:00Z")), ZoneId.of("Europe/Berlin"), thursday)

        assertEquals(PAST_NO_WALK, dots[0].state)
        assertEquals(WALKED, dots[1].state)
    }

    @Test
    fun UX_AC41_on_sunday_every_day_is_past_or_walked_and_sunday_is_today() {
        val dots = WeeklyTotals.dayDots(emptyList(), ZoneOffset.UTC, Instant.parse("2026-10-25T20:00:00Z"))

        assertEquals(List(7) { PAST_NO_WALK }, dots.map { it.state })
        assertEquals(6, dots.indexOfFirst { it.isToday })
    }
}
