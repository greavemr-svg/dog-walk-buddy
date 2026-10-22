package io.github.greavemrsvg.dogwalkbuddy.data.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.domain.location.LocationFix
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Instant

/** The adapter is exercised through the platform LocationManager (ADR 0005), not through a fake. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GpsWalkLocationSourceTest {
    private lateinit var locationManager: LocationManager
    private var permissionGranted = true

    @Before
    fun setUp() {
        locationManager = ApplicationProvider.getApplicationContext<Context>().getSystemService(LocationManager::class.java)
        shadowOf(locationManager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
    }

    private fun source() = GpsWalkLocationSource(locationManager, { permissionGranted }, { it.run() })

    private fun location(
        provider: String = LocationManager.GPS_PROVIDER,
        lat: Double = 51.5,
        withAccuracy: Boolean = true,
        timeMs: Long = TIME.toEpochMilli(),
    ) = Location(provider).apply {
        latitude = lat
        longitude = -0.12
        if (withAccuracy) accuracy = 6.5f
        time = timeMs
    }

    @Test
    fun AC31_fixes_from_the_gps_provider_are_delivered_with_accuracy_and_time() =
        runTest(UnconfinedTestDispatcher()) {
            val received = mutableListOf<LocationFix>()
            val job = launch { source().fixes().toList(received) }

            shadowOf(locationManager).simulateLocation(location())

            assertEquals(1, received.size)
            assertEquals(51.5, received[0].latitude, 0.0)
            assertEquals(6.5, received[0].accuracyM!!, 0.001)
            assertEquals(TIME, received[0].timeUtc)
            job.cancel()
        }

    @Test
    fun a_fix_without_accuracy_has_a_null_accuracy() =
        runTest(UnconfinedTestDispatcher()) {
            val received = mutableListOf<LocationFix>()
            val job = launch { source().fixes().toList(received) }

            shadowOf(locationManager).simulateLocation(location(withAccuracy = false))

            assertEquals(null, received.single().accuracyM)
            job.cancel()
        }

    @Test
    fun AC25_only_the_gps_provider_is_used_never_the_network_provider() =
        runTest(UnconfinedTestDispatcher()) {
            val received = mutableListOf<LocationFix>()
            val job = launch { source().fixes().toList(received) }

            shadowOf(locationManager).simulateLocation(location(provider = LocationManager.NETWORK_PROVIDER))

            assertTrue(received.isEmpty())
            job.cancel()
        }

    @Test
    fun cancelling_the_collection_removes_the_location_updates() =
        runTest(UnconfinedTestDispatcher()) {
            val received = mutableListOf<LocationFix>()
            val job = launch { source().fixes().toList(received) }
            shadowOf(locationManager).simulateLocation(location(timeMs = TIME.toEpochMilli()))

            job.cancel()
            shadowOf(locationManager).simulateLocation(location(timeMs = TIME.toEpochMilli() + 3000))

            assertEquals(1, received.size)
        }

    @Test
    fun AC33_without_precise_permission_nothing_is_requested_and_the_flow_ends() =
        runTest(UnconfinedTestDispatcher()) {
            permissionGranted = false

            val received = source().fixes().toList()

            assertTrue(received.isEmpty())
        }

    private companion object {
        val TIME: Instant = Instant.parse("2026-10-19T09:00:03Z")
    }
}
