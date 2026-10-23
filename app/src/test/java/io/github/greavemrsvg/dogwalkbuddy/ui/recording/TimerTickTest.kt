package io.github.greavemrsvg.dogwalkbuddy.ui.recording

import io.github.greavemrsvg.dogwalkbuddy.domain.walk.LiveWalkFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** AC34: the recording timer ticks on each whole second since the walk started. */
class TimerTickTest {
    @Test
    fun AC34_on_a_whole_second_the_next_tick_is_one_second_away() {
        assertEquals(1_000L, millisUntilNextWholeSecond(START, START.plusSeconds(5)))
    }

    @Test
    fun AC34_part_way_through_a_second_the_next_tick_is_at_the_next_whole_second() {
        assertEquals(250L, millisUntilNextWholeSecond(START, START.plusMillis(3_750)))
    }

    @Test
    fun AC34_just_before_a_whole_second_the_next_tick_is_one_millisecond_away() {
        assertEquals(1L, millisUntilNextWholeSecond(START, START.plusMillis(1_999)))
    }

    @Test
    fun AC34_a_start_with_a_part_second_is_measured_from_the_start_not_the_wall_clock() {
        val start = Instant.parse("2026-10-23T08:00:00.400Z")

        assertEquals(300L, millisUntilNextWholeSecond(start, Instant.parse("2026-10-23T08:00:10.100Z")))
    }

    @Test
    fun AC34_a_clock_set_before_the_start_still_waits_between_one_millisecond_and_one_second() {
        assertEquals(300L, millisUntilNextWholeSecond(START, START.minusMillis(1_300)))
        assertEquals(1_000L, millisUntilNextWholeSecond(START, START.minusSeconds(2)))
    }

    @Test
    fun AC34_ticking_with_a_slow_clock_read_never_skips_or_repeats_a_second() {
        // Each tick reads the clock a little late, as a busy main thread does; the old fixed one-second wait drifted.
        val lateReadMs = 7L
        var now = START.plusMillis(lateReadMs)
        val shown = mutableListOf(LiveWalkFormat.elapsed(Duration.between(START, now)))
        repeat(TICKS) {
            now = now.plusMillis(millisUntilNextWholeSecond(START, now) + lateReadMs)
            shown += LiveWalkFormat.elapsed(Duration.between(START, now))
        }

        val expected = (0..TICKS).map { LiveWalkFormat.elapsed(Duration.ofSeconds(it.toLong())) }
        assertEquals(expected, shown)
    }

    private companion object {
        val START: Instant = Instant.parse("2026-10-23T08:00:00Z")
        const val TICKS = 600
    }
}
