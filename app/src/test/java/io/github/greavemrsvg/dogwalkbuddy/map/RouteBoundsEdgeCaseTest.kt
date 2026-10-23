package io.github.greavemrsvg.dogwalkbuddy.map

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Extra fit-to-route edge cases (AC38). Every coordinate here is synthetic. */
class RouteBoundsEdgeCaseTest {
    private val delta = 1e-9

    @Test
    fun AC38_many_identical_points_are_treated_like_a_single_point() {
        val same = LatLon(-33.5, -70.25)
        val bounds = RouteBounds.of(List(50) { same })!!

        assertEquals(RouteBounds.MIN_SPAN_DEGREES, bounds.north - bounds.south, delta)
        assertEquals(RouteBounds.MIN_SPAN_DEGREES, bounds.east - bounds.west, delta)
        assertEquals(-33.5, (bounds.north + bounds.south) / 2, delta)
        assertEquals(-70.25, (bounds.east + bounds.west) / 2, delta)
    }

    @Test
    fun AC38_widening_never_goes_below_the_valid_coordinate_range() {
        val bounds = RouteBounds.of(listOf(LatLon(-90.0, -180.0)))!!

        assertEquals(-90.0, bounds.south, delta)
        assertEquals(-180.0, bounds.west, delta)
        assertTrue(bounds.south < bounds.north)
        assertTrue(bounds.west < bounds.east)
    }

    @Test
    fun AC38_a_route_in_the_southern_and_western_hemispheres_is_bounded_correctly() {
        val bounds = RouteBounds.of(listOf(LatLon(-34.0, -58.5), LatLon(-34.2, -58.3), LatLon(-34.1, -58.4)))!!

        assertEquals(-34.2, bounds.south, delta)
        assertEquals(-34.0, bounds.north, delta)
        assertEquals(-58.5, bounds.west, delta)
        assertEquals(-58.3, bounds.east, delta)
    }

    @Test
    fun AC38_the_bounds_do_not_depend_on_the_order_of_the_points() {
        val route = listOf(LatLon(51.5, -0.12), LatLon(51.6, -0.05), LatLon(51.55, -0.2))

        assertEquals(RouteBounds.of(route), RouteBounds.of(route.reversed()))
    }

    @Test
    fun AC38_a_span_exactly_the_minimum_is_not_widened() {
        val bounds = RouteBounds.of(listOf(LatLon(10.0, 20.0), LatLon(10.0 + RouteBounds.MIN_SPAN_DEGREES, 20.5)))!!

        assertEquals(10.0, bounds.south, delta)
        assertEquals(10.0 + RouteBounds.MIN_SPAN_DEGREES, bounds.north, delta)
    }
}
