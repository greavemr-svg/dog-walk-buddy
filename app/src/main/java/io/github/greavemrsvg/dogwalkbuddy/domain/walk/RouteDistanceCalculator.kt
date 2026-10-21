package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** What the calculator did with one fix. [addedM] is 0 for an accepted fix that was too close to count. */
sealed interface FixResult {
    data object Rejected : FixResult

    data class Accepted(
        val addedM: Double,
        val totalM: Double,
    ) : FixResult
}

/**
 * Filters GPS fixes and measures the walked distance (ADR 0005). Pure Kotlin, no Android types.
 *
 * - A fix with no accuracy, with accuracy over [MAX_ACCURACY_M], or with a time that is not after the previous
 *   accepted fix is rejected. It is neither stored nor counted.
 * - An accepted fix adds the haversine distance from the last counted fix, but only when that distance is at least
 *   `max(MIN_SEGMENT_M, 0.5 x combined accuracy)`. The combined accuracy is the root of the sum of the two squared
 *   accuracies. Closer fixes are accepted (they are part of the route) but add nothing, and the reference stays on
 *   the last counted fix, so slow walking still adds up and stationary jitter adds nothing.
 */
class RouteDistanceCalculator(
    initialDistanceM: Double = 0.0,
) {
    private var reference: LocationFix? = null
    private var lastAcceptedTime: Instant? = null

    var distanceM: Double = initialDistanceM
        private set

    fun add(fix: LocationFix): FixResult {
        val accuracy = fix.accuracyM
        if (accuracy == null || accuracy > MAX_ACCURACY_M) return FixResult.Rejected
        val previousTime = lastAcceptedTime
        if (previousTime != null && !fix.timeUtc.isAfter(previousTime)) return FixResult.Rejected
        lastAcceptedTime = fix.timeUtc

        val from = reference
        if (from == null) {
            reference = fix
            return FixResult.Accepted(addedM = 0.0, totalM = distanceM)
        }
        val segmentM = haversineMetres(from, fix)
        val combinedAccuracy = hypot(from.accuracyM ?: 0.0, accuracy)
        if (segmentM < max(MIN_SEGMENT_M, COMBINED_ACCURACY_FACTOR * combinedAccuracy)) {
            return FixResult.Accepted(addedM = 0.0, totalM = distanceM)
        }
        reference = fix
        distanceM += segmentM
        return FixResult.Accepted(addedM = segmentM, totalM = distanceM)
    }

    companion object {
        const val MAX_ACCURACY_M = 25.0
        const val MIN_SEGMENT_M = 3.0
        const val COMBINED_ACCURACY_FACTOR = 0.5
        const val EARTH_RADIUS_M = 6_371_008.8

        /** Great-circle distance in metres between two fixes. */
        fun haversineMetres(
            a: LocationFix,
            b: LocationFix,
        ): Double {
            val lat1 = Math.toRadians(a.latitude)
            val lat2 = Math.toRadians(b.latitude)
            val dLat = lat2 - lat1
            val dLon = Math.toRadians(b.longitude - a.longitude)
            val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
            return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
        }
    }
}
