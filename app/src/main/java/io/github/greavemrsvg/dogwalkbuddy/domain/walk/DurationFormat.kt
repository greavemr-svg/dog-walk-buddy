package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import java.util.Locale

/** The one place a duration is turned into display text: `h:mm`, flooring total seconds to whole minutes. */
object DurationFormat {
    private const val SECONDS_PER_HOUR = 3600
    private const val SECONDS_PER_MINUTE = 60

    /** `h:mm` for [totalSeconds]. A negative duration (a clock set back) shows 0:00. */
    fun hoursMinutes(totalSeconds: Long): String {
        val total = totalSeconds.coerceAtLeast(0)
        return String.format(Locale.UK, "%d:%02d", total / SECONDS_PER_HOUR, total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE)
    }
}
