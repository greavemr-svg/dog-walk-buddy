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

    /**
     * A default-network callback while collected; it is unregistered when collection stops. If the system refuses the
     * callback, the flow ends without emitting, so a screen keeps the connection state it read when it opened.
     */
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
            val registered = register(callback)
            if (!registered) close()
            awaitClose { if (registered) manager.unregisterNetworkCallback(callback) }
        }.conflate()

    private fun register(callback: ConnectivityManager.NetworkCallback): Boolean =
        try {
            manager.registerDefaultNetworkCallback(callback)
            true
        } catch (_: RuntimeException) {
            // Too many callbacks for this app (TooManyRequestsException) or ACCESS_NETWORK_STATE missing
            // (SecurityException): live updates are optional, so carry on without them (security review R-02).
            false
        }
}
