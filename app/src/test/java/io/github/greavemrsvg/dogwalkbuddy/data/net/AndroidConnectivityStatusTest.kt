package io.github.greavemrsvg.dogwalkbuddy.data.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
