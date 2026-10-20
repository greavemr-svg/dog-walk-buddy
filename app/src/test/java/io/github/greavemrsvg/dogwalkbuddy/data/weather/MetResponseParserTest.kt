package io.github.greavemrsvg.dogwalkbuddy.data.weather

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HeatSensitivity
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.Rating
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.RatingEngine
import io.github.greavemrsvg.dogwalkbuddy.testutil.StubWeatherServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MetResponseParserTest {
    @Test
    fun AC23_parses_the_live_london_fixture_into_hourly_conditions() {
        val hours = MetResponseParser.parse(StubWeatherServer.fixtureText())

        val first = hours.first()
        assertEquals(Instant.parse("2026-10-19T14:00:00Z"), first.startUtc)
        assertEquals(13.3, first.temperatureC!!, 0.0)
        assertEquals(0.0, first.precipitationMmPerHour!!, 0.0)
        assertEquals(69.5, first.cloudCoverPercent!!, 0.0)
        assertTrue(hours.size <= WeatherConfig.MAX_PERIODS_KEPT)
        assertTrue(hours.take(12).all { it.temperatureC != null && it.windMeanMph != null && it.precipitationMmPerHour != null })
    }

    @Test
    fun AC8_converts_mean_wind_from_metres_per_second_to_mph() {
        val hour = parseOne(instant = """{"wind_speed":3.5}""")

        assertEquals(3.5 * 2.236936, hour.windMeanMph!!, 1e-9)
    }

    @Test
    fun AC15_missing_fields_become_null_not_zero() {
        val hour = parseOne(instant = "{}", nextHour = null)

        assertNull(hour.temperatureC)
        assertNull(hour.windMeanMph)
        assertNull(hour.cloudCoverPercent)
        assertNull(hour.precipitationMmPerHour)
    }

    @Test
    fun AC13_out_of_range_values_become_missing() {
        val hour =
            parseOne(
                instant = """{"air_temperature":61,"wind_speed":100.5,"cloud_area_fraction":101}""",
                nextHour = """{"precipitation_amount":-0.1}""",
            )

        assertNull(hour.temperatureC)
        assertNull(hour.windMeanMph)
        assertNull(hour.cloudCoverPercent)
        assertNull(hour.precipitationMmPerHour)
    }

    @Test
    fun AC13_range_limits_are_inclusive() {
        val hour =
            parseOne(
                instant = """{"air_temperature":-60,"wind_speed":100,"cloud_area_fraction":100}""",
                nextHour = """{"precipitation_amount":500}""",
            )

        assertEquals(-60.0, hour.temperatureC!!, 0.0)
        assertNotNull(hour.windMeanMph)
        assertEquals(100.0, hour.cloudCoverPercent!!, 0.0)
        assertEquals(500.0, hour.precipitationMmPerHour!!, 0.0)
    }

    @Test
    fun AC13_wrong_typed_values_become_missing() {
        val hour =
            parseOne(
                instant = """{"air_temperature":"13.3","wind_speed":true,"cloud_area_fraction":null}""",
                nextHour = """{"precipitation_amount":[0]}""",
            )

        assertNull(hour.temperatureC)
        assertNull(hour.windMeanMph)
        assertNull(hour.cloudCoverPercent)
        assertNull(hour.precipitationMmPerHour)
    }

    @Test
    fun AC13_NaN_values_become_missing() {
        val hour =
            parseOne(
                instant = """{"air_temperature":NaN,"wind_speed":NaN,"cloud_area_fraction":NaN}""",
                nextHour = """{"precipitation_amount":NaN}""",
            )

        assertEquals(HourlyConditions(START, null, null, null, null), hour)
    }

    @Test
    fun AC13_Infinity_and_overflowing_values_become_missing() {
        val hour =
            parseOne(
                instant = """{"air_temperature":Infinity,"wind_speed":-Infinity,"cloud_area_fraction":1e999}""",
                nextHour = """{"precipitation_amount":Infinity}""",
            )

        assertEquals(HourlyConditions(START, null, null, null, null), hour)
    }

    @Test
    fun AC13_a_NaN_hour_is_never_rated_Go() {
        val hour =
            parseOne(
                instant = """{"air_temperature":NaN,"wind_speed":NaN,"cloud_area_fraction":NaN}""",
                nextHour = """{"precipitation_amount":NaN}""",
            )

        val rating = RatingEngine.rate(hour, HeatSensitivity.NORMAL, isDaylight = true).rating

        assertNotEquals(Rating.GO, rating)
    }

    @Test
    fun AC13_a_period_without_next_1_hours_has_missing_precipitation_only() {
        val hour = parseOne(instant = """{"air_temperature":10,"wind_speed":1,"cloud_area_fraction":5}""", nextHour = null)

        assertNull(hour.precipitationMmPerHour)
        assertEquals(10.0, hour.temperatureC!!, 0.0)
    }

    @Test
    fun AC22_bad_json_is_a_malformed_response() {
        assertThrows(MalformedWeatherResponseException::class.java) { MetResponseParser.parse("{not json") }
    }

    @Test
    fun AC22_truncated_json_is_a_malformed_response() {
        val truncated = StubWeatherServer.fixtureText().take(TRUNCATED_LENGTH)

        assertThrows(MalformedWeatherResponseException::class.java) { MetResponseParser.parse(truncated) }
    }

    @Test
    fun AC22_a_document_without_a_timeseries_is_malformed() {
        assertThrows(MalformedWeatherResponseException::class.java) { MetResponseParser.parse("""{"properties":{}}""") }
        assertThrows(MalformedWeatherResponseException::class.java) { MetResponseParser.parse("[]") }
        assertThrows(MalformedWeatherResponseException::class.java) { MetResponseParser.parse("""{"properties":{"timeseries":5}}""") }
    }

    @Test
    fun AC22_an_unparseable_time_is_malformed() {
        val body = """{"properties":{"timeseries":[{"time":"yesterday","data":{}}]}}"""

        assertThrows(MalformedWeatherResponseException::class.java) { MetResponseParser.parse(body) }
    }

    @Test
    fun AC22_a_missing_time_or_non_object_entry_is_malformed() {
        assertThrows(MalformedWeatherResponseException::class.java) {
            MetResponseParser.parse("""{"properties":{"timeseries":[{"data":{}}]}}""")
        }
        assertThrows(MalformedWeatherResponseException::class.java) {
            MetResponseParser.parse("""{"properties":{"timeseries":[7]}}""")
        }
    }

    @Test
    fun AC22_an_empty_timeseries_gives_no_hours() {
        assertTrue(MetResponseParser.parse("""{"properties":{"timeseries":[]}}""").isEmpty())
    }

    private fun parseOne(
        instant: String,
        nextHour: String? = "{}",
    ): HourlyConditions {
        val next = nextHour?.let { ""","next_1_hours":{"details":$it}""" }.orEmpty()
        val body =
            """{"properties":{"timeseries":[{"time":"$START_TEXT","data":{"instant":{"details":$instant}$next}}]}}"""
        return MetResponseParser.parse(body).single()
    }

    private companion object {
        const val START_TEXT = "2026-10-19T14:00:00Z"
        val START: Instant = Instant.parse(START_TEXT)
        const val TRUNCATED_LENGTH = 400
    }
}
