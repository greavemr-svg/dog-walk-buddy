package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import java.time.Duration

/** How the recording screen shows the walk so far. It uses the same formats as the history. Pure Kotlin. */
object LiveWalkFormat {
    /** Elapsed time as `h:mm`, flooring to whole minutes. A negative duration (a clock set back) shows 0:00. */
    fun elapsed(duration: Duration): String = DurationFormat.hoursMinutes(duration.seconds)

    /** Distance in miles to 1 decimal place (spec Assumption 7). */
    fun miles(metres: Double): String = DistanceFormat.miles(metres)
}
