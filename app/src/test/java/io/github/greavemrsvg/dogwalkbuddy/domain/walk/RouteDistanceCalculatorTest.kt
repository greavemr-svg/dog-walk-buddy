package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RouteDistanceCalculatorTest {
    private val start = Instant.parse("2026-10-19T09:00:00Z")

    private fun fix(
        lat: Double,
        accuracy: Double? = 5.0,
        seconds: Long = 0,
        lon: Double = -0.12,
    ) = LocationFix(lat, lon, accuracy, start.plusSeconds(seconds))

    // 0.0001 degrees of latitude is about 11.12 m.
    private val step = 0.0001

    @Test
    fun haversine_matches_the_length_of_a_degree_of_latitude() {
        val metres = RouteDistanceCalculator.haversineMetres(fix(51.0), fix(52.0))

        assertEquals(111_195.0, metres, 5.0)
    }

    @Test
    fun haversine_of_the_same_point_is_zero() {
        assertEquals(0.0, RouteDistanceCalculator.haversineMetres(fix(51.0), fix(51.0)), 0.0)
    }

    @Test
    fun AC32_fixes_with_accuracy_over_25_m_are_rejected_and_25_m_is_accepted() {
        val calculator = RouteDistanceCalculator()
        calculator.add(fix(51.0, seconds = 1))

        assertEquals(FixResult.Rejected, calculator.add(fix(51.0 + step, accuracy = 25.1, seconds = 2)))
        assertTrue(calculator.add(fix(51.0 + step, accuracy = 25.0, seconds = 3)) is FixResult.Accepted)
    }

    @Test
    fun fixes_with_no_accuracy_are_rejected() {
        assertEquals(FixResult.Rejected, RouteDistanceCalculator().add(fix(51.0, accuracy = null, seconds = 1)))
    }

    @Test
    fun a_rejected_fix_adds_no_distance() {
        val calculator = RouteDistanceCalculator()
        calculator.add(fix(51.0, seconds = 1))
        calculator.add(fix(51.5, accuracy = 80.0, seconds = 2))

        assertEquals(0.0, calculator.distanceM, 0.0)
    }

    @Test
    fun fixes_not_after_the_previous_fix_are_rejected() {
        val calculator = RouteDistanceCalculator()
        calculator.add(fix(51.0, seconds = 10))

        assertEquals(FixResult.Rejected, calculator.add(fix(51.0 + step, seconds = 10)))
        assertEquals(FixResult.Rejected, calculator.add(fix(51.0 + step, seconds = 9)))
        assertTrue(calculator.add(fix(51.0 + step, seconds = 11)) is FixResult.Accepted)
    }

    @Test
    fun the_first_fix_is_accepted_and_adds_nothing() {
        val result = RouteDistanceCalculator().add(fix(51.0, seconds = 1))

        assertEquals(FixResult.Accepted(addedM = 0.0, totalM = 0.0), result)
    }

    @Test
    fun AC32_a_move_smaller_than_the_jitter_floor_adds_no_distance() {
        val calculator = RouteDistanceCalculator()
        calculator.add(fix(51.0, accuracy = 3.0, seconds = 1))

        // About 2.2 m, under the 3 m floor.
        val result = calculator.add(fix(51.0 + 0.00002, accuracy = 3.0, seconds = 2))

        assertEquals(FixResult.Accepted(addedM = 0.0, totalM = 0.0), result)
    }

    @Test
    fun AC32_a_move_smaller_than_half_the_combined_accuracy_adds_no_distance() {
        val calculator = RouteDistanceCalculator()
        calculator.add(fix(51.0, accuracy = 10.0, seconds = 1))

        // About 6.7 m, over the 3 m floor but under half of the combined accuracy (7.07 m).
        val result = calculator.add(fix(51.0 + 0.00006, accuracy = 10.0, seconds = 2))

        assertEquals(0.0, (result as FixResult.Accepted).addedM, 0.0)
    }

    @Test
    fun a_move_at_the_threshold_is_counted() {
        val calculator = RouteDistanceCalculator()
        calculator.add(fix(51.0, accuracy = 5.0, seconds = 1))

        // About 11.1 m, well over max(3, 3.54).
        val result = calculator.add(fix(51.0 + step, accuracy = 5.0, seconds = 2)) as FixResult.Accepted

        assertEquals(11.12, result.addedM, 0.1)
        assertEquals(result.addedM, calculator.distanceM, 0.0)
    }

    @Test
    fun small_moves_keep_adding_up_from_the_last_counted_fix() {
        val calculator = RouteDistanceCalculator()
        calculator.add(fix(51.0, accuracy = 5.0, seconds = 1))

        // Each step is about 2.2 m, which is too small alone. After 2 steps (4.4 m) the move is counted.
        calculator.add(fix(51.0 + 0.00002, accuracy = 5.0, seconds = 2))
        val counted = calculator.add(fix(51.0 + 0.00004, accuracy = 5.0, seconds = 3)) as FixResult.Accepted

        assertEquals(4.45, counted.addedM, 0.1)
    }

    @Test
    fun AC32_a_clean_2_km_track_with_5_m_accuracy_is_within_2_percent() {
        val calculator = RouteDistanceCalculator()

        SyntheticTrack.north(fixes = 400, stepM = 5.0, accuracyM = 5.0, start = start).forEach { calculator.add(it) }

        assertEquals(2000.0, calculator.distanceM, 40.0)
    }

    @Test
    fun AC32_a_2_km_track_with_10_m_accuracy_and_small_noise_is_within_2_percent() {
        val calculator = RouteDistanceCalculator()

        SyntheticTrack
            .north(fixes = 400, stepM = 5.0, accuracyM = 10.0, start = start, noiseM = 0.5)
            .forEach { calculator.add(it) }

        assertEquals(2000.0, calculator.distanceM, 40.0)
    }

    @Test
    fun AC32_a_track_walked_at_10_m_steps_is_within_2_percent() {
        val calculator = RouteDistanceCalculator()

        SyntheticTrack.north(fixes = 200, stepM = 10.0, accuracyM = 8.0, start = start).forEach { calculator.add(it) }

        assertEquals(2000.0, calculator.distanceM, 40.0)
    }

    @Test
    fun a_stationary_phone_with_jitter_adds_nothing() {
        val calculator = RouteDistanceCalculator()
        val wobble = listOf(0.0, 0.00001, -0.00001, 0.000015, -0.000005, 0.00001)

        wobble.forEachIndexed { index, offset -> calculator.add(fix(51.0 + offset, accuracy = 8.0, seconds = index + 1L)) }

        assertEquals(0.0, calculator.distanceM, 0.0)
    }

    @Test
    fun the_distance_can_start_from_a_known_total() {
        val calculator = RouteDistanceCalculator(initialDistanceM = 100.0)

        assertEquals(100.0, calculator.distanceM, 0.0)
    }
}
