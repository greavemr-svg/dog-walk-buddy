package io.github.greavemrsvg.dogwalkbuddy.domain.rating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RatingEngineEdgeCasesTest {
    private val start = Instant.parse("2026-10-19T10:00:00Z")

    private fun hour(
        t: Double? = 15.0,
        p: Double? = 0.0,
        m: Double? = 10.0,
        cloud: Double? = 100.0,
    ) = HourlyConditions(start, t, p, m, cloud)

    private fun rate(
        h: HourlyConditions,
        s: HeatSensitivity = HeatSensitivity.NORMAL,
        daylight: Boolean = false,
    ) = RatingEngine.rate(h, s, daylight).let { it.rating to it.reason }

    @Test
    fun AC4_cloud_cover_boundary_49_9_is_sunny_and_50_is_not() {
        assertEquals(Rating.CAUTION to Reason.PAVEMENT_WARM, rate(hour(t = 22.0, cloud = 49.9), daylight = true))
        assertEquals(Rating.CAUTION to Reason.WARM, rate(hour(t = 22.0, cloud = 50.0), daylight = true))
        assertEquals(Rating.AVOID to Reason.PAVEMENT_TOO_HOT, rate(hour(t = 25.0, cloud = 0.0), daylight = true))
    }

    @Test
    fun AC4_pavement_never_applies_outside_daylight_even_when_sunny_and_hot() {
        assertEquals(Rating.AVOID to Reason.TOO_HOT, rate(hour(t = 25.0, cloud = 0.0), daylight = false))
        assertEquals(Rating.CAUTION to Reason.WARM, rate(hour(t = 20.0, cloud = 0.0), daylight = false))
    }

    @Test
    fun AC5_sensitive_pavement_just_below_thresholds_is_go_and_caution() {
        val s = HeatSensitivity.SENSITIVE
        assertEquals(Rating.GO to Reason.GOOD, rate(hour(t = 15.9, cloud = 30.0), s, true))
        assertEquals(Rating.CAUTION to Reason.PAVEMENT_WARM, rate(hour(t = 20.9, cloud = 30.0), s, true))
    }

    @Test
    fun AC6_sensitive_at_night_or_without_cloud_data_uses_heat_reason_not_pavement() {
        val s = HeatSensitivity.SENSITIVE
        assertEquals(Rating.AVOID to Reason.TOO_HOT, rate(hour(t = 21.0, cloud = 0.0), s, false))
        assertEquals(Rating.CAUTION to Reason.WARM, rate(hour(t = 18.0, cloud = null), s, true))
    }

    @Test
    fun AC11_avoid_from_rain_beats_pavement_caution_in_daylight() {
        assertEquals(Rating.AVOID to Reason.HEAVY_RAIN, rate(hour(t = 22.0, p = 5.0, cloud = 10.0), daylight = true))
    }

    @Test
    fun AC12_avoid_ties_prefer_pavement_then_heat_then_wind_then_rain() {
        val hotSunny = hour(t = 26.0, p = 5.0, m = 40.0, cloud = 10.0)
        assertEquals(Rating.AVOID to Reason.PAVEMENT_TOO_HOT, rate(hotSunny, daylight = true))
        assertEquals(Rating.AVOID to Reason.TOO_HOT, rate(hotSunny, daylight = false))
        assertEquals(Rating.AVOID to Reason.DANGEROUS_WINDS, rate(hour(t = 15.0, p = 5.0, m = 40.0)))
        assertEquals(Rating.AVOID to Reason.HEAVY_RAIN, rate(hour(t = 15.0, p = 5.0, m = 30.0)))
    }

    @Test
    fun AC12_heat_caution_beats_wind_and_rain_caution_at_night() {
        assertEquals(Rating.CAUTION to Reason.WARM, rate(hour(t = 21.0, m = 26.0, p = 1.0)))
    }

    @Test
    fun AC12_wind_caution_beats_cold_caution() {
        assertEquals(Rating.CAUTION to Reason.WINDY, rate(hour(t = -2.0, m = 30.0)))
    }

    @Test
    fun AC8_wind_far_above_threshold_stays_avoid_and_zero_is_go() {
        assertEquals(Rating.AVOID to Reason.DANGEROUS_WINDS, rate(hour(m = 80.0)))
        assertEquals(Rating.GO to Reason.GOOD, rate(hour(m = 0.0)))
    }

    @Test
    fun AC13_missing_data_with_a_caution_factor_keeps_that_factor_reason() {
        assertEquals(Rating.CAUTION to Reason.WARM, rate(hour(t = 22.0, m = null)))
        assertEquals(Rating.CAUTION to Reason.RAIN, rate(hour(p = 1.0, m = null)))
        assertEquals(Rating.CAUTION to Reason.FREEZING, rate(hour(t = -1.0, p = null)))
    }

    @Test
    fun AC13_two_missing_values_is_still_caution_data_missing() {
        assertEquals(Rating.CAUTION to Reason.DATA_MISSING, rate(hour(t = 15.0, p = null, m = null)))
        assertEquals(Rating.CAUTION to Reason.DATA_MISSING, rate(hour(t = null, p = null, m = 5.0)))
        assertEquals(Rating.CAUTION to Reason.DATA_MISSING, rate(hour(t = null, p = 0.0, m = null)))
    }

    @Test
    fun AC14_missing_data_does_not_hide_avoid_from_heat_wind_or_pavement() {
        assertEquals(Rating.AVOID to Reason.TOO_HOT, rate(hour(t = 25.0, m = null)))
        assertEquals(Rating.AVOID to Reason.DANGEROUS_WINDS, rate(hour(t = null, m = 39.0)))
        assertEquals(
            Rating.AVOID to Reason.PAVEMENT_TOO_HOT,
            rate(hour(t = 25.0, m = null, cloud = 0.0), daylight = true),
        )
        assertEquals(Rating.AVOID to Reason.HEAVY_RAIN, rate(hour(t = null, p = 6.0, m = null)))
    }

    @Test
    fun AC14_missing_temperature_with_wind_present_is_rated_from_wind() {
        assertEquals(Rating.CAUTION to Reason.WINDY, rate(hour(t = null, m = 30.0)))
    }

    @Test
    fun AC15_cloud_cover_alone_does_not_count_as_core_data() {
        assertTrue(RatingEngine.hasNoCoreData(hour(t = null, p = null, m = null, cloud = 20.0)))
        assertTrue(RatingEngine.hasNoCoreData(hour(t = null, p = null, m = null, cloud = null)))
    }

    @Test
    fun AC1_start_instant_is_carried_through() {
        assertEquals(start, RatingEngine.rate(hour(), HeatSensitivity.NORMAL, false).startUtc)
    }
}
