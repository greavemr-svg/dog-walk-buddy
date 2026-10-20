package io.github.greavemrsvg.dogwalkbuddy.data.location

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix

/** Approximate (coarse) device location for the weather check. Tests inject fakes (ADR 0005). */
fun interface ApproximateLocationSource {
    /** The current approximate location, or null when permission is missing, no provider answers or it times out. */
    suspend fun current(): LocationFix?
}
