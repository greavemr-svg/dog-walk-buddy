package io.github.greavemrsvg.dogwalkbuddy.data.location

import android.location.Location
import android.location.LocationManager
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import java.util.concurrent.Executor

/**
 * Precise locations from the platform GPS provider (ADR 0005): every 3 s, no minimum distance, high accuracy.
 * The GPS provider works offline and needs no Google Play Services. It is used only while a walk is recorded,
 * and it never asks for permission itself. The flow ends without an error if the permission is missing or the
 * provider is unavailable, so the walk keeps timing.
 */
class GpsWalkLocationSource(
    private val locationManager: LocationManager,
    private val hasFinePermission: () -> Boolean,
    private val executor: Executor,
    private val intervalMs: Long = UPDATE_INTERVAL_MS,
) : WalkLocationSource {
    override fun fixes(): Flow<LocationFix> =
        callbackFlow {
            val listener =
                LocationListenerCompat { location -> trySend(location.toFix()) }
            val registered = register(listener)
            if (!registered) close()
            awaitClose { if (registered) locationManager.removeUpdates(listener) }
        }

    private fun register(listener: LocationListenerCompat): Boolean {
        if (!hasFinePermission()) return false
        val request =
            LocationRequestCompat
                .Builder(intervalMs)
                .setMinUpdateDistanceMeters(0f)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .build()
        return try {
            LocationManagerCompat.requestLocationUpdates(locationManager, LocationManager.GPS_PROVIDER, request, executor, listener)
            true
        } catch (_: SecurityException) {
            // Permission revoked after the check, or no GPS provider: record time only.
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun Location.toFix(): LocationFix =
        LocationFix(
            latitude = latitude,
            longitude = longitude,
            accuracyM = if (hasAccuracy()) accuracy.toDouble() else null,
            timeUtc = Instant.ofEpochMilli(time),
        )

    companion object {
        const val UPDATE_INTERVAL_MS = 3_000L
    }
}
