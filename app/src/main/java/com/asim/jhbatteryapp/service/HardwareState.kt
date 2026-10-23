package com.asim.jhbatteryapp.service

/**
 * Immutable snapshot of the phone's physical hardware and network states
 * that determine which custom indicators should be displayed on the status bar.
 */
data class HardwareState(
    val isAirplaneActive: Boolean = false,
    val isRingerSilentOrVibrate: Boolean = false,
    val isWifiConnected: Boolean = false,
    val isMobileDataConnected: Boolean = false,
    val isHotspotActive: Boolean = false,
    val isCharging: Boolean = false,
    val batteryLevel: Int = 58,
    /** Cellular signal bars 0..4 from the system, or -1 when unknown (no SIM / not reported yet). */
    val signalLevel: Int = -1
)
