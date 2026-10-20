package com.universal.remote.ads

import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest

/**
 * Logs every ad format's lifecycle under one "AdmobAd" tag, so QA can follow request / load /
 * show / fail (including no-fill) with `adb logcat -s AdmobAd`. Kept in release builds on purpose.
 *
 * [placement] is the banner/native screen key, or the hosting activity's class name for
 * full-screen formats.
 */
object AdLogger {

    private const val TAG = "AdmobAd"

    const val BANNER = "BANNER"
    const val NATIVE = "NATIVE"
    const val FULL_SCREEN_NATIVE = "FULL_SCREEN_NATIVE"
    const val INTERSTITIAL = "INTERSTITIAL"
    const val APP_OPEN = "APP_OPEN"

    /** Placement for the app open ad shown on return from background (see ResumeAppOpenManager). */
    const val RESUME_APP_OPEN_PLACEMENT = "ResumeAppOpen"

    fun disabled(format: String, placement: String) =
        info(format, placement, "DISABLED", "(flag off or unit id blank)")

    fun cacheHit(format: String, placement: String) = info(format, placement, "CACHE_HIT")

    fun skipped(format: String, placement: String, reason: String) =
        info(format, placement, "SKIPPED", "($reason)")

    fun request(
        format: String,
        placement: String,
        adUnitId: String,
        layoutType: Int? = null,
        adSize: String? = null
    ) = info(
        format, placement, "REQUEST",
        "unit=$adUnitId" + (layoutType?.let { " layout=$it" } ?: "") + (adSize?.let { " size=$it" } ?: "")
    )

    fun loaded(format: String, placement: String, adSize: String? = null) =
        info(format, placement, "LOADED", adSize?.let { "size=$it" } ?: "")

    fun impression(format: String, placement: String) = info(format, placement, "IMPRESSION")

    fun clicked(format: String, placement: String) = info(format, placement, "CLICKED")

    fun shown(format: String, placement: String) = info(format, placement, "SHOWN")

    fun dismissed(format: String, placement: String) = info(format, placement, "DISMISSED")

    fun timeout(format: String, placement: String, timeoutMs: Long) =
        warn(format, placement, "TIMEOUT", "after ${timeoutMs}ms")

    fun failed(format: String, placement: String, error: AdError) =
        warn(format, placement, "FAILED", describe(error, loadErrorName(error.code)))

    fun showFailed(format: String, placement: String, error: AdError) =
        warn(format, placement, "SHOW_FAILED", describe(error, showErrorName(error.code)))

    private fun info(format: String, placement: String, status: String, details: String = "") {
        Log.i(TAG, line(format, placement, status, details))
    }

    private fun warn(format: String, placement: String, status: String, details: String) {
        Log.w(TAG, line(format, placement, status, details))
    }

    private fun line(format: String, placement: String, status: String, details: String): String =
        "[$format] $placement $status $details".trimEnd()

    private fun describe(error: AdError, codeName: String): String =
        "code=${error.code} $codeName msg=\"${error.message}\""

    private fun loadErrorName(code: Int): String = when (code) {
        AdRequest.ERROR_CODE_INTERNAL_ERROR -> "INTERNAL_ERROR"
        AdRequest.ERROR_CODE_INVALID_REQUEST -> "INVALID_REQUEST"
        AdRequest.ERROR_CODE_NETWORK_ERROR -> "NETWORK_ERROR"
        AdRequest.ERROR_CODE_NO_FILL -> "NO_FILL"
        AdRequest.ERROR_CODE_APP_ID_MISSING -> "APP_ID_MISSING"
        AdRequest.ERROR_CODE_MEDIATION_NO_FILL -> "MEDIATION_NO_FILL"
        AdRequest.ERROR_CODE_REQUEST_ID_MISMATCH -> "REQUEST_ID_MISMATCH"
        AdRequest.ERROR_CODE_INVALID_AD_STRING -> "INVALID_AD_STRING"
        else -> "UNKNOWN"
    }

    // Show errors use their own code space (FullScreenContentCallback), e.g. 3 is not NO_FILL here.
    private fun showErrorName(code: Int): String = when (code) {
        0 -> "INTERNAL_ERROR"
        1 -> "AD_REUSED"
        2 -> "NOT_READY"
        3 -> "APP_NOT_FOREGROUND"
        4 -> "MEDIATION_SHOW_ERROR"
        else -> "UNKNOWN"
    }
}
