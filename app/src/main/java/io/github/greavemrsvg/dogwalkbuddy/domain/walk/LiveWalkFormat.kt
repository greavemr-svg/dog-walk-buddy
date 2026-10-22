package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import java.time.Duration
import java.util.Locale

/** How the recording screen shows the walk so far. Distance uses the history's format. Pure Kotlin. */
object LiveWalkFormat {
    private const val SECONDS_PER_HOUR = 3600
    private const val SECONDS_PER_MINUTE = 60

    /** Elapsed time as `h:mm:ss`, flooring to whole seconds. A negative duration (a clock set back) shows 0:00:00. */
    fun elapsed(duration: Duration): String {
        val total = duration.seconds.coerceAtLeast(0)
        return String.format(
            Locale.UK,
            "%d:%02d:%02d",
            total / SECONDS_PER_HOUR,
            total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE,
            total % SECONDS_PER_MINUTE,
        )
    }

    /** Distance in miles to 1 decimal place (spec Assumption 7). */
    fun miles(metres: Double): String = DistanceFormat.miles(metres)
}
