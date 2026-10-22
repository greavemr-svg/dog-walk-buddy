package io.github.greavemrsvg.dogwalkbuddy.domain.location

/** A position on the map: the part of a stored route point that the map needs. Personal data, never logged. */
data class LatLon(
    val latitude: Double,
    val longitude: Double,
)
