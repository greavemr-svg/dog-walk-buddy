package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class LiveWalkFormatTest {
    @Test
    fun AC28_elapsed_time_is_shown_as_hours_and_minutes() {
        assertEquals("0:00", LiveWalkFormat.elapsed(Duration.ZERO))
        assertEquals("0:10", LiveWalkFormat.elapsed(Duration.ofMinutes(10)))
        assertEquals("1:02", LiveWalkFormat.elapsed(Duration.ofSeconds(3723)))
        assertEquals("12:00", LiveWalkFormat.elapsed(Duration.ofHours(12)))
    }

    @Test
    fun elapsed_time_floors_to_whole_minutes() {
        assertEquals("0:00", LiveWalkFormat.elapsed(Duration.ofSeconds(59)))
        assertEquals("0:01", LiveWalkFormat.elapsed(Duration.ofSeconds(60)))
        assertEquals("1:01", LiveWalkFormat.elapsed(Duration.ofMinutes(61).plusSeconds(59)))
    }

    @Test
    fun a_negative_elapsed_time_shows_zero() {
        assertEquals("0:00", LiveWalkFormat.elapsed(Duration.ofSeconds(-30)))
    }

    @Test
    fun AC28_distance_is_shown_in_miles_with_one_decimal() {
        assertEquals("0.0", LiveWalkFormat.miles(0.0))
        assertEquals("1.0", LiveWalkFormat.miles(1609.344))
        assertEquals("1.2", LiveWalkFormat.miles(2000.0))
        assertEquals("0.1", LiveWalkFormat.miles(100.0))
        assertEquals("0.0", LiveWalkFormat.miles(-5.0))
    }
}
