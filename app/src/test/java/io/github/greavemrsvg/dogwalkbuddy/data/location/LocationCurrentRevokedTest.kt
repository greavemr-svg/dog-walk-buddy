package io.github.greavemrsvg.dogwalkbuddy.data.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.greavemrsvg.dogwalkbuddy.testutil.MutableClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLocationManager
import java.time.Duration
import java.time.Instant

/** The platform can throw SecurityException from the current-location request when the user revokes location in between. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [LocationCurrentRevokedTest.RevokedCurrentLocationManager::class])
class LocationCurrentRevokedTest {
    @Implements(LocationManager::class)
    class RevokedCurrentLocationManager : ShadowLocationManager() {
        @Implementation
        override fun isProviderEnabled(provider: String): Boolean = true

        @Implementation
        override fun getLastKnownLocation(provider: String): Location? = null

        @Implementation
        override fun getCurrentLocation(
            provider: String,
            cancellationSignal: android.os.CancellationSignal?,
            executor: java.util.concurrent.Executor,
            consumer: java.util.function.Consumer<Location>,
        ): Unit = throw SecurityException("permission revoked")
    }

    private val manager = ApplicationProvider.getApplicationContext<Context>().getSystemService(LocationManager::class.java)

    @Test
    fun AC25_a_permission_revoked_during_the_current_location_request_gives_no_location_and_does_not_hang() =
        runTest {
            val source =
                LocationManagerApproximateSource(
                    manager,
                    { true },
                    MutableClock(Instant.parse("2026-10-19T14:20:00Z")),
                    { it.run() },
                    Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                    Duration.ofSeconds(30),
                )

            assertNull(source.current())
        }
}
