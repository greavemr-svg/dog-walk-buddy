package io.github.greavemrsvg.dogwalkbuddy.domain.check

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import java.time.Instant

/** What the check screen shows: a rated result, or the fact that nothing honest can be rated. */
sealed interface CheckOutcome {
    /** [current] is the headline for the hour that contains now. [isCached] results are shown with their age. */
    data class Rated(
        val current: HourRating,
        val window: BestWindow,
        val fetchedAt: Instant,
        val isCached: Boolean,
    ) : CheckOutcome

    /** "Can't check right now": no usable data for the current hour (AC15, AC22). No rating is shown. */
    data object CantCheck : CheckOutcome
}
