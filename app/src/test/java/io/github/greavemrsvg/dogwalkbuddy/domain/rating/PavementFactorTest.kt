package io.github.greavemrsvg.dogwalkbuddy.domain.rating

import io.github.greavemrsvg.dogwalkbuddy.domain.check.CheckData
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CheckOutcome
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CheckRater
import io.github.greavemrsvg.dogwalkbuddy.domain.check.Factor
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorCell
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorGrid
import io.github.greavemrsvg.dogwalkbuddy.domain.check.FactorValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** The pavement factor, the driver mapping and the factor cells of the Today grid (AC17). */
class PavementFactorTest {
    private fun hour(
        temperatureC: Double? = 15.0,
        cloud: Double? = 20.0,
        rain: Double? = 0.0,
        wind: Double? = 5.0,
    ) = HourlyConditions(NOON, temperatureC, rain, wind, cloud)

    @Test
    fun UX_AC17_pavement_is_not_rated_at_night_in_cloud_of_50_percent_or_more_or_without_cloud_data() {
        assertEquals(PavementFactor.NotRated, RatingEngine.pavementFactor(hour(), HeatSensitivity.NORMAL, isDaylight = false))
        assertEquals(PavementFactor.NotRated, RatingEngine.pavementFactor(hour(cloud = 50.0), HeatSensitivity.NORMAL, true))
        assertEquals(PavementFactor.NotRated, RatingEngine.pavementFactor(hour(cloud = null), HeatSensitivity.NORMAL, true))
        assertEquals(PavementFactor.Rated(Rating.GO), RatingEngine.pavementFactor(hour(cloud = 49.9), HeatSensitivity.NORMAL, true))
    }

    @Test
    fun UX_AC17_pavement_has_no_data_when_the_rule_applies_but_the_temperature_is_missing() {
        assertEquals(PavementFactor.NoData, RatingEngine.pavementFactor(hour(temperatureC = null), HeatSensitivity.NORMAL, true))
    }

    @Test
    fun UX_AC17_pavement_uses_the_heat_thresholds_with_the_sensitivity_offset() {
        fun rate(
            t: Double,
            s: HeatSensitivity,
        ) = RatingEngine.pavementFactor(hour(temperatureC = t), s, true)
        assertEquals(PavementFactor.Rated(Rating.GO), rate(19.9, HeatSensitivity.NORMAL))
        assertEquals(PavementFactor.Rated(Rating.CAUTION), rate(20.0, HeatSensitivity.NORMAL))
        assertEquals(PavementFactor.Rated(Rating.AVOID), rate(25.0, HeatSensitivity.NORMAL))
        assertEquals(PavementFactor.Rated(Rating.GO), rate(15.9, HeatSensitivity.SENSITIVE))
        assertEquals(PavementFactor.Rated(Rating.CAUTION), rate(16.0, HeatSensitivity.SENSITIVE))
        assertEquals(PavementFactor.Rated(Rating.AVOID), rate(21.0, HeatSensitivity.SENSITIVE))
    }

    @Test
    fun UX_AC17_each_reason_highlights_the_factor_it_names_and_good_or_missing_data_highlights_none() {
        val expected =
            mapOf(
                Reason.WARM to Factor.AIR,
                Reason.TOO_HOT to Factor.AIR,
                Reason.FREEZING to Factor.AIR,
                Reason.RAIN to Factor.RAIN,
                Reason.HEAVY_RAIN to Factor.RAIN,
                Reason.WINDY to Factor.WIND,
                Reason.DANGEROUS_WINDS to Factor.WIND,
                Reason.PAVEMENT_WARM to Factor.PAVEMENT,
                Reason.PAVEMENT_TOO_HOT to Factor.PAVEMENT,
            )
        expected.forEach { (reason, factor) -> assertEquals(reason.name, factor, FactorGrid.driverOf(reason)) }
        assertNull(FactorGrid.driverOf(Reason.GOOD))
        assertNull(FactorGrid.driverOf(Reason.DATA_MISSING))
        assertEquals(Reason.entries.toSet(), expected.keys + Reason.GOOD + Reason.DATA_MISSING)
    }

