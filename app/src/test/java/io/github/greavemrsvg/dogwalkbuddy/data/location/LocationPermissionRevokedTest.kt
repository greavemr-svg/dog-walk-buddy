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
import java.time.Instant

/** The platform throws SecurityException when the user revokes location after the app's own check passed. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [LocationPermissionRevokedTest.RevokedLocationManager::class])
class LocationPermissionRevokedTest {
    @Implements(LocationManager::class)
    class RevokedLocationManager : ShadowLocationManager() {
        @Implementation
        override fun getLastKnownLocation(provider: String): Location? = throw SecurityException("permission revoked")

        @Implementation
        override fun isProviderEnabled(provider: String): Boolean = true
    }

    private val manager = ApplicationProvider.getApplicationContext<Context>().getSystemService(LocationManager::class.java)

    private fun source() =
        LocationManagerApproximateSource(
            manager,
            { true },
            MutableClock(Instant.parse("2026-10-19T14:20:00Z")),
            { it.run() },
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
        )

    @Test
    fun AC25_a_permission_revoked_after_the_check_gives_no_location_instead_of_a_crash() =
        runTest {
            assertNull(source().current())
        }
}
