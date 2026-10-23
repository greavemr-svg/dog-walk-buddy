package io.github.greavemrsvg.dogwalkbuddy.data.net

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowConnectivityManager

/**
 * Security review R-02 (issue #48): above the per-app callback limit the system throws its hidden
 * `ConnectivityManager.TooManyRequestsException`, a [RuntimeException]. The live offline lead must then end quietly,
 * and nothing must try to unregister a callback the system never accepted (that throws IllegalArgumentException).
 */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ConnectivityCallbackLimitTest.OverLimitConnectivityManager::class])
class ConnectivityCallbackLimitTest {
    @Implements(ConnectivityManager::class)
    class OverLimitConnectivityManager : ShadowConnectivityManager() {
        @Implementation
        override fun registerDefaultNetworkCallback(networkCallback: ConnectivityManager.NetworkCallback) {
            registerAttempts++
            throw tooManyRequests()
        }

        @Implementation
        override fun unregisterNetworkCallback(networkCallback: ConnectivityManager.NetworkCallback) {
            unregisterCalls++
            throw IllegalArgumentException("NetworkCallback was not registered")
        }
    }

    private val manager = ApplicationProvider.getApplicationContext<Context>().getSystemService(ConnectivityManager::class.java)

    @Before
    fun resetCounts() {
        registerAttempts = 0
        unregisterCalls = 0
    }

    @Test
    fun UX_AC21_R02_a_callback_over_the_per_app_limit_ends_the_changes_quietly_instead_of_crashing() =
        runBlocking {
            val changes = withTimeout(WAIT_MS) { AndroidConnectivityStatus(manager).changes().toList() }

            assertEquals(emptyList<Unit>(), changes)
            assertEquals(1, registerAttempts)
        }

    @Test
    fun UX_AC21_R02_a_refused_callback_is_never_unregistered() =
        runBlocking {
            withTimeout(WAIT_MS) { AndroidConnectivityStatus(manager).changes().toList() }

            assertEquals(0, unregisterCalls)
        }

    @Test
    fun UX_AC21_R02_each_collection_tries_to_register_again_and_ends_quietly_each_time() =
        runBlocking {
            val status = AndroidConnectivityStatus(manager)

            repeat(COLLECTIONS) { withTimeout(WAIT_MS) { assertEquals(emptyList<Unit>(), status.changes().toList()) } }

            assertEquals(COLLECTIONS, registerAttempts)
            assertEquals(0, unregisterCalls)
        }

    @Test
    fun UX_AC21_R02_the_on_open_reading_still_works_when_live_updates_are_refused() =
        runBlocking {
            val status = AndroidConnectivityStatus(manager)
            val before = status.isOnline()

            withTimeout(WAIT_MS) { status.changes().toList() }

            assertEquals(before, status.isOnline())
        }

    companion object {
        const val WAIT_MS = 5_000L
        const val COLLECTIONS = 3

        @JvmStatic var registerAttempts = 0

        @JvmStatic var unregisterCalls = 0

        /** The platform's hidden exception when available, otherwise a plain [RuntimeException] of the same kind. */
        fun tooManyRequests(): RuntimeException =
            runCatching {
                Class
                    .forName("android.net.ConnectivityManager\$TooManyRequestsException")
                    .getDeclaredConstructor()
                    .apply { isAccessible = true }
                    .newInstance() as RuntimeException
            }.getOrElse { RuntimeException("Too many NetworkRequests filed") }
    }
}
