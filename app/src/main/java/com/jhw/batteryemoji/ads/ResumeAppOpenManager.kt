package com.universal.remote.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.jhw.batteryemoji.ads.RemoteConfigManager
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * App open ad shown when the app comes back from background, gated by
 * `is_show_admob_resume_app_open`. The ad is preloaded and only shown if it's already loaded —
 * there is no loading dialog; if nothing is ready the return is skipped and a load starts.
 *
 * Driven by the application class's activity callbacks. Skipped on cold start, over the splash
 * screen or AdMob's own activity, while another full-screen ad is up ([FullScreenAdState]), and once
 * after [skipNextResume] (ad click, share, rate, external link).
 */
object ResumeAppOpenManager {

    private const val MAX_AD_AGE_MS = 4 * 3600_000L
    private const val LOAD_TIMEOUT_MS = 30_000L

    /** After a failed load (no fill / timeout), foreground and resume don't request again for this long. */
    private const val RETRY_AFTER_FAIL_MS = 60_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    private var ad: AppOpenAd? = null
    private var loadedAt = 0L
    private var lastFailAt = 0L
    private var isLoading = false
    private var isShowing = false
    private var skipNext = false
    private var firstForegroundSeen = false

    /** The next return from background shows no ad — call right before leaving the app on purpose. */
    fun skipNextResume() {
        skipNext = true
    }

    fun onAppForeground(activity: Activity) {
        val placement = AdLogger.RESUME_APP_OPEN_PLACEMENT
        if (!firstForegroundSeen) {
            firstForegroundSeen = true
            return
        }
        if (isSkipped(activity)) return
        // The app's own veto (AdsSetup.allowResumeAd), e.g. a return from a permission screen.
        if (!AdsController.setup.allowResumeAd()) return
        if (FullScreenAdState.isShowing || isShowing) {
            AdLogger.skipped(AdLogger.APP_OPEN, placement, "full-screen ad showing")
            return
        }
        if (skipNext) {
            skipNext = false
            AdLogger.skipped(AdLogger.APP_OPEN, placement, "returned from ad click / external app")
            return
        }
        if (!isEnabled()) return

        val loaded = ad
        if (loaded == null || System.currentTimeMillis() - loadedAt > MAX_AD_AGE_MS) {
            if (loaded != null) AdLogger.skipped(AdLogger.APP_OPEN, placement, "ad expired")
            else AdLogger.skipped(AdLogger.APP_OPEN, placement, "ad not loaded")
            ad = null
            load(activity)
            return
        }

        ad = null
        isShowing = true
        // Called from onActivityStarted, before the SDK's own callbacks see the app as foreground —
        // showing right here fails with APP_NOT_FOREGROUND, so wait until start/resume finished.
        mainHandler.post {
            if (activity.isFinishing || activity.isDestroyed) {
                isShowing = false
                ad = loaded
                return@post
            }
            AppOpenAdManager.show(activity, loaded, onComplete = {
                isShowing = false
                load(activity)
            }, placement = placement)
        }
    }

    fun onActivityResumed(activity: Activity) {
        // A real return runs onAppForeground (onStart) first and consumes the skip there. A resume
        // without going to background means the user never left (e.g. share chooser cancelled).
        // AdActivity is skipped: the SDK opens it as a trampoline on every ad click, before the browser.
        if (isSkipped(activity)) return
        skipNext = false
        load(activity)
    }

    private fun isSkipped(activity: Activity): Boolean =
        activity is AdActivity || activity.javaClass.simpleName in AdsController.setup.noResumeAdOver

    private fun isEnabled(): Boolean {
        val config = RemoteConfigManager.current()
        return config.isShowResumeAppOpen == 1 &&
            AdUnitIds.appOpen(config.resumeAppOpenId).isNotBlank() &&
            AdsInitializer.isInitialized
    }

    private fun load(activity: Activity) {
        if (!isEnabled() || isLoading || ad != null) return
        if (lastFailAt != 0L && SystemClock.elapsedRealtime() - lastFailAt < RETRY_AFTER_FAIL_MS) return
        isLoading = true
        AppOpenAdManager.load(
            activity = activity,
            adUnitId = AdUnitIds.appOpen(RemoteConfigManager.current().resumeAppOpenId),
            isEnabled = true,
            timeoutMs = LOAD_TIMEOUT_MS,
            placement = AdLogger.RESUME_APP_OPEN_PLACEMENT
        ) { loaded ->
            isLoading = false
            if (loaded != null) {
                ad = loaded
                loadedAt = System.currentTimeMillis()
                lastFailAt = 0L
            } else {
                lastFailAt = SystemClock.elapsedRealtime()
            }
        }
    }
}
