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
