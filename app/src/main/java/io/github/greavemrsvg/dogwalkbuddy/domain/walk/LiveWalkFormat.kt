package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import java.time.Duration
import java.util.Locale

/** How the recording screen shows the walk so far. Pure Kotlin. */
object LiveWalkFormat {
    private const val METRES_PER_MILE = 1609.344

    /** Elapsed time as `h:mm:ss`, flooring to whole seconds. A negative duration (a clock set back) shows 0:00:00. */
    fun elapsed(duration: Duration): String {
        val total = duration.seconds.coerceAtLeast(0)
        return String.format(Locale.UK, "%d:%02d:%02d", total / 3600, total % 3600 / 60, total % 60)
    }

    /**
     * Distance in miles with 2 decimals, so it visibly changes during a short walk. The history shows 1 decimal
     * (design, Walk flow).
     */
    fun miles(metres: Double): String = String.format(Locale.UK, "%.2f", metres.coerceAtLeast(0.0) / METRES_PER_MILE)
}
