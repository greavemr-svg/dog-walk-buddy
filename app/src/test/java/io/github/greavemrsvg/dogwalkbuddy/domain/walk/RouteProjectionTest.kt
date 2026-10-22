package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos

class RouteProjectionTest {
    private val a = LatLon(51.5000, -0.1300)
    private val b = LatLon(51.5010, -0.1290)
    private val c = LatLon(51.5020, -0.1270)

    private fun assertPoint(
        x: Float,
        y: Float,
        actual: PanelPoint,
    ) {
        assertEquals("x", x, actual.x, TOLERANCE)
        assertEquals("y", y, actual.y, TOLERANCE)
    }

    @Test
    fun UX_AC35_the_route_is_scaled_to_fit_inside_the_padding_with_the_start_first_and_the_current_fix_last() {
        val points = RouteProjection.fit(listOf(a, b, c), WIDTH, HEIGHT, PADDING)

        assertEquals(3, points.size)
        points.forEach { point ->
            assertTrue("$point is inside the padding", point.x in PADDING - TOLERANCE..WIDTH - PADDING + TOLERANCE)
            assertTrue("$point is inside the padding", point.y in PADDING - TOLERANCE..HEIGHT - PADDING + TOLERANCE)
        }
        // The route is taller than the panel allows, so it fills the usable height and is centred across.
        assertEquals(HEIGHT - PADDING, points.first().y, TOLERANCE)
        assertEquals(PADDING, points.last().y, TOLERANCE)
        assertEquals(WIDTH - points.last().x, points.first().x, TOLERANCE)
    }

    @Test
    fun UX_AC35_north_is_up_and_east_is_right() {
        val points = RouteProjection.fit(listOf(a, c), WIDTH, HEIGHT, PADDING)

        assertTrue("the later fix is further east", points[1].x > points[0].x)
        assertTrue("the later fix is further north", points[1].y < points[0].y)
    }

    @Test
    fun UX_AC35_longitude_is_scaled_by_the_cosine_of_the_mean_latitude_so_the_shape_is_kept() {
        val east = LatLon(60.0, 0.002)
        val north = LatLon(60.001, 0.0)
        val origin = LatLon(60.0, 0.0)

        val points = RouteProjection.fit(listOf(origin, east, north), 1000f, 1000f, 0f)

        val eastward = points[1].x - points[0].x
        val northward = points[0].y - points[2].y
        val expected = 0.002 * cos(Math.toRadians((60.0 + 60.0 + 60.001) / 3)) / 0.001
        assertEquals(expected, (eastward / northward).toDouble(), 1e-3)
    }

    @Test
    fun UX_AC35_a_route_along_one_axis_is_centred_across_the_other() {
        val points = RouteProjection.fit(listOf(LatLon(51.5, -0.13), LatLon(51.5, -0.12)), WIDTH, HEIGHT, PADDING)

        assertPoint(PADDING, HEIGHT / 2, points[0])
        assertPoint(WIDTH - PADDING, HEIGHT / 2, points[1])
    }

    @Test
    fun UX_AC35_a_route_that_has_not_moved_is_drawn_at_the_centre() {
        val points = RouteProjection.fit(listOf(a, a), WIDTH, HEIGHT, PADDING)

        points.forEach { assertPoint(WIDTH / 2, HEIGHT / 2, it) }
    }

    @Test
    fun UX_AC35_no_fixes_give_no_points() {
        assertTrue(RouteProjection.fit(emptyList(), WIDTH, HEIGHT, PADDING).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun a_negative_padding_is_rejected() {
        RouteProjection.fit(listOf(a, b), WIDTH, HEIGHT, -1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun a_negative_panel_size_is_rejected() {
        RouteProjection.fit(listOf(a, b), -1f, HEIGHT, PADDING)
    }

    private companion object {
        const val WIDTH = 900f
        const val HEIGHT = 600f
        const val PADDING = 60f
        const val TOLERANCE = 0.01f
    }
}
