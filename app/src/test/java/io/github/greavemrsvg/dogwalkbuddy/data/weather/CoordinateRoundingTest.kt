package io.github.greavemrsvg.dogwalkbuddy.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CoordinateRoundingTest {
    @Test
    fun AC23_rounds_the_spec_location_to_51_51_and_minus_0_13() {
        assertEquals("51.51", CoordinateRounding.format(51.507351))
        assertEquals("-0.13", CoordinateRounding.format(-0.127758))
    }

    @Test
    fun AC23_rounds_halves_away_from_zero_for_negative_and_positive_values() {
        assertEquals("-0.13", CoordinateRounding.format(-0.125))
        assertEquals("0.13", CoordinateRounding.format(0.125))
        assertEquals("51.51", CoordinateRounding.format(51.505))
    }

    @Test
    fun AC23_always_gives_two_decimals_with_a_dot() {
        assertEquals("52.50", CoordinateRounding.format(52.5))
        assertEquals("1.00", CoordinateRounding.format(1.0))
    }

    @Test
    fun AC23_round_returns_the_same_value_as_the_formatted_text() {
        assertEquals(51.51, CoordinateRounding.round(51.507351), 0.0)
        assertEquals(-0.13, CoordinateRounding.round(-0.127758), 0.0)
    }

    @Test
    fun AC23_rejects_non_finite_coordinates() {
        assertThrows(IllegalArgumentException::class.java) { CoordinateRounding.round(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { CoordinateRounding.format(Double.POSITIVE_INFINITY) }
    }
}
