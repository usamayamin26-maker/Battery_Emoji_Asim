package com.jhw.batteryemoji.service

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
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

/**
 * Dedicated observer for tracking physical hardware states and system broadcasts.
 * Decouples hardware/network monitoring from the Accessibility/Overlay Service lifecycle.
 */
class SystemHardwareStateObserver {

    var currentState: HardwareState = HardwareState()
        private set

    private var onStateChangedListener: ((HardwareState) -> Unit)? = null

    /** Day the listener was last called for, so the overlay's date still rolls over at midnight. */
    private var lastNotifiedDay = currentDayKey()

    private fun currentDayKey(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.YEAR) * 1000 + cal.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null) return
            val previous = currentState
            refreshState(context, intent)
            publishIfChanged(previous)
        }
    }

    /** Hotspot state from WIFI_AP_STATE_CHANGED; null until the first broadcast arrives. */
    private var hotspotFromBroadcast: Boolean? = null

    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: Any? = null

    /**
     * ACTION_BATTERY_CHANGED fires every few seconds (voltage/temperature) and signal strength
     * flickers constantly; only redraw the overlay when what it shows (or the shown date) differs.
     */
    private fun publishIfChanged(previous: HardwareState) {
        val today = currentDayKey()
        if (currentState == previous && today == lastNotifiedDay) return
        lastNotifiedDay = today
        onStateChangedListener?.invoke(currentState)
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
                addAction(ACTION_WIFI_AP_STATE_CHANGED)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            // System broadcasts still arrive; other apps can no longer fake "charger connected"
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                systemReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startSignalListener(context)

        // Initial snapshot
        refreshState(context, null)
        listener(currentState)
    }

    fun stopObserving(context: Context) {
        try {
            context.unregisterReceiver(systemReceiver)
        } catch (_: Exception) {}
        stopSignalListener()
        onStateChangedListener = null
    }

    /** Live cellular signal bars. Needs no permission; callbacks arrive on the main thread. */
    private fun startSignalListener(context: Context) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        telephonyManager = tm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        onSignalLevel(signalStrength.level)
                    }
                }
                tm.registerTelephonyCallback(context.mainExecutor, callback)
                telephonyCallback = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                        if (signalStrength != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            onSignalLevel(signalStrength.level)
                        }
                    }
                }
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                telephonyCallback = listener
            }
        } catch (e: Exception) {
            // Some ROMs restrict telephony callbacks; the icon then stays at full bars as before
            e.printStackTrace()
        }
    }

    private fun stopSignalListener() {
        val tm = telephonyManager ?: return
        try {
            val callback = telephonyCallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && callback is TelephonyCallback) {
                tm.unregisterTelephonyCallback(callback)
            } else if (callback is PhoneStateListener) {
                @Suppress("DEPRECATION")
                tm.listen(callback, PhoneStateListener.LISTEN_NONE)
            }
        } catch (_: Exception) {}
        telephonyCallback = null
        telephonyManager = null
    }

    private fun onSignalLevel(level: Int) {
        val previous = currentState
        currentState = currentState.copy(signalLevel = level.coerceIn(0, 4))
        publishIfChanged(previous)
    }

    fun refreshState(context: Context, intent: Intent? = null) {
        var isCharging = currentState.isCharging
        var batteryLevel = currentState.batteryLevel

        var actualIntent = intent
        if (actualIntent == null) {
            try {
                actualIntent = androidx.core.content.ContextCompat.registerReceiver(
                    context,
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                )
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
        } else if (intent?.action == ACTION_WIFI_AP_STATE_CHANGED) {
            // The hidden isWifiApEnabled() call is blocked on Android 9+; the broadcast is not
            hotspotFromBroadcast =
                intent.getIntExtra(EXTRA_WIFI_AP_STATE, WIFI_AP_STATE_DISABLED) == WIFI_AP_STATE_ENABLED
        }

        currentState = HardwareState(
            isAirplaneActive = isAirplaneModeOn(context),
            isRingerSilentOrVibrate = isRingerSilentOrVibrate(context),
            isWifiConnected = isWifiConnected(context),
            isMobileDataConnected = isMobileDataConnected(context),
            isHotspotActive = hotspotFromBroadcast ?: isHotspotOn(context),
            isCharging = isCharging,
            batteryLevel = batteryLevel,
            signalLevel = currentState.signalLevel
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

    private companion object {
        // Hidden WifiManager constants, unchanged since Android 4
        const val ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        const val EXTRA_WIFI_AP_STATE = "wifi_state"
        const val WIFI_AP_STATE_DISABLED = 11
        const val WIFI_AP_STATE_ENABLED = 13
    }
}
