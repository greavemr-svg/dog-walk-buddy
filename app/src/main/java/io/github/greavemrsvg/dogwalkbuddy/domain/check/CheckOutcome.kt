package io.github.greavemrsvg.dogwalkbuddy.domain.check

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.PavementFactor
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import java.time.Instant

/** What the check screen shows: a rated result, or the fact that nothing honest can be rated. */
sealed interface CheckOutcome {
    /**
     * [current] is the headline for the hour that contains now. [isCached] results are shown with their age.
     * [hours] are the up to 12 rated hours from the current one, [conditions] the current hour's values and
     * [pavement] its pavement factor, for the factor grid (AC17).
     */
    data class Rated(
        val current: HourRating,
        val window: BestWindow,
        val fetchedAt: Instant,
        val isCached: Boolean,
        val hours: List<HourRating>,
        val conditions: HourlyConditions,
        val pavement: PavementFactor,
    ) : CheckOutcome

    /** "Can't check right now": no usable data for the current hour (AC15, AC22). No rating is shown. */
    data object CantCheck : CheckOutcome
}
