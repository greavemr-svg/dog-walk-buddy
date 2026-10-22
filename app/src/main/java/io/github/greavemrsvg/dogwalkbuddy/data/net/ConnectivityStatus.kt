package io.github.greavemrsvg.dogwalkbuddy.data.net

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Whether the phone has a working network connection right now (AC39). */
fun interface ConnectivityStatus {
    fun isOnline(): Boolean
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
}
