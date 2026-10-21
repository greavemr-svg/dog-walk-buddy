package io.github.greavemrsvg.dogwalkbuddy.domain.walk

import java.util.Locale

/** The one place a distance is turned into display text: miles to 1 decimal place (spec Assumption 7). */
object DistanceFormat {
    const val METRES_PER_MILE = 1609.344

    /** Miles to 1 decimal place, without the unit. A negative distance shows 0.0. */
    fun miles(metres: Double): String = String.format(Locale.UK, "%.1f", metres.coerceAtLeast(0.0) / METRES_PER_MILE)
}
