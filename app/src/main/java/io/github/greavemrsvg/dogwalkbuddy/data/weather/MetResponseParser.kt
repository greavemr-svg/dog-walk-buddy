package io.github.greavemrsvg.dogwalkbuddy.data.weather

import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.format.DateTimeParseException

/** The response is not usable at all (bad JSON, no timeseries, unparseable times). The check has failed. */
class MalformedWeatherResponseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Turns a MET Locationforecast "complete" body into provider-neutral hourly conditions (ADR 0002).
 * The body is untrusted: a value that is missing, of the wrong type, non-finite or out of range becomes
 * null ("missing"), and a malformed document throws [MalformedWeatherResponseException]. It never yields Go
 * by itself, because the rating engine treats a missing value as missing data.
 */
object MetResponseParser {
    private const val METRES_PER_SECOND_TO_MPH = 2.236936

    private val temperatureRangeC = -60.0..60.0
    private val precipitationRangeMmPerHour = 0.0..500.0
    private val windRangeMetresPerSecond = 0.0..100.0
    private val cloudRangePercent = 0.0..100.0

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): List<HourlyConditions> {
        val timeseries = readTimeseries(body)
        return timeseries.take(WeatherConfig.MAX_PERIODS_KEPT).map { entry -> toConditions(entry) }
    }

    private fun readTimeseries(body: String): JsonArray {
        val root =
            try {
                json.parseToJsonElement(body)
            } catch (error: SerializationException) {
                throw MalformedWeatherResponseException("Response is not valid JSON", error)
            } catch (error: IllegalArgumentException) {
                throw MalformedWeatherResponseException("Response is not valid JSON", error)
            }
        val timeseries = (root as? JsonObject)?.obj("properties")?.get("timeseries") as? JsonArray
        if (timeseries == null || timeseries.isEmpty()) {
            throw MalformedWeatherResponseException("Response has no properties.timeseries periods")
        }
        return timeseries
    }

    private fun toConditions(entry: JsonElement): HourlyConditions {
        val period = entry as? JsonObject ?: throw MalformedWeatherResponseException("Timeseries entry is not an object")
        val startUtc = parseTime(period.text("time"))
        val data = period.obj("data")
        val instant = data?.obj("instant")?.obj("details")
        val nextHour = data?.obj("next_1_hours")?.obj("details")
        val windMetresPerSecond = instant.number("wind_speed", windRangeMetresPerSecond)
        return HourlyConditions(
            startUtc = startUtc,
            temperatureC = instant.number("air_temperature", temperatureRangeC),
            precipitationMmPerHour = nextHour.number("precipitation_amount", precipitationRangeMmPerHour),
            windMeanMph = windMetresPerSecond?.let { it * METRES_PER_SECOND_TO_MPH },
            cloudCoverPercent = instant.number("cloud_area_fraction", cloudRangePercent),
        )
    }

    private fun parseTime(text: String?): Instant {
        if (text == null) throw MalformedWeatherResponseException("Timeseries entry has no time")
        return try {
            Instant.parse(text)
        } catch (error: DateTimeParseException) {
            throw MalformedWeatherResponseException("Timeseries time is not a valid instant", error)
        }
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    /** A finite number inside [range], else null. Quoted numbers, NaN and Infinity count as the wrong type. */
    private fun JsonObject?.number(
        key: String,
        range: ClosedFloatingPointRange<Double>,
    ): Double? {
        val primitive = this?.get(key) as? JsonPrimitive ?: return null
        if (primitive.isString) return null
        val value = primitive.content.toDoubleOrNull() ?: return null
        return value.takeIf { it.isFinite() && it in range }
    }
}
