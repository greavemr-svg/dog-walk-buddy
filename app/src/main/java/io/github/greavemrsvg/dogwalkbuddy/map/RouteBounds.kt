package io.github.greavemrsvg.dogwalkbuddy.map

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LatLon
import kotlin.math.max
import kotlin.math.min

/**
 * The box that holds a whole route, which the map is zoomed to fit (AC38). A route that is a single point, or very
 * small, is widened to [MIN_SPAN_DEGREES] around its centre so the map does not zoom in to street level on a dot.
 */
data class RouteBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    companion object {
        /** About 440 m of latitude. */
        const val MIN_SPAN_DEGREES = 0.004

        private const val MAX_LATITUDE = 90.0
        private const val MAX_LONGITUDE = 180.0

        /** The bounds of [points], or null when there are none. */
        fun of(points: List<LatLon>): RouteBounds? {
            if (points.isEmpty()) return null
            var south = points.first().latitude
            var north = south
            var west = points.first().longitude
            var east = west
            for (point in points) {
                south = min(south, point.latitude)
                north = max(north, point.latitude)
                west = min(west, point.longitude)
                east = max(east, point.longitude)
            }
            val (paddedSouth, paddedNorth) = widen(south, north, MAX_LATITUDE)
            val (paddedWest, paddedEast) = widen(west, east, MAX_LONGITUDE)
            return RouteBounds(paddedSouth, paddedWest, paddedNorth, paddedEast)
        }

        private fun widen(
            low: Double,
            high: Double,
            limit: Double,
        ): Pair<Double, Double> {
            if (high - low >= MIN_SPAN_DEGREES) return low to high
            val centre = (low + high) / 2
            val half = MIN_SPAN_DEGREES / 2
            return (centre - half).coerceAtLeast(-limit) to (centre + half).coerceAtMost(limit)
        }
    }
}
