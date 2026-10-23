package io.github.greavemrsvg.dogwalkbuddy.data.net

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowConnectivityManager

/** Security review R-02: the system can refuse a network callback, for example above the per-app limit. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ConnectivityCallbackRefusedTest.RefusingConnectivityManager::class])
class ConnectivityCallbackRefusedTest {
    @Implements(ConnectivityManager::class)
    class RefusingConnectivityManager : ShadowConnectivityManager() {
        @Implementation
        override fun registerDefaultNetworkCallback(networkCallback: ConnectivityManager.NetworkCallback): Unit =
            throw SecurityException("callback refused")
    }

    private val manager = ApplicationProvider.getApplicationContext<Context>().getSystemService(ConnectivityManager::class.java)

    @Test
    fun UX_AC21_a_refused_network_callback_ends_the_changes_quietly_instead_of_crashing() =
        runBlocking {
            val changes = withTimeout(WAIT_MS) { AndroidConnectivityStatus(manager).changes().toList() }

            assertEquals(emptyList<Unit>(), changes)
        }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
