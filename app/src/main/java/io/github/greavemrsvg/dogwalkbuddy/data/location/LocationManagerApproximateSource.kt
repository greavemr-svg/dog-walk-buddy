package io.github.greavemrsvg.dogwalkbuddy.data.location

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.location.LocationManagerCompat
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * Approximate location through the platform [LocationManager] (ADR 0005). It uses a last known fix under
 * [MAX_LAST_KNOWN_AGE] old first, then asks for one current fix. The fused provider is used on API 31+ and the
 * network provider before that. It never uses the GPS provider, and it never asks for permission itself.
 */
class LocationManagerApproximateSource(
    private val locationManager: LocationManager,
    private val hasCoarsePermission: () -> Boolean,
    private val clock: Clock,
    private val executor: Executor,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val timeout: Duration = REQUEST_TIMEOUT,
) : ApproximateLocationSource {
    // The permission is checked through hasCoarsePermission(), which lint cannot see through.
    @SuppressLint("MissingPermission")
    override suspend fun current(): LocationFix? {
        if (!hasCoarsePermission()) return null
        val provider = provider()
        if (!locationManager.isProviderEnabled(provider)) return null
        val lastKnown = locationManager.getLastKnownLocation(provider)
        if (lastKnown != null && isRecent(lastKnown)) return lastKnown.toFix()
        return withTimeoutOrNull(timeout.toMillis()) { requestCurrent(provider) }?.toFix()
    }

    // FUSED_PROVIDER is a plain string constant that is only read on API 31 and later, so the inlined value is safe.
    @SuppressLint("InlinedApi")
    private fun provider(): String =
        if (sdkInt >= Build.VERSION_CODES.S) LocationManager.FUSED_PROVIDER else LocationManager.NETWORK_PROVIDER

    private fun isRecent(location: Location): Boolean {
        val age = Duration.between(Instant.ofEpochMilli(location.time), clock.instant())
        return !age.isNegative && age <= MAX_LAST_KNOWN_AGE
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrent(provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            LocationManagerCompat.getCurrentLocation(locationManager, provider, signal, executor) { location ->
                if (continuation.isActive) continuation.resume(location)
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
        val MAX_LAST_KNOWN_AGE: Duration = Duration.ofMinutes(30)
        val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
