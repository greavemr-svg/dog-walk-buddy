package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import java.time.Instant

/** One saved walk as the history lists it. The distance is in metres. */
data class WalkSummary(
    val id: Long,
    val startUtc: Instant,
    val durationSeconds: Long,
    val distanceM: Double,
)
