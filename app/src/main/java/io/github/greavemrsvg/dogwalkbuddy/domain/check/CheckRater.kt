package io.github.greavemrsvg.dogwalkbuddy.domain.check

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.RatingEngine
import io.github.greavemrsvg.dogwalkbuddy.domain.sun.SunCalculator
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindowFinder
import java.time.Duration
import java.time.Instant

/**
 * Turns validated hourly data into what the screen shows. It is pure, so a change of heat sensitivity re-rates
 * the data already in memory without a network request (AC44).
 */
object CheckRater {
    /** The best window looks at this many hourly periods, starting with the one that contains now. */
    const val WINDOW_HOURS = 12

    private val hour: Duration = Duration.ofHours(1)

    /**
     * The up to [WINDOW_HOURS] consecutive hourly periods starting with the one that contains [now]. The list
     * is empty when no period contains [now], so stale data can never be rated as if it were current.
     */
    fun upcomingHours(
        hours: List<HourlyConditions>,
        now: Instant,
    ): List<HourlyConditions> {
        val first = hours.indexOfFirst { !now.isBefore(it.startUtc) && now.isBefore(it.startUtc.plus(hour)) }
        if (first < 0) return emptyList()
        val selected = mutableListOf(hours[first])
        for (index in first + 1 until hours.size) {
            // The best window needs consecutive hours, so a gap ends the run of usable periods.
            if (selected.size == WINDOW_HOURS || hours[index].startUtc != selected.last().startUtc.plus(hour)) break
            selected.add(hours[index])
        }
        return selected
    }

    fun rate(
        data: CheckData.Available,
        sensitivity: HeatSensitivity,
        now: Instant,
    ): CheckOutcome {
        val upcoming = upcomingHours(data.hours, now)
        val current = upcoming.firstOrNull()
        if (current == null || RatingEngine.hasNoCoreData(current)) return CheckOutcome.CantCheck
        val ratings =
            upcoming.map { conditions ->
                val isDaylight = SunCalculator.isDaylight(conditions.startUtc, data.latitude, data.longitude)
                RatingEngine.rate(conditions, sensitivity, isDaylight)
            }
        return CheckOutcome.Rated(
            current = ratings.first(),
            window = BestWindowFinder.find(ratings),
            fetchedAt = data.fetchedAt,
            isCached = data.isCached,
        )
    }
}
