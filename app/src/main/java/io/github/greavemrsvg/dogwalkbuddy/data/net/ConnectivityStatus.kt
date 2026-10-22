package io.github.greavemrsvg.dogwalkbuddy.data.net

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow

/** Whether the phone has a working network connection right now (AC39). */
fun interface ConnectivityStatus {
    fun isOnline(): Boolean

    /**
     * Emits each time the default network changes, so a screen that stays open can read [isOnline] again (AC21).
     * The default never emits, for sources that cannot change.
     */
    fun changes(): Flow<Unit> = emptyFlow()
}

/** Online when the active network can reach the internet and the system has validated it (ADR 0003). */
class AndroidConnectivityStatus(
    private val manager: ConnectivityManager,
) : ConnectivityStatus {
    override fun isOnline(): Boolean {
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** A default-network callback while collected; it is unregistered when collection stops. */
    override fun changes(): Flow<Unit> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(Unit)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        trySend(Unit)
                    }

                    override fun onLost(network: Network) {
                        trySend(Unit)
                    }
                }
            manager.registerDefaultNetworkCallback(callback)
            awaitClose { manager.unregisterNetworkCallback(callback) }
        }.conflate()
}
