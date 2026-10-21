package io.github.greavemrsvg.dogwalkbuddy.map

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import java.util.Locale

/**
 * Builds the GeoJSON line of a route on the device, so the route is never sent anywhere (ADR 0003). A single
 * point is repeated, because a line needs two positions.
 */
object RouteGeoJson {
    fun lineString(route: List<LatLon>): String {
        require(route.isNotEmpty()) { "A route needs at least one point" }
        val points = if (route.size == 1) listOf(route.first(), route.first()) else route
        val coordinates = points.joinToString(",") { "[${decimal(it.longitude)},${decimal(it.latitude)}]" }
        return """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coordinates]}}"""
    }

    // GeoJSON positions are longitude then latitude. Locale.ROOT keeps the decimal separator a full stop.
    private fun decimal(value: Double): String = String.format(Locale.ROOT, "%.6f", value)
}
