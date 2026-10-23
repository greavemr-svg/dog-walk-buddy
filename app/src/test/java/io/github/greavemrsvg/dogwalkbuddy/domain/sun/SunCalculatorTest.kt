package io.github.greavemrsvg.dogwalkbuddy.domain.sun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** Published London (51.51, -0.13) times, converted to UTC. The tolerance is 2 minutes. */
class SunCalculatorTest {
    private val lat = 51.51
    private val lon = -0.13

    private fun assertWithinTwoMinutes(
        expected: String,
        actual: Instant?,
    ) {
        assertNotNull(actual)
        val diff = Duration.between(Instant.parse(expected), actual).abs()
        assertTrue("expected $expected but was $actual", diff <= Duration.ofMinutes(2))
    }

    @Test
    fun midsummer_2026_sunrise_and_sunset() {
        val times = SunCalculator.sunTimes(Instant.parse("2026-06-21T12:00:00Z"), lat, lon)

        assertWithinTwoMinutes("2026-06-21T03:43:00Z", times.sunrise)
        assertWithinTwoMinutes("2026-06-21T20:21:00Z", times.sunset)
    }

    @Test
    fun midwinter_2026_sunrise_and_sunset() {
        val times = SunCalculator.sunTimes(Instant.parse("2026-12-21T12:00:00Z"), lat, lon)

        assertWithinTwoMinutes("2026-12-21T08:04:00Z", times.sunrise)
        assertWithinTwoMinutes("2026-12-21T15:53:00Z", times.sunset)
    }

    @Test
    fun isDaylight_is_true_between_sunrise_and_sunset_only() {
        assertTrue(SunCalculator.isDaylight(Instant.parse("2026-10-19T12:00:00Z"), lat, lon))
        assertFalse(SunCalculator.isDaylight(Instant.parse("2026-10-19T02:00:00Z"), lat, lon))
        assertFalse(SunCalculator.isDaylight(Instant.parse("2026-10-19T23:00:00Z"), lat, lon))
    }

    @Test
    fun an_hour_straddling_sunrise_takes_the_state_at_its_start() {
        val sunrise = SunCalculator.sunTimes(Instant.parse("2026-12-21T12:00:00Z"), lat, lon).sunrise!!

        assertFalse(SunCalculator.isDaylight(sunrise.minusSeconds(1), lat, lon))
        assertTrue(SunCalculator.isDaylight(sunrise, lat, lon))
    }

    @Test
    fun sunset_instant_itself_is_not_daylight() {
        val sunset = SunCalculator.sunTimes(Instant.parse("2026-12-21T12:00:00Z"), lat, lon).sunset!!

        assertTrue(SunCalculator.isDaylight(sunset.minusSeconds(1), lat, lon))
        assertFalse(SunCalculator.isDaylight(sunset, lat, lon))
    }

    @Test
    fun polar_night_has_no_sunrise_and_is_never_daylight() {
        val noon = Instant.parse("2026-12-21T12:00:00Z")

        val times = SunCalculator.sunTimes(noon, 80.0, 0.0)

        assertNull(times.sunrise)
        assertNull(times.sunset)
        assertFalse(SunCalculator.isDaylight(noon, 80.0, 0.0))
    }

    @Test
    fun midnight_sun_has_no_sunset_and_is_always_daylight() {
        val midnight = Instant.parse("2026-06-21T00:30:00Z")

        assertTrue(SunCalculator.isDaylight(midnight, 80.0, 0.0))
        assertEquals(true, SunCalculator.sunTimes(midnight, 80.0, 0.0).sunAlwaysUp)
    }
}
