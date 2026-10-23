package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class WalkFormatsTest {
    @Test
    fun AC35_distance_is_miles_to_one_decimal_place() {
        assertEquals("0.0", DistanceFormat.miles(0.0))
        assertEquals("1.0", DistanceFormat.miles(1609.344))
        assertEquals("1.5", DistanceFormat.miles(2414.016))
        assertEquals("0.3", DistanceFormat.miles(482.8))
        assertEquals("0.0", DistanceFormat.miles(-1.0))
    }

    @Test
    fun AC35_duration_is_hours_and_minutes_floored() {
        assertEquals("0:00", DurationFormat.hoursMinutes(0))
        assertEquals("0:00", DurationFormat.hoursMinutes(59))
        assertEquals("0:50", DurationFormat.hoursMinutes(50 * 60L))
        assertEquals("1:01", DurationFormat.hoursMinutes(3660 + 59))
        assertEquals("100:00", DurationFormat.hoursMinutes(100 * 3600L))
        assertEquals("0:00", DurationFormat.hoursMinutes(-5))
    }

    @Test
    fun the_live_screen_uses_the_same_formats_as_the_history() {
        assertEquals(DurationFormat.hoursMinutes(3723) + ":03", LiveWalkFormat.elapsed(Duration.ofSeconds(3723)))
        assertEquals(DistanceFormat.miles(2000.0), LiveWalkFormat.miles(2000.0))
    }
}
