package com.lji.assignment.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * An interface to observe the device's network connectivity status.
 */
interface NetworkObserver {
    fun observe(): Flow<NetworkStatus>
}

/**
 * A sealed class to represent the different network statuses.
 */
sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Unavailable : NetworkStatus()
}

/**
 * An implementation of [NetworkObserver] that uses [ConnectivityManager]
 * to monitor network status changes.
 * @param context The application context.
 */
class NetworkObserverImpl(context: Context) : NetworkObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Observes the network status and emits updates as a Flow.
     */
    override fun observe(): Flow<NetworkStatus> = callbackFlow @androidx.annotation.RequiresPermission(
        android.Manifest.permission.ACCESS_NETWORK_STATE
    ) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                // When a network becomes available, re-check the overall status
                trySend(if (isNetworkAvailable()) NetworkStatus.Available else NetworkStatus.Unavailable)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(if (isNetworkAvailable()) NetworkStatus.Available else NetworkStatus.Unavailable)
            }

            // You might also want to override onCapabilitiesChanged or onLosing for more granular updates
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                trySend(if (isNetworkAvailable()) NetworkStatus.Available else NetworkStatus.Unavailable)
            }
        }

        // Check initial network status.
        val initialStatus = if (isNetworkAvailable()) NetworkStatus.Available else NetworkStatus.Unavailable
        trySend(initialStatus)

        // Register the network callback to listen for changes.
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)

        // Unregister the callback when the flow is closed.
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    /**
     * Checks if any network connection is currently available and has internet capability.
     * This is a more robust synchronous check for the initial state and whenever network changes occur.
     */
    private fun isNetworkAvailable(): Boolean {
        // For API 23 (Marshmallow) and above, get all active networks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networks = connectivityManager.allNetworks
            for (network in networks) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities != null) {
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                    ) {
                        return true // Found a valid, internet-capable network
                    }
                }
            }
            return false
        } else {
            // For older APIs, rely on active network info (less reliable)
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }
}