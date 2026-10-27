package com.jhw.batteryemoji.core.Utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.core.content.getSystemService

object ConnectivityManager {

    private var connectivityListener: ConnectivityListener? = null
    private var connectivityManager: ConnectivityManager? = null

    // Init method to initialize ConnectivityManager with context
    fun init(context: Context) {
        connectivityManager = context.getSystemService<ConnectivityManager>()

        // Register network callback to observe connectivity changes
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val isConnected = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                connectivityListener?.onConnectivityChanged(isConnected)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                connectivityListener?.onConnectivityChanged(false)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                connectivityListener?.onConnectivityChanged(false)
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectivityListener?.onConnectivityChanged(true)
               // RemoteConfig.createConfigSettings()
            }
        }

        connectivityManager?.registerDefaultNetworkCallback(callback)
    }

    // Method to check internet availability using existing setup
    fun isInternetAvailable(): Boolean {
        val network = connectivityManager?.activeNetwork ?: return false
        val networkCapabilities = connectivityManager?.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Method to set the listener for connectivity changes
    fun setConnectivityListener(listener: ConnectivityListener) {
        connectivityListener = listener
    }

    // Method to remove the listener
    fun removeConnectivityListener() {
        connectivityListener = null
    }

    // Connectivity Listener Interface
    interface ConnectivityListener {
        fun onConnectivityChanged(isConnected: Boolean)
    }
}
