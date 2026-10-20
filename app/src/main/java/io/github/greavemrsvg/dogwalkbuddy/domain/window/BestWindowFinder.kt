package io.github.greavemrsvg.dogwalkbuddy.domain.window

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import java.time.Duration

/** Finds the best walking window in a list of consecutive hourly ratings (spec, "Best window"). */
object BestWindowFinder {
    private val hour: Duration = Duration.ofHours(1)

    /**
     * [hours] are consecutive hourly periods, the first being the one that contains the current
     * time. The window is the longest run at the best rating, and the earliest run wins a tie.
     */
    fun find(hours: List<HourRating>): BestWindow {
        val best = hours.minOfOrNull { it.rating }
        if (best == null || best == Rating.AVOID) return BestWindow.NoGoodTime

        var bestStart = -1
        var bestLength = 0
        var index = 0
        while (index < hours.size) {
            if (hours[index].rating != best) {
                index++
                continue
            }
            val start = index
            while (index < hours.size && hours[index].rating == best) index++
            // Strictly longer only, so the earliest run wins a tie.
            if (index - start > bestLength) {
                bestStart = start
                bestLength = index - start
            }
        }
        return BestWindow.Window(
            startUtc = hours[bestStart].startUtc,
            endUtc = hours[bestStart + bestLength - 1].startUtc.plus(hour),
            rating = best,
            startsNow = bestStart == 0,
        )
    }
}
