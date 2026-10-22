package io.github.greavemrsvg.dogwalkbuddy.map

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** The route line is built on the device (ADR 0003). Coordinates are synthetic. */
class RouteGeoJsonTest {
    private fun line(coordinates: String) =
        """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coordinates]}}"""

    @Test
    fun AC38_the_line_lists_positions_as_longitude_then_latitude_in_order() {
        val json = RouteGeoJson.lineString(listOf(LatLon(51.5, -0.12), LatLon(51.6, -0.1)))

        assertEquals(line("[-0.120000,51.500000],[-0.100000,51.600000]"), json)
    }

    @Test
    fun AC38_a_single_point_is_repeated_because_a_line_needs_two_positions() {
        val json = RouteGeoJson.lineString(listOf(LatLon(51.5, -0.12)))

        assertEquals(line("[-0.120000,51.500000],[-0.120000,51.500000]"), json)
    }

    @Test
    fun an_empty_route_is_rejected() {
        assertThrows(IllegalArgumentException::class.java) { RouteGeoJson.lineString(emptyList()) }
    }

    @Test
    fun the_decimal_separator_is_a_full_stop_whatever_the_device_language() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)

            val json = RouteGeoJson.lineString(listOf(LatLon(51.5, -0.12)))

            assertTrue(json.contains("[-0.120000,51.500000]"))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
