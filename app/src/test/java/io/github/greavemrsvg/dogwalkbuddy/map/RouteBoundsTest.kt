package io.github.greavemrsvg.dogwalkbuddy.map

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fit-to-route bounds (AC38). Every coordinate here is synthetic. */
class RouteBoundsTest {
    private val delta = 1e-9

    @Test
    fun AC38_no_points_have_no_bounds() {
        assertNull(RouteBounds.of(emptyList()))
    }

    @Test
    fun AC38_the_bounds_hold_every_point_of_the_route() {
        val route =
            listOf(
                LatLon(51.50, -0.12),
                LatLon(51.52, -0.10),
                LatLon(51.51, -0.15),
                LatLon(51.49, -0.11),
            )

        val bounds = RouteBounds.of(route)!!

        assertEquals(51.49, bounds.south, delta)
        assertEquals(51.52, bounds.north, delta)
        assertEquals(-0.15, bounds.west, delta)
        assertEquals(-0.10, bounds.east, delta)
        route.forEach { point ->
            assertTrue(point.latitude in bounds.south..bounds.north)
            assertTrue(point.longitude in bounds.west..bounds.east)
        }
    }

    @Test
    fun AC38_a_route_that_is_large_enough_is_not_widened() {
        val bounds = RouteBounds.of(listOf(LatLon(51.500, -0.120), LatLon(51.510, -0.100)))!!

        assertEquals(51.500, bounds.south, delta)
        assertEquals(51.510, bounds.north, delta)
        assertEquals(-0.120, bounds.west, delta)
        assertEquals(-0.100, bounds.east, delta)
    }

    @Test
    fun AC38_a_single_point_is_widened_around_itself_by_the_minimum_span() {
        val bounds = RouteBounds.of(listOf(LatLon(51.5, -0.12)))!!

        assertEquals(RouteBounds.MIN_SPAN_DEGREES, bounds.north - bounds.south, delta)
        assertEquals(RouteBounds.MIN_SPAN_DEGREES, bounds.east - bounds.west, delta)
        assertEquals(51.5, (bounds.north + bounds.south) / 2, delta)
        assertEquals(-0.12, (bounds.east + bounds.west) / 2, delta)
    }

    @Test
    fun AC38_a_tiny_route_is_widened_in_each_direction_that_is_too_small() {
        val bounds = RouteBounds.of(listOf(LatLon(51.5, -0.120), LatLon(51.5005, -0.100)))!!

        // Latitude spans only 0.0005, so it is widened. Longitude spans 0.02, so it is kept.
        assertEquals(RouteBounds.MIN_SPAN_DEGREES, bounds.north - bounds.south, delta)
        assertEquals(-0.120, bounds.west, delta)
        assertEquals(-0.100, bounds.east, delta)
    }

    @Test
    fun AC38_widening_never_goes_past_the_valid_coordinate_range() {
        val bounds = RouteBounds.of(listOf(LatLon(90.0, 180.0)))!!

        assertEquals(90.0, bounds.north, delta)
        assertEquals(180.0, bounds.east, delta)
        assertTrue(bounds.south < bounds.north)
        assertTrue(bounds.west < bounds.east)
    }
}
