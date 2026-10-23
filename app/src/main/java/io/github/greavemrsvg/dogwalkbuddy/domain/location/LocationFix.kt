package io.github.greavemrsvg.dogwalkbuddy.domain.location

import java.time.Instant

/** One device location. [accuracyM] is null when the provider gives none. */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double?,
    val timeUtc: Instant,
)
