package io.github.greavemrsvg.dogwalkbuddy.ui.common

import android.content.res.Resources
import androidx.annotation.PluralsRes
import io.github.greavemrsvg.dogwalkbuddy.R
import java.time.Duration

/** The units a spoken duration can use, largest first. */
enum class SpokenUnit(
    val seconds: Long,
    @param:PluralsRes val plural: Int,
) {
    HOURS(3600, R.plurals.duration_hours),
    MINUTES(60, R.plurals.duration_minutes),
    SECONDS(1, R.plurals.duration_seconds),
}

/**
 * A duration in words for screen readers (D6, AC34), for example "1 hour 2 minutes 5 seconds". Units whose value is
 * 0 are left out, except that a zero duration is "0 [smallest unit]". Anything below [smallest] is floored away, and
 * a negative duration (a clock set back) counts as zero.
 */
object SpokenDuration {
    /** The non-zero parts of [duration], largest unit first, down to [smallest]. */
    fun parts(
        duration: Duration,
        smallest: SpokenUnit = SpokenUnit.SECONDS,
    ): List<Pair<SpokenUnit, Long>> {
        val units = SpokenUnit.entries.filter { it.seconds >= smallest.seconds }
        var remaining = duration.seconds.coerceAtLeast(0)
        val parts =
            units.mapNotNull { unit ->
                val value = remaining / unit.seconds
                remaining %= unit.seconds
                if (value > 0) unit to value else null
            }
        return parts.ifEmpty { listOf(smallest to 0L) }
    }

    /** The spoken text of [duration], with each part from the app's plurals so 1 is singular. */
    fun format(
        resources: Resources,
        duration: Duration,
        smallest: SpokenUnit = SpokenUnit.SECONDS,
    ): String =
        parts(duration, smallest).joinToString(" ") { (unit, value) ->
            resources.getQuantityString(unit.plural, value.toInt(), value)
        }
}
