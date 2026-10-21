package io.github.greavemrsvg.dogwalkbuddy.data.location

import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import kotlinx.coroutines.flow.Flow

/**
 * Precise device locations while a walk is recorded (ADR 0005). Collecting the flow starts the updates and
 * cancelling the collection removes them. Tests inject fakes.
 */
fun interface WalkLocationSource {
    fun fixes(): Flow<LocationFix>
}
