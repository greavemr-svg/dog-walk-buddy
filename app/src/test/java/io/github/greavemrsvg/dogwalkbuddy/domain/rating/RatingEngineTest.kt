package io.github.greavemrsvg.dogwalkbuddy.domain.rating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RatingEngineTest {
    private val start = Instant.parse("2026-10-19T10:00:00Z")

    private fun hour(
        t: Double? = 15.0,
        p: Double? = 0.0,
        m: Double? = 10.0,
        cloud: Double? = 100.0,
    ) = HourlyConditions(start, t, p, m, cloud)

    private fun rate(
        hour: HourlyConditions,
        sensitivity: HeatSensitivity = HeatSensitivity.NORMAL,
        daylight: Boolean = false,
    ) = RatingEngine.rate(hour, sensitivity, daylight)

    private fun assertResult(
        expected: Pair<Rating, Reason>,
        actual: HourRating,
    ) {
        assertEquals(expected, actual.rating to actual.reason)
        assertEquals(start, actual.startUtc)
    }

    private val go = Rating.GO to Reason.GOOD

    @Test
    fun AC1_all_factors_go_gives_go_with_good_conditions() {
        assertResult(go, rate(hour(t = 15.0, p = 0.0, m = 10.0)))
    }

    @Test
    fun AC2_normal_heat_boundaries() {
        assertResult(go, rate(hour(t = 19.9)))
        assertResult(Rating.CAUTION to Reason.WARM, rate(hour(t = 20.0)))
        assertResult(Rating.CAUTION to Reason.WARM, rate(hour(t = 24.9)))
        assertResult(Rating.AVOID to Reason.TOO_HOT, rate(hour(t = 25.0)))
    }

    @Test
    fun AC3_sensitive_heat_boundaries() {
        val s = HeatSensitivity.SENSITIVE
        assertResult(go, rate(hour(t = 15.9), s))
        assertResult(Rating.CAUTION to Reason.WARM, rate(hour(t = 16.0), s))
        assertResult(Rating.CAUTION to Reason.WARM, rate(hour(t = 20.9), s))
        assertResult(Rating.AVOID to Reason.TOO_HOT, rate(hour(t = 21.0), s))
    }

    @Test
    fun AC4_normal_pavement_in_sunny_daylight() {
        assertResult(go, rate(hour(t = 19.9, cloud = 30.0), daylight = true))
        assertResult(Rating.CAUTION to Reason.PAVEMENT_WARM, rate(hour(t = 20.0, cloud = 30.0), daylight = true))
        assertResult(Rating.AVOID to Reason.PAVEMENT_TOO_HOT, rate(hour(t = 25.0, cloud = 30.0), daylight = true))
    }

    @Test
    fun AC5_sensitive_pavement_in_sunny_daylight() {
        val s = HeatSensitivity.SENSITIVE
        assertResult(Rating.CAUTION to Reason.PAVEMENT_WARM, rate(hour(t = 16.0, cloud = 30.0), s, true))
        assertResult(Rating.AVOID to Reason.PAVEMENT_TOO_HOT, rate(hour(t = 21.0, cloud = 30.0), s, true))
    }

    @Test
    fun AC6_pavement_does_not_apply_at_night_when_cloudy_or_when_cloud_is_missing() {
        val caution = Rating.CAUTION to Reason.WARM
        assertResult(caution, rate(hour(t = 22.0, cloud = 0.0), daylight = false))
        assertResult(caution, rate(hour(t = 22.0, cloud = 50.0), daylight = true))
        assertResult(caution, rate(hour(t = 22.0, cloud = null), daylight = true))
    }

    @Test
    fun AC7_rain_thresholds() {
        assertResult(go, rate(hour(p = 0.4)))
        assertResult(Rating.CAUTION to Reason.RAIN, rate(hour(p = 0.5)))
        assertResult(Rating.CAUTION to Reason.RAIN, rate(hour(p = 4.0)))
        assertResult(Rating.AVOID to Reason.HEAVY_RAIN, rate(hour(p = 4.1)))
    }

    @Test
    fun AC8_wind_thresholds_in_mph() {
        assertResult(go, rate(hour(m = 24.9)))
        assertResult(Rating.CAUTION to Reason.WINDY, rate(hour(m = 25.0)))
        assertResult(Rating.CAUTION to Reason.WINDY, rate(hour(m = 38.9)))
        assertResult(Rating.AVOID to Reason.DANGEROUS_WINDS, rate(hour(m = 39.0)))
    }

    @Test
    fun AC9_cold_gives_caution_and_never_avoid() {
        assertResult(go, rate(hour(t = 0.1)))
        assertResult(Rating.CAUTION to Reason.FREEZING, rate(hour(t = 0.0)))
        assertResult(Rating.CAUTION to Reason.FREEZING, rate(hour(t = -10.0)))
    }

    @Test
    fun AC10_sensitivity_does_not_change_rain_wind_or_cold() {
        val n = HeatSensitivity.NORMAL
        val s = HeatSensitivity.SENSITIVE
        for (p in listOf(0.4, 0.5, 4.0, 4.1)) {
            assertEquals(rate(hour(t = 10.0, p = p), n), rate(hour(t = 10.0, p = p), s))
        }
        for (m in listOf(24.9, 25.0, 38.9, 39.0)) {
            assertEquals(rate(hour(t = 10.0, m = m), n), rate(hour(t = 10.0, m = m), s))
        }
        for (t in listOf(0.1, 0.0, -10.0)) {
            assertEquals(rate(hour(t = t), n), rate(hour(t = t), s))
        }
        assertResult(Rating.AVOID to Reason.HEAVY_RAIN, rate(hour(t = 10.0, p = 4.1), s))
        assertResult(Rating.CAUTION to Reason.FREEZING, rate(hour(t = -10.0), s))
    }

    @Test
    fun AC11_worst_factor_wins_heavy_rain_over_warm() {
        assertResult(Rating.AVOID to Reason.HEAVY_RAIN, rate(hour(t = 22.0, p = 5.0)))
    }

    @Test
    fun AC12_tie_precedence_is_pavement_heat_wind_rain_cold() {
        val h = hour(t = 21.0, p = 1.0, m = 26.0, cloud = 20.0)
        assertResult(Rating.CAUTION to Reason.PAVEMENT_WARM, rate(h, daylight = true))
        // Without the pavement rule the next in line is heat, then wind, then rain, then cold.
        assertResult(Rating.CAUTION to Reason.WARM, rate(h, daylight = false))
        assertResult(Rating.CAUTION to Reason.WINDY, rate(hour(t = 15.0, p = 1.0, m = 26.0)))
        assertResult(Rating.CAUTION to Reason.RAIN, rate(hour(t = -1.0, p = 1.0)))
        assertResult(Rating.CAUTION to Reason.FREEZING, rate(hour(t = -1.0)))
    }

    @Test
    fun AC13_missing_wind_is_caution_with_data_missing_never_go() {
        assertResult(Rating.CAUTION to Reason.DATA_MISSING, rate(hour(t = 15.0, p = 0.0, m = null)))
    }

    @Test
    fun AC13_missing_temperature_or_precipitation_is_also_never_go() {
        assertResult(Rating.CAUTION to Reason.DATA_MISSING, rate(hour(t = null)))
        assertResult(Rating.CAUTION to Reason.DATA_MISSING, rate(hour(p = null)))
    }

    @Test
    fun AC13_missing_cloud_cover_is_not_missing_data() {
        assertResult(go, rate(hour(cloud = null), daylight = true))
    }

    @Test
    fun AC14_missing_data_does_not_hide_a_worse_rating() {
        assertResult(Rating.AVOID to Reason.HEAVY_RAIN, rate(hour(t = 15.0, p = 6.0, m = null)))
    }

    @Test
    fun AC15_all_core_data_missing_means_no_result() {
        assertTrue(RatingEngine.hasNoCoreData(hour(t = null, p = null, m = null)))
        assertFalse(RatingEngine.hasNoCoreData(hour(t = 15.0, p = null, m = null)))
        assertFalse(RatingEngine.hasNoCoreData(hour(t = null, p = 0.0, m = null)))
        assertFalse(RatingEngine.hasNoCoreData(hour(t = null, p = null, m = 5.0)))
    }
}
