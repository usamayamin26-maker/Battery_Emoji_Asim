package com.universal.remote.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One-shot interstitial loader. [onComplete] fires exactly once — with `shown = true` after the ad
 * was dismissed, or `false` if it was disabled, failed to load/show, or [timeoutMs] passed first —
 * so a caller showing a loading dialog can always dismiss it, and a caller can fall back to
 * another ad.
 */
object InterstitialAdManager {

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
            AdLogger.disabled(AdLogger.INTERSTITIAL, placement)
            onComplete(false)
            return
        }
        FullScreenAdState.begin(activity)

        val completed = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (completed.compareAndSet(false, true)) {
                FullScreenAdState.end()
                AdLogger.timeout(AdLogger.INTERSTITIAL, placement, timeoutMs)
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

        AdLogger.request(AdLogger.INTERSTITIAL, placement, adUnitId)
        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    AdLogger.loaded(AdLogger.INTERSTITIAL, placement)
                    if (completed.get() || activity.isFinishing) return

                    // The ad is about to be shown — cancel the load timeout so it can't fire
                    // mid-display and navigate away while the ad is still on screen. From here,
                    // completion depends solely on the ad actually being dismissed/failing to show.
                    mainHandler.removeCallbacks(timeoutRunnable)

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            // Starts the minimum gap before the next full-screen ad of any kind.
                            AdLogger.shown(AdLogger.INTERSTITIAL, placement)
                        }

                        override fun onAdImpression() = AdLogger.impression(AdLogger.INTERSTITIAL, placement)

                        override fun onAdClicked() {
                            AdLogger.clicked(AdLogger.INTERSTITIAL, placement)
                            ResumeAppOpenManager.skipNextResume()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            AdLogger.dismissed(AdLogger.INTERSTITIAL, placement)
                            finishOnce(true)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            AdLogger.showFailed(AdLogger.INTERSTITIAL, placement, error)
                            finishOnce(false)
                        }
                    }
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.failed(AdLogger.INTERSTITIAL, placement, error)
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
        onResult: (InterstitialAd?) -> Unit
    ): () -> Unit {
        val placement = activity.javaClass.simpleName
        if (!isEnabled || adUnitId.isBlank()) {
            AdLogger.disabled(AdLogger.INTERSTITIAL, placement)
            onResult(null)
            return {}
        }

        val completed = AtomicBoolean(false)
        var callback: ((InterstitialAd?) -> Unit)? = onResult
        val mainHandler = Handler(Looper.getMainLooper())

        fun finishOnce(ad: InterstitialAd?) {
            if (completed.compareAndSet(false, true)) {
                val pending = callback
                callback = null
                pending?.invoke(ad)
            }
        }

        val timeoutRunnable = Runnable {
            if (!completed.get()) AdLogger.timeout(AdLogger.INTERSTITIAL, placement, timeoutMs)
            finishOnce(null)
        }
        mainHandler.postDelayed(timeoutRunnable, timeoutMs)

        AdLogger.request(AdLogger.INTERSTITIAL, placement, adUnitId)
        // Application context, so the SDK's pending callback doesn't keep the activity alive.
        InterstitialAd.load(
            activity.applicationContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    AdLogger.loaded(AdLogger.INTERSTITIAL, placement)
                    mainHandler.removeCallbacks(timeoutRunnable)
                    finishOnce(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.failed(AdLogger.INTERSTITIAL, placement, error)
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
    fun show(activity: Activity, ad: InterstitialAd, onComplete: (shown: Boolean) -> Unit) {
        val placement = activity.javaClass.simpleName
        val completed = AtomicBoolean(false)
        FullScreenAdState.begin(activity)

        fun finishOnce(shown: Boolean) {
            if (completed.compareAndSet(false, true)) {
                FullScreenAdState.end()
                onComplete(shown)
            }
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                // Starts the minimum gap before the next full-screen ad of any kind.
                AdLogger.shown(AdLogger.INTERSTITIAL, placement)
            }

            override fun onAdImpression() = AdLogger.impression(AdLogger.INTERSTITIAL, placement)

            override fun onAdClicked() {
                AdLogger.clicked(AdLogger.INTERSTITIAL, placement)
                ResumeAppOpenManager.skipNextResume()
            }

            override fun onAdDismissedFullScreenContent() {
                AdLogger.dismissed(AdLogger.INTERSTITIAL, placement)
                finishOnce(true)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AdLogger.showFailed(AdLogger.INTERSTITIAL, placement, error)
                finishOnce(false)
            }
        }
        ad.show(activity)
    }
}
