package io.github.greavemrsvg.dogwalkbuddy.domain.check

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import java.time.Instant

/** The weather data a check is rated from, or the fact that there is none to rate. */
sealed interface CheckData {
    /**
     * Validated hourly conditions for the rounded location. [fetchedAt] is when MET data was last fetched, and
     * [isCached] is true when it comes from the local copy (so the screen shows its age, AC21).
     */
    data class Available(
        val hours: List<HourlyConditions>,
        val fetchedAt: Instant,
        val latitude: Double,
        val longitude: Double,
        val isCached: Boolean,
    ) : CheckData

    /** No fresh data and no cached copy under 3 h old (AC22). Nothing may be rated. */
    data object Unavailable : CheckData
}
