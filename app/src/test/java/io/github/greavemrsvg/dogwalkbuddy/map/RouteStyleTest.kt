package io.github.greavemrsvg.dogwalkbuddy.map

import androidx.compose.ui.graphics.toArgb
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import io.github.greavemrsvg.dogwalkbuddy.ui.theme.LightFieldKitColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The route style of the walk map (AC45). */
class RouteStyleTest {
    private fun hex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    @Test
    fun UX_AC45_the_route_line_is_the_light_primary_in_both_themes() {
        assertEquals(hex(LightFieldKitColors.primary.toArgb()), RouteStyle.LINE_COLOUR)
        assertEquals("#1B4F8F", RouteStyle.LINE_COLOUR)
        assertEquals(5.5f, RouteStyle.LINE_WIDTH_DP)
    }

    @Test
    fun UX_AC45_the_casing_is_white_and_wider_than_the_line() {
        assertEquals("#FFFFFF", RouteStyle.CASING_COLOUR)
        assertEquals(11f, RouteStyle.CASING_WIDTH_DP)
        assertTrue(RouteStyle.CASING_WIDTH_DP > RouteStyle.LINE_WIDTH_DP)
    }

    @Test
    fun UX_AC45_the_start_marker_is_primary_with_a_white_stroke() {
        assertEquals("#1B4F8F", RouteStyle.START_MARKER_FILL)
        assertEquals("#FFFFFF", RouteStyle.START_MARKER_STROKE)
        assertTrue(RouteStyle.START_MARKER_RADIUS_DP > 0f)
        assertTrue(RouteStyle.START_MARKER_STROKE_WIDTH_DP > 0f)
    }

    @Test
    fun UX_AC45_the_start_marker_is_at_the_first_fix() {
        val json = RouteStyle.startMarkerGeoJson(listOf(LatLon(51.5, -0.12), LatLon(51.51, -0.1)))

        assertEquals(
            """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[-0.120000,51.500000]}}""",
            json,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun an_empty_route_has_no_start_marker() {
        RouteStyle.startMarkerGeoJson(emptyList())
    }
}
