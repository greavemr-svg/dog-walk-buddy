package io.github.greavemrsvg.dogwalkbuddy.domain.window

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BestWindowFinderTest {
    private val ten = Instant.parse("2026-10-19T10:00:00Z")

    private fun hours(pattern: String): List<HourRating> =
        pattern.mapIndexed { i, c ->
            val rating =
                when (c) {
                    'G' -> Rating.GO
                    'C' -> Rating.CAUTION
                    else -> Rating.AVOID
                }
            HourRating(ten.plus(Duration.ofHours(i.toLong())), rating, Reason.GOOD)
        }

    private fun at(hour: Int) = Instant.parse("2026-10-19T00:00:00Z").plus(Duration.ofHours(hour.toLong()))

    @Test
    fun AC16_longest_go_run_wins_and_earliest_run_wins_a_tie() {
        val window = BestWindowFinder.find(hours("CCGGGCGGAACC"))

        assertEquals(BestWindow.Window(at(12), at(15), Rating.GO, startsNow = false), window)
    }

    @Test
    fun AC16_earliest_of_two_equal_runs_wins() {
        val window = BestWindowFinder.find(hours("CGGCGGCCCCCC"))

        assertEquals(BestWindow.Window(at(11), at(13), Rating.GO, startsNow = false), window)
    }

    @Test
    fun AC17_window_starting_in_the_current_hour_is_flagged_as_now() {
        val window = BestWindowFinder.find(hours("GGCCCCCCAACC"))

        assertEquals(BestWindow.Window(at(10), at(12), Rating.GO, startsNow = true), window)
    }

    @Test
    fun AC18_best_available_is_the_longest_caution_run_when_no_go_hour_exists() {
        val window = BestWindowFinder.find(hours("AAAAACCAAAAA"))

        assertEquals(BestWindow.Window(at(15), at(17), Rating.CAUTION, startsNow = false), window)
    }

    @Test
    fun AC19_all_avoid_means_no_good_time() {
        assertEquals(BestWindow.NoGoodTime, BestWindowFinder.find(hours("AAAAAAAAAAAA")))
    }

    @Test
    fun an_empty_list_means_no_good_time() {
        assertEquals(BestWindow.NoGoodTime, BestWindowFinder.find(emptyList()))
    }

    @Test
    fun AC16_a_single_go_hour_at_the_end_is_a_window() {
        val window = BestWindowFinder.find(hours("CCCCCCCCCCCG"))

        assertEquals(BestWindow.Window(at(21), at(22), Rating.GO, startsNow = false), window)
    }
}
