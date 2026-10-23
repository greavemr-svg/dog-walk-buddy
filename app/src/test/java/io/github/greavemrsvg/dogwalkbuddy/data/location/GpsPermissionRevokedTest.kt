package io.github.greavemrsvg.dogwalkbuddy.data.location

import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLocationManager
import java.util.concurrent.Executor

/** The platform throws SecurityException when the user revokes precise location after the app's own check passed. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [GpsPermissionRevokedTest.RevokedLocationManager::class])
class GpsPermissionRevokedTest {
    @Implements(LocationManager::class)
    class RevokedLocationManager : ShadowLocationManager() {
        @Implementation
        override fun requestLocationUpdates(
            provider: String,
            request: LocationRequest,
            executor: Executor,
            listener: LocationListener,
        ): Unit = throw SecurityException("permission revoked")
    }

    private val manager = ApplicationProvider.getApplicationContext<Context>().getSystemService(LocationManager::class.java)

    @Test
    fun AC33_a_permission_revoked_during_the_start_ends_the_flow_instead_of_crashing() =
        runTest {
            val received = GpsWalkLocationSource(manager, { true }, { it.run() }).fixes().toList()

            assertTrue(received.isEmpty())
        }
}