    @Test
    fun UX_AC17_the_spec_example_in_daylight_shows_warm_pavement_highlighted() {
        // Normal, daylight, cloud 20 %, T = 21 °C, P = 1 mm/h, M = 26 mph (MVP AC12 inputs).
        val conditions = hour(temperatureC = 21.0, cloud = 20.0, rain = 1.0, wind = 26.0)
        val rating = RatingEngine.rate(conditions, HeatSensitivity.NORMAL, isDaylight = true)
        val pavement = RatingEngine.pavementFactor(conditions, HeatSensitivity.NORMAL, isDaylight = true)

        assertEquals(
            listOf(
                FactorCell(Factor.AIR, FactorValue.Whole(21), highlighted = false),
                FactorCell(Factor.RAIN, FactorValue.Whole(1), highlighted = false),
                FactorCell(Factor.WIND, FactorValue.Whole(26), highlighted = false),
                FactorCell(Factor.PAVEMENT, FactorValue.Pavement(Rating.CAUTION), highlighted = true),
            ),
            FactorGrid.cells(conditions, pavement, rating.reason),
        )
    }

    @Test
    fun UX_AC17_the_negative_example_at_night_shows_minus_3_and_highlights_air() {
        val conditions = hour(temperatureC = -2.5, cloud = 20.0, rain = 0.0, wind = 5.0)
        val rating = RatingEngine.rate(conditions, HeatSensitivity.NORMAL, isDaylight = false)
        val pavement = RatingEngine.pavementFactor(conditions, HeatSensitivity.NORMAL, isDaylight = false)

        assertEquals(
            listOf(
                FactorCell(Factor.AIR, FactorValue.Whole(-3), highlighted = true),
                FactorCell(Factor.RAIN, FactorValue.Whole(0), highlighted = false),
                FactorCell(Factor.WIND, FactorValue.Whole(5), highlighted = false),
                FactorCell(Factor.PAVEMENT, FactorValue.NotRated, highlighted = false),
            ),
            FactorGrid.cells(conditions, pavement, rating.reason),
        )
        val nearZero = FactorGrid.cells(hour(temperatureC = -0.4), PavementFactor.NotRated, Reason.FREEZING)
        assertEquals(FactorValue.Whole(0), nearZero.first().value)
    }

    @Test
    fun UX_AC17_a_missing_value_shows_no_data_and_no_cell_is_highlighted_for_missing_data() {
        val conditions = HourlyConditions(NOON, null, 0.0, null, null)

        val cells = FactorGrid.cells(conditions, PavementFactor.NoData, Reason.DATA_MISSING)

        assertEquals(listOf(FactorValue.NoData, FactorValue.Whole(0), FactorValue.NoData, FactorValue.NoData), cells.map { it.value })
        assertEquals(listOf(false, false, false, false), cells.map { it.highlighted })
    }

    @Test
    fun UX_AC17_the_rater_gives_the_current_hours_conditions_pavement_and_rated_hours() {
        // London, 12:00 UTC on 19 October is daylight; 23:00 UTC is night.
        val hours = (0 until 12).map { HourlyConditions(NOON.plus(Duration.ofHours(it.toLong())), 21.0, 0.0, 5.0, 20.0) }
        val data = CheckData.Available(hours, NOON, 51.51, -0.13, isCached = false)

        val day = CheckRater.rate(data, HeatSensitivity.NORMAL, NOON.plusSeconds(60)) as CheckOutcome.Rated
        assertEquals(hours.first(), day.conditions)
        assertEquals(PavementFactor.Rated(Rating.CAUTION), day.pavement)
        assertEquals(12, day.hours.size)
        assertEquals(day.current, day.hours.first())

        val night = CheckRater.rate(data, HeatSensitivity.NORMAL, NOON.plus(Duration.ofHours(11))) as CheckOutcome.Rated
        assertEquals(PavementFactor.NotRated, night.pavement)
        assertEquals(1, night.hours.size)
    }

    private companion object {
        val NOON: Instant = Instant.parse("2026-10-19T12:00:00Z")
    }
}
