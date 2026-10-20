package io.github.greavemrsvg.dogwalkbuddy.domain.window

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import java.time.Instant

sealed interface BestWindow {
    /** A run of consecutive hours at the best rating. [startsNow] is true when it begins with the current hour. */
    data class Window(
        val startUtc: Instant,
        val endUtc: Instant,
        val rating: Rating,
        val startsNow: Boolean,
    ) : BestWindow

    /** Every hour is Avoid. */
    data object NoGoodTime : BestWindow
}
