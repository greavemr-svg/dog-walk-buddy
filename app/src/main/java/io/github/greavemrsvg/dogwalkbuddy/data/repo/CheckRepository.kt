package io.github.greavemrsvg.dogwalkbuddy.data.repo

import io.github.greavemrsvg.dogwalkbuddy.data.db.CachedCheckDao
import io.github.greavemrsvg.dogwalkbuddy.data.db.CachedCheckEntity
import io.github.greavemrsvg.dogwalkbuddy.data.weather.CoordinateRounding
import io.github.greavemrsvg.dogwalkbuddy.data.weather.MetNorwayClient
import io.github.greavemrsvg.dogwalkbuddy.data.weather.WeatherFetchException
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CachePolicy
import io.github.greavemrsvg.dogwalkbuddy.domain.check.CheckData
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import io.github.greavemrsvg.dogwalkbuddy.domain.rating.HourlyConditions
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Instant

/**
 * Owns the weather check data: fetch from MET, keep the last success, and fall back to it (AC21, AC22).
 * It does nothing until [check] is called, and the screen calls that only on open or refresh (AC24).
 */
class CheckRepository(
    private val client: MetNorwayClient,
    private val dao: CachedCheckDao,
    private val clock: Clock,
) {
    /**
     * Fetches for [fix] and saves the result. When there is no location or the fetch fails, returns the
     * cached check if it is under 3 h old, else [CheckData.Unavailable].
     */
    suspend fun check(fix: LocationFix?): CheckData {
        if (fix == null) return loadCached()
        return try {
            fetchAndSave(fix)
        } catch (_: WeatherFetchException) {
            // Offline, timeout, error status or malformed body: the cache decides (AC21, AC22).
            loadCached()
        }
    }

    /** The cached check when it is under 3 h old, else [CheckData.Unavailable]. Makes no network request. */
    suspend fun loadCached(): CheckData {
        val entity = dao.get() ?: return CheckData.Unavailable
        val fetchedAt = Instant.ofEpochMilli(entity.fetchedUtcMs)
        if (!CachePolicy.isUsable(fetchedAt, clock.instant())) return CheckData.Unavailable
        val hours = decodeHours(entity.hoursJson) ?: return CheckData.Unavailable
        return CheckData.Available(hours, fetchedAt, entity.lat, entity.lon, isCached = true)
    }

    private suspend fun fetchAndSave(fix: LocationFix): CheckData.Available {
        val hours = client.fetch(fix.latitude, fix.longitude)
        val fetchedAt = clock.instant()
        val latitude = CoordinateRounding.round(fix.latitude)
        val longitude = CoordinateRounding.round(fix.longitude)
        dao.save(
            CachedCheckEntity(fetchedUtcMs = fetchedAt.toEpochMilli(), lat = latitude, lon = longitude, hoursJson = encodeHours(hours)),
        )
        return CheckData.Available(hours, fetchedAt, latitude, longitude, isCached = false)
    }

    private fun encodeHours(hours: List<HourlyConditions>): String = json.encodeToString(hours.map(CachedHour::from))

    private fun decodeHours(text: String): List<HourlyConditions>? =
        try {
            json.decodeFromString<List<CachedHour>>(text).map(CachedHour::toConditions)
        } catch (_: SerializationException) {
            // A damaged cache row is treated as no cache rather than a crash.
            null
        }

    @Serializable
    private data class CachedHour(
        val startUtcMs: Long,
        val temperatureC: Double? = null,
        val precipitationMmPerHour: Double? = null,
        val windMeanMph: Double? = null,
        val cloudCoverPercent: Double? = null,
    ) {
        fun toConditions() =
            HourlyConditions(Instant.ofEpochMilli(startUtcMs), temperatureC, precipitationMmPerHour, windMeanMph, cloudCoverPercent)

        companion object {
            fun from(hour: HourlyConditions) =
                CachedHour(
                    hour.startUtc.toEpochMilli(),
                    hour.temperatureC,
                    hour.precipitationMmPerHour,
                    hour.windMeanMph,
                    hour.cloudCoverPercent,
                )
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
