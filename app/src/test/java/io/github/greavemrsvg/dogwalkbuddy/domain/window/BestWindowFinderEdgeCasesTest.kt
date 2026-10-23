package io.github.greavemrsvg.dogwalkbuddy.domain.window

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourRating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BestWindowFinderEdgeCasesTest {
    private val midnight = Instant.parse("2026-10-19T00:00:00Z")

    private fun hours(pattern: String): List<HourRating> =
        pattern.mapIndexed { i, c ->
            val rating =
                when (c) {
                    'G' -> Rating.GO
                    'C' -> Rating.CAUTION
                    else -> Rating.AVOID
                }
            HourRating(midnight.plus(Duration.ofHours(10L + i)), rating, Reason.GOOD)
        }

    private fun at(hour: Int) = midnight.plus(Duration.ofHours(hour.toLong()))

    @Test
    fun AC16_a_short_go_run_beats_a_longer_caution_run() {
        val w = BestWindowFinder.find(hours("CCCCCCCGCCCC"))
        assertEquals(BestWindow.Window(at(17), at(18), Rating.GO, startsNow = false), w)
    }

    @Test
    fun AC16_all_twelve_hours_go_is_one_window_starting_now() {
        val w = BestWindowFinder.find(hours("GGGGGGGGGGGG"))
        assertEquals(BestWindow.Window(at(10), at(22), Rating.GO, startsNow = true), w)
    }

    @Test
    fun AC16_run_ending_at_the_last_hour_ends_at_the_end_of_that_hour() {
        val w = BestWindowFinder.find(hours("AAAAAAAAAGGG"))
        assertEquals(BestWindow.Window(at(19), at(22), Rating.GO, startsNow = false), w)
    }

    @Test
    fun AC16_longer_later_run_beats_an_earlier_shorter_run() {
        val w = BestWindowFinder.find(hours("GCGGGCCCCCCC"))
        assertEquals(BestWindow.Window(at(12), at(15), Rating.GO, startsNow = false), w)
    }

    @Test
    fun AC17_now_window_of_one_hour_ends_at_the_next_hour() {
        val w = BestWindowFinder.find(hours("GCCCCCCCCCCC"))
        assertEquals(BestWindow.Window(at(10), at(11), Rating.GO, startsNow = true), w)
    }

    @Test
    fun AC17_tie_between_now_run_and_later_run_goes_to_now() {
        val w = BestWindowFinder.find(hours("GGCCGGCCCCCC"))
        assertEquals(BestWindow.Window(at(10), at(12), Rating.GO, startsNow = true), w)
    }

    @Test
    fun AC18_caution_run_starting_now_is_flagged_as_now_with_caution_rating() {
        val w = BestWindowFinder.find(hours("CCAAAAAAAAAA"))
        assertEquals(BestWindow.Window(at(10), at(12), Rating.CAUTION, startsNow = true), w)
    }

    @Test
    fun AC18_earliest_caution_run_wins_a_tie() {
        val w = BestWindowFinder.find(hours("ACCAACCAAAAA"))
        assertEquals(BestWindow.Window(at(11), at(13), Rating.CAUTION, startsNow = false), w)
    }

    @Test
    fun AC19_single_non_avoid_hour_is_not_no_good_time() {
        val w = BestWindowFinder.find(hours("AAAAAAAAAAAC"))
        assertEquals(BestWindow.Window(at(21), at(22), Rating.CAUTION, startsNow = false), w)
    }
}
