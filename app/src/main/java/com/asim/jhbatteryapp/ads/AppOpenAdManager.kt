package com.universal.remote.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import com.asim.jhbatteryapp.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One-shot app open ad loader. Guarantees [onComplete] fires exactly once — with `shown = true`
 * after the ad was dismissed, or `false` if it was disabled, failed to load/show, or [timeoutMs]
 * passed first — so the caller never gets stuck and can fall back to another ad.
 */
object AppOpenAdManager {

    private const val DEFAULT_TIMEOUT_MS = 8000L

    fun loadAndShow(
        activity: Activity,
        adUnitId: String,
        isEnabled: Boolean,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        onComplete: (shown: Boolean) -> Unit
    ) {
        val placement = activity.javaClass.simpleName
        if (!isEnabled || adUnitId.isBlank()) {
            AdLogger.disabled(AdLogger.APP_OPEN, placement)
            onComplete(false)
            return
        }
        FullScreenAdState.begin(activity)

        val completed = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (completed.compareAndSet(false, true)) {
                FullScreenAdState.end()
                AdLogger.timeout(AdLogger.APP_OPEN, placement, timeoutMs)
                onComplete(false)
            }
        }
        mainHandler.postDelayed(timeoutRunnable, timeoutMs)

        fun finishOnce(shown: Boolean) {
            if (completed.compareAndSet(false, true)) {
                FullScreenAdState.end()
                mainHandler.removeCallbacks(timeoutRunnable)
                onComplete(shown)
            }
        }

        AdLogger.request(AdLogger.APP_OPEN, placement, adUnitId)
        AppOpenAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    AdLogger.loaded(AdLogger.APP_OPEN, placement)
                    if (completed.get() || activity.isFinishing) return

                    // The ad is about to be shown — cancel the load timeout so it can't fire
                    // mid-display and navigate away while the ad is still on screen. From here,
                    // completion depends solely on the ad actually being dismissed/failing to show.
                    mainHandler.removeCallbacks(timeoutRunnable)

                    val backdrop = addBlackBackdrop(activity)
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            // Starts the minimum gap before the next full-screen ad of any kind.
                            AdLogger.shown(AdLogger.APP_OPEN, placement)
                        }

                        override fun onAdImpression() = AdLogger.impression(AdLogger.APP_OPEN, placement)

                        override fun onAdClicked() {
                            AdLogger.clicked(AdLogger.APP_OPEN, placement)
                            ResumeAppOpenManager.skipNextResume()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            AdLogger.dismissed(AdLogger.APP_OPEN, placement)
                            finishOnce(true)
                            removeBlackBackdrop(activity, backdrop)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            AdLogger.showFailed(AdLogger.APP_OPEN, placement, error)
                            finishOnce(false)
                            removeBlackBackdrop(activity, backdrop)
                        }
                    }
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.failed(AdLogger.APP_OPEN, placement, error)
                    finishOnce(false)
                }
            }
        )
    }

    /**
     * Load-only variant: the ad is handed back instead of shown. [onResult] fires at most
     * once — with the loaded ad, or `null` if it was disabled, failed to load, or [timeoutMs] passed
     * first. The returned function cancels the request (drops the timeout and [onResult]) so a
     * destroyed activity isn't held; after it runs [onResult] never fires.
     */
    fun load(
        activity: Activity,
        adUnitId: String,
        isEnabled: Boolean,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        placement: String = activity.javaClass.simpleName,
        onResult: (AppOpenAd?) -> Unit
    ): () -> Unit {
        if (!isEnabled || adUnitId.isBlank()) {
            AdLogger.disabled(AdLogger.APP_OPEN, placement)
            onResult(null)
            return {}
        }

        val completed = AtomicBoolean(false)
        var callback: ((AppOpenAd?) -> Unit)? = onResult
        val mainHandler = Handler(Looper.getMainLooper())

        fun finishOnce(ad: AppOpenAd?) {
            if (completed.compareAndSet(false, true)) {
                val pending = callback
                callback = null
                pending?.invoke(ad)
            }
        }

        val timeoutRunnable = Runnable {
            if (!completed.get()) AdLogger.timeout(AdLogger.APP_OPEN, placement, timeoutMs)
            finishOnce(null)
        }
        mainHandler.postDelayed(timeoutRunnable, timeoutMs)

        AdLogger.request(AdLogger.APP_OPEN, placement, adUnitId)
        // Application context, so the SDK's pending callback doesn't keep the activity alive.
        AppOpenAd.load(
            activity.applicationContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    AdLogger.loaded(AdLogger.APP_OPEN, placement)
                    mainHandler.removeCallbacks(timeoutRunnable)
                    finishOnce(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.failed(AdLogger.APP_OPEN, placement, error)
                    mainHandler.removeCallbacks(timeoutRunnable)
                    finishOnce(null)
                }
            }
        )

        return {
            mainHandler.removeCallbacks(timeoutRunnable)
            completed.set(true)
            callback = null
        }
    }

    /**
     * Shows an ad returned by [load]. [onComplete] fires exactly once — `true` after the ad was
     * dismissed, `false` if it failed to show.
     */
    fun show(
        activity: Activity,
        ad: AppOpenAd,
        onComplete: (shown: Boolean) -> Unit,
        placement: String = activity.javaClass.simpleName,
        keepBackdrop: Boolean = false
    ) {
        val completed = AtomicBoolean(false)
        FullScreenAdState.begin(activity)
        var backdrop: View? = null

        fun finishOnce(shown: Boolean) {
            if (completed.compareAndSet(false, true)) {
                FullScreenAdState.end()
                onComplete(shown)
            }
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // Starts the minimum gap before the next full-screen ad of any kind.
                AdLogger.shown(AdLogger.APP_OPEN, placement)
            }

            override fun onAdImpression() = AdLogger.impression(AdLogger.APP_OPEN, placement)

            override fun onAdClicked() {
                AdLogger.clicked(AdLogger.APP_OPEN, placement)
                ResumeAppOpenManager.skipNextResume()
            }

            override fun onAdDismissedFullScreenContent() {
                AdLogger.dismissed(AdLogger.APP_OPEN, placement)
                finishOnce(true)
                if (!keepBackdrop) removeBlackBackdrop(activity, backdrop)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AdLogger.showFailed(AdLogger.APP_OPEN, placement, error)
                finishOnce(false)
                if (!keepBackdrop) removeBlackBackdrop(activity, backdrop)
            }
        }
        backdrop = addBlackBackdrop(activity)
        ad.show(activity)
    }

    /**
     * Full-screen opaque view on top of the activity's window, so nothing of the screen shows around
     * the (translucent) AdActivity's "Test Ad" label and "Continue to app" bar. Clickable so touches
     * can't reach the screen underneath. It lives in the window, so it goes away with the activity.
     */
    private fun addBlackBackdrop(activity: Activity): View? {
        val decor = activity.window?.decorView as? ViewGroup ?: return null
        val backdrop = View(activity).apply {
            setBackgroundColor(getColor(activity, R.color.ad_backdrop))
            isClickable = true
            isFocusable = true
            elevation = 10_000f
        }
        decor.addView(
            backdrop,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        return backdrop
    }

    /** Skipped on a finishing activity: the next screen then opens over the backdrop, with no flash. */
    private fun removeBlackBackdrop(activity: Activity, backdrop: View?) {
        if (backdrop == null || activity.isFinishing) return
        (backdrop.parent as? ViewGroup)?.removeView(backdrop)
    }
}
