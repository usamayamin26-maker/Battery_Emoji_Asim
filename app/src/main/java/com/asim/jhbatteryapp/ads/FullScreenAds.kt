package com.universal.remote.ads

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.asim.jhbatteryapp.ads.RemoteConfigManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Full-screen ads on a user action: interstitial, rewarded and full-screen native. Reached through
 * [AdsController]; see there for what each placement means.
 *
 * One interstitial path serves every placement — a tab, a button that opens a screen, back, a launch
 * button — and they differ only in their arguments: the `is_show_*` flag, which `counter_*` it draws
 * on (none for the launch buttons, which fire at most once a session), which unit, and how long the
 * user may wait behind the loading dialog.
 *
 * `onDone` always runs exactly once — immediately when the flag is off, the counter says no, the unit
 * is blank or the bootstrap is not ready, otherwise once the ad is dismissed, fails or times out — on
 * every path but an activity that is gone. While one ad is in flight further taps are ignored, so a
 * double tap cannot stack two.
 */
internal object FullScreenAds {

    private var interstitialInProgress = false
    private var rewardedInProgress = false

    /** Whether [interstitial] would put an ad on screen right now, without starting one. */
    fun willShowInterstitial(flag: String, unitKey: String?): Boolean {
        if (interstitialInProgress) return false
        if (!AdsBootstrap.mayRequestNow()) return false
        val config = RemoteConfigManager.current()
        return config.isOn(flag) && AdUnitIds.interstitial(config.unit(unitKey, config.appInterId)).isNotBlank()
    }

    fun interstitial(
        activity: Activity,
        flag: String,
        counter: String?,
        unitKey: String?,
        timeoutMs: Long?,
        onDone: () -> Unit,
    ) {
        if (interstitialInProgress) return
        // Not [AdsBootstrap.whenReady]: this runs on a tap the user is waiting behind, so a bootstrap
        // that has not finished means no ad rather than a wait. It also keeps the click counter from
        // being spent on a tap that could never have shown one.
        if (!AdsBootstrap.mayRequestNow(activity)) {
            onDone()
            return
        }

        val config = RemoteConfigManager.current()
        val adUnitId = AdUnitIds.interstitial(config.unit(unitKey, config.appInterId))
        if (!config.isOn(flag) || adUnitId.isBlank() ||
            (counter != null && !InterstitialClickGates.shouldShow(config, counter))
        ) {
            onDone()
            return
        }

        interstitialInProgress = true
        val dialog = AdLoadingDialog.show(activity)
        // A dialog belongs to its activity's window: if the activity goes while the ad loads, the
        // dialog goes with it and the next tap is not blocked by a flag nobody clears.
        val whileAlive = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                interstitialInProgress = false
                dialog.dismissSafely()
            }
        }
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        lifecycle?.addObserver(whileAlive)

        val finish: (Boolean) -> Unit = {
            interstitialInProgress = false
            lifecycle?.removeObserver(whileAlive)
            dialog.dismissSafely()
            if (!activity.isFinishing && !activity.isDestroyed) onDone()
        }
        if (timeoutMs != null) {
            InterstitialAdManager.loadAndShow(activity, adUnitId, isEnabled = true, timeoutMs = timeoutMs, onComplete = finish)
        } else {
            InterstitialAdManager.loadAndShow(activity, adUnitId, isEnabled = true, onComplete = finish)
        }
    }

    /** Rewarded ad, then [onDone] — watched, skipped, failed or switched off, the action continues. */
    fun rewarded(activity: Activity, flag: String, unitKey: String?, onDone: () -> Unit) {
        if (rewardedInProgress) return
        val config = RemoteConfigManager.current()
        val unitId = AdUnitIds.rewarded(config.unit(unitKey, config.appRewardedId))
        if (!config.isOn(flag) || unitId.isBlank()) {
            onDone()
            return
        }
        rewardedInProgress = true
        val dialog = AdLoadingDialog.show(activity)
        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            rewardedInProgress = false
            dialog.dismissSafely()
            FullScreenAdState.end()
            if (!activity.isFinishing) onDone()
        }

        AdsBootstrap.whenReady(activity) { canRequestAds ->
            if (!canRequestAds || activity.isFinishing) {
                finish()
                return@whenReady
            }
            FullScreenAdState.begin(activity)
            RewardedAd.load(activity, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (finished || activity.isFinishing) {
                        finish()
                        return
                    }
                    dialog.dismissSafely()
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() = finish()
                        override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
                    }
                    ad.show(activity) { /* reward earned — the action itself is the reward */ }
                }

                override fun onAdFailedToLoad(error: LoadAdError) = finish()
            })
        }
    }

    /** Full-screen native (no click counter), then [onDone] — closed, failed, off or not ready. */
    fun fullScreenNative(activity: Activity, flag: String, unitKey: String?, onDone: () -> Unit) {
        if (!RemoteConfigManager.current().isOn(flag) || !AdsBootstrap.mayRequestNow(activity)) {
            onDone()
            return
        }
        val config = RemoteConfigManager.current()
        FullScreenNativeAdLoader.loadAndShow(
            activity = activity,
            adUnitId = AdUnitIds.native(config.unit(unitKey, config.appNativeId)),
            isEnabled = true,
        ) {
            if (!activity.isFinishing && !activity.isDestroyed) onDone()
        }
    }
}
