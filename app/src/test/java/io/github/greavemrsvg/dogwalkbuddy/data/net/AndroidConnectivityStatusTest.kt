package io.github.greavemrsvg.dogwalkbuddy.data.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowNetworkCapabilities

/** AC39: the walk detail asks whether the phone is online. */
@RunWith(AndroidJUnit4::class)
class AndroidConnectivityStatusTest {
    private lateinit var manager: ConnectivityManager

    @Before
    fun setUp() {
        manager = ApplicationProvider.getApplicationContext<Context>().getSystemService(ConnectivityManager::class.java)
    }

    private fun capabilities(vararg capability: Int): NetworkCapabilities {
        val result = ShadowNetworkCapabilities.newInstance()
        capability.forEach { shadowOf(result).addCapability(it) }
        return result
    }

    @Test
    fun AC39_a_validated_internet_network_is_online() {
        shadowOf(manager).setNetworkCapabilities(
            manager.activeNetwork,
            capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET, NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        )

        assertTrue(AndroidConnectivityStatus(manager).isOnline())
    }

    @Test
    fun AC39_a_network_the_system_has_not_validated_is_offline() {
        shadowOf(manager).setNetworkCapabilities(
            manager.activeNetwork,
            capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        )

        assertFalse(AndroidConnectivityStatus(manager).isOnline())
    }

    @Test
    fun AC39_a_network_without_internet_is_offline() {
        shadowOf(manager).setNetworkCapabilities(
            manager.activeNetwork,
            capabilities(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        )

        assertFalse(AndroidConnectivityStatus(manager).isOnline())
    }

    @Test
    fun AC39_no_active_network_is_offline() {
        shadowOf(manager).setActiveNetworkInfo(null)
        shadowOf(manager).setDefaultNetworkActive(false)

        assertFalse(AndroidConnectivityStatus(manager).isOnline())
    }

    @Test
    fun AC39_an_active_network_with_no_capabilities_is_offline() {
        shadowOf(manager).setNetworkCapabilities(manager.activeNetwork, null)

        assertFalse(AndroidConnectivityStatus(manager).isOnline())
    }

    @Test
    fun UX_AC21_network_changes_are_reported_while_collected_and_the_callback_is_removed_afterwards() =
        runBlocking {
            withTimeout(WAIT_MS) {
                var changes = 0
                val job = launch { AndroidConnectivityStatus(manager).changes().collect { changes++ } }
                while (shadowOf(manager).networkCallbacks.isEmpty()) yield()
                assertEquals(1, shadowOf(manager).networkCallbacks.size)

                val callback = shadowOf(manager).networkCallbacks.single()
                val network = manager.activeNetwork!!
                callback.onLost(network)
                while (changes < 1) yield()
                callback.onAvailable(network)
                while (changes < 2) yield()
                callback.onCapabilitiesChanged(network, capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                while (changes < 3) yield()
                job.cancelAndJoin()

                assertEquals(3, changes)
                assertTrue(shadowOf(manager).networkCallbacks.isEmpty())
            }
        }

    @Test
    fun UX_AC21_a_status_that_cannot_change_reports_no_changes() =
        runBlocking {
            assertEquals(emptyList<Unit>(), ConnectivityStatus { true }.changes().toList())
        }

    private companion object {
        const val WAIT_MS = 5_000L
    }
}
