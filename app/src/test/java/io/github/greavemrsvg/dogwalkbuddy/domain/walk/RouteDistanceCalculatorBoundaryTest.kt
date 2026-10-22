package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.testutil.SyntheticTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/** Extra boundary cases for the jitter rule and the logging rule (ADR 0005). */
class RouteDistanceCalculatorBoundaryTest {
    private val start = Instant.parse("2026-10-19T09:00:00Z")

    private fun fix(
        lat: Double,
        accuracy: Double? = 5.0,
        seconds: Long = 0,
    ) = LocationFix(lat, -0.12, accuracy, start.plusSeconds(seconds))

    @Test
    fun AC32_a_2_km_track_at_the_best_allowed_10_m_accuracy_with_1_m_steps_is_within_2_percent() {
        val calculator = RouteDistanceCalculator()

        SyntheticTrack.north(fixes = 2000, stepM = 1.0, accuracyM = 10.0, start = start).forEach { calculator.add(it) }

        assertEquals(2000.0, calculator.distanceM, 40.0)
    }

    @Test
    fun AC32_a_2_km_track_at_walking_pace_steps_of_4_m_with_10_m_accuracy_is_within_2_percent() {
        val calculator = RouteDistanceCalculator()

        SyntheticTrack.north(fixes = 500, stepM = 4.0, accuracyM = 10.0, start = start, noiseM = 1.0).forEach { calculator.add(it) }

        assertEquals(2000.0, calculator.distanceM, 40.0)
    }

    @Test
    fun AC32_a_poor_fix_in_the_middle_of_a_track_does_not_change_the_distance() {
        val clean = RouteDistanceCalculator()
        val withBad = RouteDistanceCalculator()
        val track = SyntheticTrack.north(fixes = 100, stepM = 10.0, accuracyM = 8.0, start = start)

        track.forEach { clean.add(it) }
        track.forEachIndexed { i, f ->
            withBad.add(f)
            if (i == 50) withBad.add(f.copy(latitude = f.latitude + 0.01, accuracyM = 25.1, timeUtc = f.timeUtc.plusMillis(500)))
        }

        assertEquals(clean.distanceM, withBad.distanceM, 0.0)
    }

    @Test
    fun AC32_a_fix_with_zero_accuracy_is_accepted_and_the_total_never_goes_down() {
        val calculator = RouteDistanceCalculator()
        var previous = 0.0

        listOf(fix(51.0, 0.0, 1), fix(51.001, 0.0, 2), fix(51.0, 10.0, 3), fix(51.002, 10.0, 4)).forEach {
            calculator.add(it)
            assertTrue(calculator.distanceM >= previous)
            previous = calculator.distanceM
        }
        assertTrue(calculator.distanceM > 100.0)
    }

    @Test
    fun AC34_a_calculator_resumed_from_a_saved_total_adds_to_it_and_counts_no_distance_from_the_old_position() {
        val calculator = RouteDistanceCalculator(initialDistanceM = 500.0)

        val first = calculator.add(fix(51.0, seconds = 1))
        val second = calculator.add(fix(51.001, seconds = 2))

        assertEquals(RouteDistanceCalculator.haversineMetres(fix(51.0), fix(51.001)), (second as FixResult.Accepted).addedM, 0.001)
        assertEquals(500.0, (first as FixResult.Accepted).totalM, 0.0)
    }

    @Test
    fun route_points_are_never_logged_no_log_call_in_the_app_mentions_coordinates() {
        val offenders =
            File("src/main/java")
                .walkTopDown()
                .filter { it.extension == "kt" }
                .flatMap { file -> file.readLines().mapIndexed { i, line -> Triple(file.name, i + 1, line) } }
                .filter { (_, _, line) -> line.contains("Log.") || line.contains("println(") }
                .filter { (_, _, line) -> Regex("(?i)latitude|longitude|\blat\b|\blon\b|routePoint|fix\b").containsMatchIn(line) }
                .toList()

        assertEquals(emptyList<Triple<String, Int, String>>(), offenders)
    }
}
