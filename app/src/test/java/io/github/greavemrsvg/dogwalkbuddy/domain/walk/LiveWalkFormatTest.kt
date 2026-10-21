package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class LiveWalkFormatTest {
    @Test
    fun AC28_elapsed_time_is_shown_as_hours_minutes_and_seconds() {
        assertEquals("0:00:00", LiveWalkFormat.elapsed(Duration.ZERO))
        assertEquals("0:00:05", LiveWalkFormat.elapsed(Duration.ofSeconds(5)))
        assertEquals("0:10:00", LiveWalkFormat.elapsed(Duration.ofMinutes(10)))
        assertEquals("1:02:03", LiveWalkFormat.elapsed(Duration.ofSeconds(3723)))
    }

    @Test
    fun elapsed_time_floors_to_whole_seconds() {
        assertEquals("0:00:01", LiveWalkFormat.elapsed(Duration.ofMillis(1999)))
    }

    @Test
    fun a_negative_elapsed_time_shows_zero() {
        assertEquals("0:00:00", LiveWalkFormat.elapsed(Duration.ofSeconds(-30)))
    }

    @Test
    fun AC28_distance_is_shown_in_miles_with_two_decimals() {
        assertEquals("0.00", LiveWalkFormat.miles(0.0))
        assertEquals("1.00", LiveWalkFormat.miles(1609.344))
        assertEquals("1.24", LiveWalkFormat.miles(2000.0))
        assertEquals("0.00", LiveWalkFormat.miles(-5.0))
    }
}
