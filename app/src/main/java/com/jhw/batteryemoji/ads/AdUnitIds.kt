package com.universal.remote.ads

import com.jhw.batteryemoji.ads.AdsConfig

/**
 * Resolves the ad unit ID to actually request: Google's official sample test IDs in debug
 * builds, the real remote-config-driven ID in release. Keeps real inventory from ever being
 * requested off a developer/test device, which AdMob treats as invalid traffic.
 *
 * Test IDs are Google's publicly documented constants (safe to hardcode, same for every app):
 * https://developers.google.com/admob/android/test-ads
 *
 * **Resolve before deciding whether a slot has an id.** A blank id means "this slot is off", and that
 * question has to be asked of the value these functions return, never of the raw config value. In a
 * release build the two are the same, so nothing changes; in a debug build a blank remote id still
 * resolves to a test id, which is what lets every format be exercised with the flags alone and no ad
 * unit ids filled in anywhere. Checking the raw value instead left the full-screen formats dead in
 * debug while banners and natives worked - the same config, two different answers.
 */
object AdUnitIds {

    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_BANNER_COLLAPSIBLE = "ca-app-pub-3940256099942544/2014213617"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    /**
     * [mode] is the slot's <screen>_<position>_banner value: the collapsible modes need Google's own
     * collapsible test unit in debug, since the plain banner test unit never serves one.
     */
    fun banner(remoteAdUnitId: String, mode: Int = AdsConfig.BANNER_MODE_STANDARD): String = when {
        !AdsController.isDebug -> remoteAdUnitId
        mode == AdsConfig.BANNER_MODE_COLLAPSIBLE_TOP ||
            mode == AdsConfig.BANNER_MODE_COLLAPSIBLE_BOTTOM -> TEST_BANNER_COLLAPSIBLE
        else -> TEST_BANNER
    }

    fun native(remoteAdUnitId: String): String =
        if (AdsController.isDebug) TEST_NATIVE else remoteAdUnitId

    fun interstitial(remoteAdUnitId: String): String =
        if (AdsController.isDebug) TEST_INTERSTITIAL else remoteAdUnitId

    fun appOpen(remoteAdUnitId: String): String =
        if (AdsController.isDebug) TEST_APP_OPEN else remoteAdUnitId

    fun rewarded(remoteAdUnitId: String): String =
        if (AdsController.isDebug) TEST_REWARDED else remoteAdUnitId
}
