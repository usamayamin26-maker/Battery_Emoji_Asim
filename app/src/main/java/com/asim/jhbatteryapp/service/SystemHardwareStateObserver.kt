package com.asim.jhbatteryapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings

/**
 * Dedicated observer for tracking physical hardware states and system broadcasts.
 * Decouples hardware/network monitoring from the Accessibility/Overlay Service lifecycle.
 */
class SystemHardwareStateObserver {

    var currentState: HardwareState = HardwareState()
        private set

    private var onStateChangedListener: ((HardwareState) -> Unit)? = null

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null) return
            refreshState(context, intent)
            onStateChangedListener?.invoke(currentState)
        }
    }

    fun startObserving(context: Context, listener: (HardwareState) -> Unit) {
        this.onStateChangedListener = listener

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            context.registerReceiver(systemReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initial snapshot
        refreshState(context, null)
        listener(currentState)
    }

    fun stopObserving(context: Context) {
        try {
            context.unregisterReceiver(systemReceiver)
        } catch (_: Exception) {}
        onStateChangedListener = null
    }

    fun refreshState(context: Context, intent: Intent? = null) {
        var isCharging = currentState.isCharging
        var batteryLevel = currentState.batteryLevel

        var actualIntent = intent
        if (actualIntent == null) {
            try {
                actualIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            } catch (_: Exception) {}
        }

        if (actualIntent?.action == Intent.ACTION_BATTERY_CHANGED) {
            val level = actualIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = actualIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = actualIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100 / scale)
            }
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        } else if (intent?.action == Intent.ACTION_POWER_CONNECTED) {
            isCharging = true
        } else if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
            isCharging = false
        }

        currentState = HardwareState(
            isAirplaneActive = isAirplaneModeOn(context),
            isRingerSilentOrVibrate = isRingerSilentOrVibrate(context),
            isWifiConnected = isWifiConnected(context),
            isMobileDataConnected = isMobileDataConnected(context),
            isHotspotActive = isHotspotOn(context),
            isCharging = isCharging,
            batteryLevel = batteryLevel
        )
    }

    private fun isAirplaneModeOn(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0
        } catch (_: Exception) {
            false
        }
    }

    private fun isRingerSilentOrVibrate(context: Context): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val mode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            mode != AudioManager.RINGER_MODE_NORMAL
        } catch (_: Exception) {
            false
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } else {
                @Suppress("DEPRECATION")
                val info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                info?.isConnected == true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isMobileDataConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val caps = cm.getNetworkCapabilities(activeNetwork)
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return true
                    }
                }
                cm.allNetworks.any { net ->
                    val caps = cm.getNetworkCapabilities(net)
                    caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                }
            } else {
                @Suppress("DEPRECATION")
                val info = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                info?.isConnected == true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isHotspotOn(context: Context): Boolean {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val method = wm?.javaClass?.getDeclaredMethod("isWifiApEnabled")
            method?.isAccessible = true
            (method?.invoke(wm) as? Boolean) == true
        } catch (_: Exception) {
            false
        }
    }
}
