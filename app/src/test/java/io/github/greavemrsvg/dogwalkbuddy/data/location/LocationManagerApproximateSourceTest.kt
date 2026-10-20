package io.github.greavemrsvg.dogwalkbuddy.data.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.testutil.MutableClock
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LocationManagerApproximateSourceTest {
    private lateinit var locationManager: LocationManager
    private val clock = MutableClock(NOW)
    private val directExecutor = Executor { it.run() }
    private var permissionGranted = true

    @Before
    fun setUp() {
        locationManager = ApplicationProvider.getApplicationContext<Context>().getSystemService(LocationManager::class.java)
    }

    private fun source(sdkInt: Int = Build.VERSION_CODES.UPSIDE_DOWN_CAKE) =
        LocationManagerApproximateSource(locationManager, { permissionGranted }, clock, directExecutor, sdkInt, TIMEOUT)

    private fun location(
        provider: String,
        ageMinutes: Long,
    ) = Location(provider).apply {
        latitude = LAT
        longitude = LON
        accuracy = ACCURACY
        time = NOW.minus(Duration.ofMinutes(ageMinutes)).toEpochMilli()
    }

    private fun enableWithLastKnown(
        provider: String,
        last: Location?,
    ) {
        shadowOf(locationManager).setProviderEnabled(provider, true)
        if (last != null) shadowOf(locationManager).simulateLocation(last)
    }

    @Test
    fun AC25_without_coarse_permission_no_location_is_read() =
        runTest {
            permissionGranted = false
            enableWithLastKnown(LocationManager.FUSED_PROVIDER, location(LocationManager.FUSED_PROVIDER, 1))

            assertNull(source().current())
        }

    @Test
    fun AC23_a_recent_last_known_fix_is_used_first() =
        runTest {
            enableWithLastKnown(LocationManager.FUSED_PROVIDER, location(LocationManager.FUSED_PROVIDER, 29))

            val fix = source().current()!!

            assertEquals(LAT, fix.latitude, 0.0)
            assertEquals(LON, fix.longitude, 0.0)
            assertEquals(ACCURACY.toDouble(), fix.accuracyM!!, 0.0)
            assertEquals(NOW.minus(Duration.ofMinutes(29)), fix.timeUtc)
        }

    @Test
    fun AC23_without_a_recent_last_known_fix_a_current_one_is_requested() =
        runTest(UnconfinedTestDispatcher()) {
            enableWithLastKnown(LocationManager.FUSED_PROVIDER, null)
            val pending = async(start = CoroutineStart.UNDISPATCHED) { source().current() }
            val current = location(LocationManager.FUSED_PROVIDER, 0).apply { latitude = LAT + 1 }

            shadowOf(locationManager).simulateLocation(current)

            assertEquals(LAT + 1, pending.await()!!.latitude, 0.0)
        }

    @Test
    fun AC23_before_android_12_the_network_provider_is_used() =
        runTest {
            enableWithLastKnown(LocationManager.NETWORK_PROVIDER, location(LocationManager.NETWORK_PROVIDER, 1))

            val fix = source(sdkInt = Build.VERSION_CODES.R).current()

            assertEquals(LAT, fix!!.latitude, 0.0)
        }

    @Test
    fun AC25_a_disabled_provider_gives_no_location() =
        runTest {
            shadowOf(locationManager).setProviderEnabled(LocationManager.FUSED_PROVIDER, false)

            assertNull(source().current())
        }

    @Test
    fun AC25_a_fix_without_accuracy_has_null_accuracy() =
        runTest {
            val bare = location(LocationManager.FUSED_PROVIDER, 1).apply { removeAccuracy() }
            enableWithLastKnown(LocationManager.FUSED_PROVIDER, bare)

            assertNull(source().current()!!.accuracyM)
        }

    @Test
    fun AC25_no_answer_within_the_timeout_gives_no_location() =
        runTest {
            enableWithLastKnown(LocationManager.FUSED_PROVIDER, null)

            assertNull(source().current())
        }

    private companion object {
        val NOW: Instant = Instant.parse("2026-10-19T14:20:00Z")
        val TIMEOUT: Duration = Duration.ofSeconds(1)
        const val LAT = 51.5
        const val LON = -0.1
        const val ACCURACY = 800f
    }
}
