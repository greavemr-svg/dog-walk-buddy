package io.github.greavemrsvg.dogwalkbuddy.domain.check

import org.junit.Assert.assertEquals
import org.junit.Test

/** Whole numbers, rounded half up, and halves of negative values away from zero (AC17, D3). */
class FactorRoundingTest {
    @Test
    fun UX_AC17_positive_values_round_half_up() {
        assertEquals(15L, FactorRounding.wholeNumber(14.5))
        assertEquals(14L, FactorRounding.wholeNumber(14.4))
        assertEquals(0L, FactorRounding.wholeNumber(0.4))
        assertEquals(1L, FactorRounding.wholeNumber(0.5))
        assertEquals(25L, FactorRounding.wholeNumber(24.6))
        assertEquals(26L, FactorRounding.wholeNumber(26.3))
        // Half to even would give 2 here; half up gives 3.
        assertEquals(3L, FactorRounding.wholeNumber(2.5))
        assertEquals(0L, FactorRounding.wholeNumber(0.0))
    }

    @Test
    fun UX_AC17_negative_halves_round_away_from_zero() {
        assertEquals(-3L, FactorRounding.wholeNumber(-2.5))
        assertEquals(-2L, FactorRounding.wholeNumber(-2.4))
        assertEquals(-1L, FactorRounding.wholeNumber(-0.5))
    }

    @Test
    fun UX_AC17_a_value_that_rounds_to_zero_has_no_minus_sign() {
        assertEquals("0", FactorRounding.wholeNumber(-0.4).toString())
        assertEquals("0", FactorRounding.wholeNumber(-0.0).toString())
    }

    @Test
    fun UX_AC17_the_largest_double_below_a_half_rounds_down_not_up() {
        // floor(x + 0.5) gave 1 here, because 0.49999999999999994 + 0.5 rounds to 1.0 in floating point.
        assertEquals(0L, FactorRounding.wholeNumber(0.49999999999999994))
        assertEquals("0", FactorRounding.wholeNumber(-0.49999999999999994).toString())
        assertEquals(14L, FactorRounding.wholeNumber(Math.nextDown(14.5)))
        assertEquals(-14L, FactorRounding.wholeNumber(-Math.nextDown(14.5)))
        assertEquals(15L, FactorRounding.wholeNumber(Math.nextUp(14.5)))
    }
}
