package io.github.greavemrsvg.dogwalkbuddy.domain.check

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tester edge cases for whole-number display (AC17, owner decisions "Whole numbers" and "Away from zero"): every
 * owner example in one place, the doubles either side of a half, and negative halves beyond -2.5.
 */
class FactorRoundingEdgeTest {
    @Test
    fun UX_AC17_every_owner_example_rounds_as_decided() {
        val examples =
            mapOf(
                14.5 to "15",
                0.4 to "0",
                24.6 to "25",
                -2.5 to "-3",
                -2.4 to "-2",
                -0.5 to "-1",
                -0.4 to "0",
            )
        examples.forEach { (value, shown) -> assertEquals("$value", shown, FactorRounding.wholeNumber(value).toString()) }
    }

    @Test
    fun UX_AC17_the_doubles_either_side_of_one_half_round_down_and_up() {
        assertEquals(0L, FactorRounding.wholeNumber(Math.nextDown(0.5)))
        assertEquals(1L, FactorRounding.wholeNumber(Math.nextUp(0.5)))
        assertEquals(-1L, FactorRounding.wholeNumber(-Math.nextUp(0.5)))
        assertEquals("0", FactorRounding.wholeNumber(-Math.nextDown(0.5)).toString())
    }

    @Test
    fun UX_AC17_negative_halves_round_away_from_zero_at_any_size() {
        assertEquals(-15L, FactorRounding.wholeNumber(-14.5))
        assertEquals(-41L, FactorRounding.wholeNumber(-40.5))
        assertEquals(-40L, FactorRounding.wholeNumber(-Math.nextDown(40.5)))
    }

    @Test
    fun UX_AC17_whole_numbers_and_large_halves_are_exact() {
        assertEquals(-7L, FactorRounding.wholeNumber(-7.0))
        assertEquals(42L, FactorRounding.wholeNumber(42.0))
        // 2^52 - 0.5 is exactly representable; a true half rounds away from zero.
        assertEquals(4_503_599_627_370_496L, FactorRounding.wholeNumber(4_503_599_627_370_495.5))
        assertEquals(-4_503_599_627_370_496L, FactorRounding.wholeNumber(-4_503_599_627_370_495.5))
    }
}
