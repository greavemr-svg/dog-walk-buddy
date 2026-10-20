package io.github.greavemrsvg.dogwalkbuddy.data.weather

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Spec Assumption 1: the MET Norway "complete" product has hourly temperature, precipitation,
 * mean wind and cloud cover for the first 12 hours at a UK point. The fixture is a live capture
 * for 51.51, -0.13 (public forecast data, no personal data).
 */
class MetFixtureContractTest {
    private val timeseries: List<JsonObject> by lazy {
        val text =
            checkNotNull(javaClass.classLoader?.getResourceAsStream(FIXTURE)) { "Missing fixture $FIXTURE" }
                .use { it.readBytes().decodeToString() }
        Json
            .parseToJsonElement(text)
            .jsonObject
            .getValue("properties")
            .jsonObject
            .getValue("timeseries")
            .jsonArray
            .map { it.jsonObject }
    }

    @Test
    fun Assumption1_met_complete_has_hourly_temp_precip_wind_cloud_for_uk_point() {
        val firstHours = timeseries.take(HOURS_REQUIRED)
        assertEquals(HOURS_REQUIRED, firstHours.size)

        val times = firstHours.map { Instant.parse(it.getValue("time").jsonPrimitive.content) }
        times.zipWithNext().forEach { (earlier, later) ->
            assertEquals(Duration.ofHours(1), Duration.between(earlier, later))
        }

        firstHours.forEachIndexed { index, entry ->
            val data = entry.getValue("data").jsonObject
            val instant =
                data
                    .getValue("instant")
                    .jsonObject
                    .getValue("details")
                    .jsonObject
            val nextHour = data["next_1_hours"]?.jsonObject?.get("details")?.jsonObject

            assertNumber("air_temperature", instant["air_temperature"]?.jsonPrimitive?.content, index)
            assertNumber("wind_speed", instant["wind_speed"]?.jsonPrimitive?.content, index)
            assertNumber("cloud_area_fraction", instant["cloud_area_fraction"]?.jsonPrimitive?.content, index)
            assertNumber("precipitation_amount", nextHour?.get("precipitation_amount")?.jsonPrimitive?.content, index)
        }
    }

    @Test
    fun Assumption1_fixture_units_are_celsius_mm_metres_per_second_and_percent() {
        val units = fixtureRoot().obj("properties").obj("meta").obj("units")

        assertEquals("celsius", units.text("air_temperature"))
        assertEquals("mm", units.text("precipitation_amount"))
        assertEquals("m/s", units.text("wind_speed"))
        assertEquals("%", units.text("cloud_area_fraction"))
    }

    @Test
    fun Assumption1_values_are_in_plausible_ranges() {
        timeseries.take(HOURS_REQUIRED).forEach { entry ->
            val data = entry.obj("data")
            val instant = data.obj("instant").obj("details")
            val rainDetails = data.obj("next_1_hours").obj("details")

            assertTrue(instant.text("cloud_area_fraction").toDouble() in 0.0..100.0)
            assertTrue(instant.text("wind_speed").toDouble() >= 0.0)
            assertTrue(rainDetails.text("precipitation_amount").toDouble() >= 0.0)
        }
    }

    @Test
    fun Assumption1_the_fixture_is_for_a_uk_point() {
        val coordinates = fixtureRoot().obj("geometry").getValue("coordinates").jsonArray
        val lon = coordinates[0].jsonPrimitive.content.toDouble()
        val lat = coordinates[1].jsonPrimitive.content.toDouble()

        assertTrue(lat in 49.8..60.9)
        assertTrue(lon in -8.7..1.8)
    }

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private fun fixtureRoot(): JsonObject {
        val text =
            checkNotNull(javaClass.classLoader?.getResourceAsStream(FIXTURE))
                .use { it.readBytes().decodeToString() }
        return Json.parseToJsonElement(text).jsonObject
    }

    private fun assertNumber(
        field: String,
        value: String?,
        hourIndex: Int,
    ) {
        assertNotNull("$field missing in hour $hourIndex", value)
        assertTrue("$field is not a number in hour $hourIndex", value?.toDoubleOrNull() != null)
    }

    private companion object {
        const val FIXTURE = "met/complete_london_51.51_-0.13.json"
        const val HOURS_REQUIRED = 12
    }
}
