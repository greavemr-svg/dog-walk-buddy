package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class LiveWalkFormatTest {
    @Test
    fun AC28_elapsed_time_is_shown_as_hours_and_minutes() {
        assertEquals("0:00:00", LiveWalkFormat.elapsed(Duration.ZERO))
        assertEquals("0:10:00", LiveWalkFormat.elapsed(Duration.ofMinutes(10)))
        assertEquals("1:02:03", LiveWalkFormat.elapsed(Duration.ofSeconds(3723)))
        assertEquals("12:00:00", LiveWalkFormat.elapsed(Duration.ofHours(12)))
    }

    @Test
    fun elapsed_time_floors_to_whole_seconds() {
        assertEquals("0:00:59", LiveWalkFormat.elapsed(Duration.ofSeconds(59)))
        assertEquals("0:01:00", LiveWalkFormat.elapsed(Duration.ofSeconds(60)))
        assertEquals("0:00:59", LiveWalkFormat.elapsed(Duration.ofMillis(59_999)))
        assertEquals("1:01:59", LiveWalkFormat.elapsed(Duration.ofMinutes(61).plusSeconds(59)))
    }

    @Test
    fun a_negative_elapsed_time_shows_zero() {
        assertEquals("0:00:00", LiveWalkFormat.elapsed(Duration.ofSeconds(-30)))
    }

    @Test
    fun AC28_distance_is_shown_in_miles_with_one_decimal() {
        assertEquals("0.0", LiveWalkFormat.miles(0.0))
        assertEquals("1.0", LiveWalkFormat.miles(1609.344))
        assertEquals("1.2", LiveWalkFormat.miles(2000.0))
        assertEquals("0.1", LiveWalkFormat.miles(100.0))
        assertEquals("0.0", LiveWalkFormat.miles(-5.0))
    }

    @Test
    fun AC28_elapsed_time_boundaries_59_59_and_one_hour() {
        assertEquals("0:59:59", LiveWalkFormat.elapsed(Duration.ofMinutes(59).plusSeconds(59)))
        assertEquals("1:00:00", LiveWalkFormat.elapsed(Duration.ofHours(1)))
        assertEquals("1:00:59", LiveWalkFormat.elapsed(Duration.ofHours(1).plusSeconds(59)))
    }

    @Test
    fun AC28_distance_rounds_to_the_nearest_tenth_of_a_mile() {
        assertEquals("0.0", LiveWalkFormat.miles(0.04 * 1609.344))
        assertEquals("0.1", LiveWalkFormat.miles(0.06 * 1609.344))
        assertEquals("0.9", LiveWalkFormat.miles(0.94 * 1609.344))
        assertEquals("1.0", LiveWalkFormat.miles(0.96 * 1609.344))
        assertEquals("10.0", LiveWalkFormat.miles(10 * 1609.344))
    }
}
