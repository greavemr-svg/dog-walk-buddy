package io.github.greavemrsvg.dogwalkbuddy.map

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import java.util.Locale

/**
 * How the walk route is drawn on the map (AC45): a light casing under a `primary` line, and a start marker at the
 * first fix. The map tiles are not themed, so the light `primary` is used in both themes (ADR 0007). Widths and the
 * radius are in dp, as MapLibre expects.
 */
object RouteStyle {
    /** The light theme `primary` (ADR 0007). */
    const val LINE_COLOUR = "#1B4F8F"
    const val LINE_WIDTH_DP = 5.5f
    const val CASING_COLOUR = "#FFFFFF"
    const val CASING_WIDTH_DP = 11f
    const val START_MARKER_FILL = LINE_COLOUR
    const val START_MARKER_STROKE = CASING_COLOUR
    const val START_MARKER_RADIUS_DP = 7f
    const val START_MARKER_STROKE_WIDTH_DP = 3f

    /** A GeoJSON point feature at the route's first fix, where the start marker is drawn. */
    fun startMarkerGeoJson(route: List<LatLon>): String {
        require(route.isNotEmpty()) { "A route needs at least one point" }
        val start = route.first()
        val coordinates = String.format(Locale.ROOT, "%.6f,%.6f", start.longitude, start.latitude)
        return """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[$coordinates]}}"""
    }
}
