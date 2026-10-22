package io.github.greavemrsvg.dogwalkbuddy.domain.check

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Reason
import io.github.greavemrsvg.dogwalkbuddy.domain.window.BestWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class CheckRaterTest {
    private fun hour(
        start: Instant,
        temperatureC: Double? = 15.0,
        precipitation: Double? = 0.0,
        wind: Double? = 5.0,
        cloud: Double? = 100.0,
    ) = HourlyConditions(start, temperatureC, precipitation, wind, cloud)

    private fun hours(
        count: Int,
        from: Instant = FIRST,
        build: (Instant) -> HourlyConditions = { hour(it) },
    ) = (0 until count).map { build(from.plus(Duration.ofHours(it.toLong()))) }

    private fun data(
        hours: List<HourlyConditions>,
        isCached: Boolean = false,
        fetchedAt: Instant = FIRST,
    ) = CheckData.Available(hours, fetchedAt, LONDON_LAT, LONDON_LON, isCached)

    @Test
    fun AC17_the_12_hours_start_with_the_hour_that_contains_now_and_ignore_the_rest_of_the_48_kept() {
        val selected = CheckRater.upcomingHours(hours(48), FIRST.plus(Duration.ofMinutes(140)))

        assertEquals(CheckRater.WINDOW_HOURS, selected.size)
        assertEquals(FIRST.plus(Duration.ofHours(2)), selected.first().startUtc)
        assertEquals(FIRST.plus(Duration.ofHours(13)), selected.last().startUtc)
    }

    @Test
    fun AC17_a_period_starts_inclusively_and_ends_exclusively() {
        val all = hours(48)

        assertEquals(FIRST.plus(Duration.ofHours(1)), CheckRater.upcomingHours(all, FIRST.plus(Duration.ofHours(1))).first().startUtc)
        assertEquals(FIRST, CheckRater.upcomingHours(all, FIRST.plus(Duration.ofMinutes(59))).first().startUtc)
    }

    @Test
    fun AC15_when_no_period_contains_now_nothing_is_rated() {
        val all = hours(12)

        assertTrue(CheckRater.upcomingHours(all, FIRST.minusSeconds(1)).isEmpty())
        assertTrue(CheckRater.upcomingHours(all, FIRST.plus(Duration.ofHours(12))).isEmpty())
        assertEquals(CheckOutcome.CantCheck, CheckRater.rate(data(all), HeatSensitivity.NORMAL, FIRST.plus(Duration.ofHours(12))))
    }

    @Test
    fun AC17_fewer_than_12_remaining_periods_are_all_used() {
        val selected = CheckRater.upcomingHours(hours(14), FIRST.plus(Duration.ofHours(10)))

        assertEquals(4, selected.size)
    }

    @Test
    fun AC17_a_gap_in_the_hourly_periods_ends_the_run_so_hours_are_never_treated_as_consecutive() {
        val withGap = hours(3) + hours(10, from = FIRST.plus(Duration.ofHours(6)))

        val selected = CheckRater.upcomingHours(withGap, FIRST)

        assertEquals(3, selected.size)
    }

    @Test
    fun AC15_temperature_precipitation_and_wind_all_missing_is_cant_check_even_with_cloud_cover() {
        val all = hours(12) { hour(it, temperatureC = null, precipitation = null, wind = null, cloud = 10.0) }

        assertEquals(CheckOutcome.CantCheck, CheckRater.rate(data(all), HeatSensitivity.NORMAL, FIRST))
    }

    @Test
    fun AC13_one_missing_core_value_is_still_rated_but_never_go() {
        val all = hours(12) { hour(it, wind = null) }

        val outcome = CheckRater.rate(data(all), HeatSensitivity.NORMAL, FIRST) as CheckOutcome.Rated

        assertEquals(Rating.CAUTION, outcome.current.rating)
        assertEquals(Reason.DATA_MISSING, outcome.current.reason)
    }

    @Test
    fun AC1_the_current_hour_gives_the_headline_and_the_window_starts_now_when_it_is_the_best() {
        val outcome =
            CheckRater.rate(
                data(hours(12), isCached = true),
                HeatSensitivity.NORMAL,
                FIRST.plus(Duration.ofMinutes(5)),
            ) as CheckOutcome.Rated

        assertEquals(Rating.GO, outcome.current.rating)
        assertEquals(Reason.GOOD, outcome.current.reason)
        assertEquals(BestWindow.Window(FIRST, FIRST.plus(Duration.ofHours(12)), Rating.GO, startsNow = true), outcome.window)
        assertTrue(outcome.isCached)
        assertEquals(FIRST, outcome.fetchedAt)
    }

    @Test
    fun AC44_a_higher_sensitivity_re_rates_the_same_data_from_go_to_caution() {
        val all = hours(12) { hour(it, temperatureC = 17.0) }

        val normal = CheckRater.rate(data(all), HeatSensitivity.NORMAL, FIRST) as CheckOutcome.Rated
        val sensitive = CheckRater.rate(data(all), HeatSensitivity.SENSITIVE, FIRST) as CheckOutcome.Rated

        assertEquals(Rating.GO to Reason.GOOD, normal.current.rating to normal.current.reason)
        assertEquals(Rating.CAUTION to Reason.WARM, sensitive.current.rating to sensitive.current.reason)
    }

    @Test
    fun AC44_a_higher_sensitivity_re_rates_the_same_data_from_caution_to_avoid() {
        val all = hours(12) { hour(it, temperatureC = 22.0) }

        val normal = CheckRater.rate(data(all), HeatSensitivity.NORMAL, FIRST) as CheckOutcome.Rated
        val sensitive = CheckRater.rate(data(all), HeatSensitivity.SENSITIVE, FIRST) as CheckOutcome.Rated

        assertEquals(Rating.CAUTION to Reason.WARM, normal.current.rating to normal.current.reason)
        assertEquals(Rating.AVOID to Reason.TOO_HOT, sensitive.current.rating to sensitive.current.reason)
    }

    @Test
    fun AC4_daylight_at_the_checked_location_turns_on_the_pavement_rule() {
        val noon = Instant.parse("2026-10-19T12:00:00Z")
        val all = hours(12, from = noon) { hour(it, temperatureC = 22.0, cloud = 30.0) }

        val outcome = CheckRater.rate(data(all, fetchedAt = noon), HeatSensitivity.NORMAL, noon) as CheckOutcome.Rated

        assertEquals(Reason.PAVEMENT_WARM, outcome.current.reason)
    }

    @Test
    fun AC6_after_dark_the_same_hour_gives_the_heat_reason() {
        val night = Instant.parse("2026-10-19T22:00:00Z")
        val all = hours(12, from = night) { hour(it, temperatureC = 22.0, cloud = 0.0) }

        val outcome = CheckRater.rate(data(all, fetchedAt = night), HeatSensitivity.NORMAL, night) as CheckOutcome.Rated

        assertEquals(Reason.WARM, outcome.current.reason)
    }

    private companion object {
        val FIRST: Instant = Instant.parse("2026-10-19T14:00:00Z")
        const val LONDON_LAT = 51.51
        const val LONDON_LON = -0.13
    }
}
