package com.universal.remote.ads

import android.app.Activity
import android.util.Log
import com.asim.jhbatteryapp.ads.AdsConfig
import com.asim.jhbatteryapp.ads.RemoteConfigManager
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd

/**
 * The splash screen's full-screen ad: an app open ad, an interstitial, or one with the other as its
 * fallback, in the order `splash_ad_preference` asks for.
 *
 * ```
 * 0  SPLASH_AD_PREFERENCE_NONE            neither
 * 1  SPLASH_AD_PREFERENCE_APP_OPEN_FIRST  app open, and the interstitial only if it does not arrive
 * 2  SPLASH_AD_PREFERENCE_INTER_FIRST     the interstitial, and the app open ad only if it does not
 * ```
 *
 * "Fallback" means what it says: the second format is tried **only** when the first was enabled and
 * failed to load or show. A format whose own `is_show_*` flag is off is never tried, first or second,
 * and when both are off the whole thing resolves immediately.
 *
 * ## Why this cannot run at the same moment as the rest of the splash
 *
 * [AdsBootstrap] resolves consent, then initialises the Mobile Ads SDK, then fetches remote config.
 * The splash awaits only the consent step, because that is the one that needs an Activity alive. An
 * ad, though, needs step 2 (there is no SDK to ask before it) *and* step 3 (the flags and the unit id
 * live in the config), so a splash ad can only be requested at [AdsBootstrap.whenReady] - strictly
 * later than the point the splash is otherwise ready to route.
 *
 * [isWorthWaitingFor] is what keeps that from costing every launch. It reads the config already in
 * hand - the one the previous session persisted, or `DEFAULT_JSON` on a first run - and if neither
 * splash format is on, the splash does not wait for the bootstrap at all and routes exactly as it did
 * before this existed. Only a console that has actually asked for a splash ad pays the wait, and only
 * from the second launch, because the first launch has nothing persisted yet and so fails closed.
 *
 * Every wait is bounded and the user is never held behind an ad that is not there: the splash
 * screen caps its wait at [LOAD_BUDGET_MS].
 */
object SplashAds {

    private const val TAG = "SplashAds"

    /**
     * How long one format may spend loading in the load-and-show path ([show]). Both managers cancel
     * this deadline the moment the ad is about to be shown, so it bounds the *wait*, never the ad
     * itself - a user who is shown an ad still dismisses it in their own time.
     *
     * [load], which is what the splash screen actually uses, divides [LOAD_BUDGET_MS] between the
     * formats it will try instead of giving each of them this.
     */
    const val LOAD_TIMEOUT_MS = 5_000L

    /**
     * The whole time [load] may spend, however many formats the preference asks it to try - and so
     * the time the splash waits on it.
     *
     * It is divided between those formats rather than given to each: with `splash_ad_preference` 1 or
     * 2 and both `is_show_*` flags on, the chain is two deep, and five seconds each against a caller
     * that stopped waiting after six meant the fallback was never really tried - the splash had
     * already given up one second into it. One budget, split, keeps the fallback reachable without
     * the splash standing there for twice as long.
     */
    const val LOAD_BUDGET_MS = 6_000L

    /**
     * Whether the config currently in hand asks for a splash full-screen ad at all. Read before
     * waiting on [AdsBootstrap], so the ads-off path costs nothing.
     */
    fun isWorthWaitingFor(appOpenOnly: Boolean = false): Boolean {
        val config = RemoteConfigManager.current()
        val hasFormatOn = isAppOpenOn(config) || (!appOpenOnly && isInterstitialOn(config))
        return hasFormatOn || config.isOn("is_show_admob_splash_native")
    }

    /** A splash ad that has loaded and is waiting for the user to press Continue. */
    sealed interface Loaded {
        @JvmInline
        value class AppOpen(val ad: AppOpenAd) : Loaded

        @JvmInline
        value class Interstitial(val ad: InterstitialAd) : Loaded
    }

    /**
     * Loads - and deliberately does not show - the first format the preference asks for that
     * actually arrives, walking the same chain [show] does: the second format is tried only when
     * the first was enabled and failed.
     *
     * [onResult] runs exactly once, with the loaded ad or `null` when nothing is on, nothing
     * loaded, or the gap and ceiling say no. The returned function cancels whatever is in flight so
     * a destroyed activity is not held; after it runs [onResult] never fires.
     *
     * Splitting the load from the show is what lets the splash put a Continue button in front of
     * the ad instead of dropping one on the user unannounced. Nothing about the chain or the
     * preference's meaning changes: this is [showInOrder] with the show step deferred.
     *
     * [appOpenOnly] drops the interstitial from the chain, whatever the preference says.
     */
    fun load(
        activity: Activity,
        appOpenOnly: Boolean = false,
        onResult: (Loaded?) -> Unit,
    ): () -> Unit {
        val config = RemoteConfigManager.current()
        val order = orderFor(config).filter { !appOpenOnly || it == Format.APP_OPEN }

        if (order.isEmpty()) {
            Log.d(TAG, "No splash full-screen ad: preference=${config.splashAdPreference}")
            onResult(null)
            return {}
        }
        // The chain walks one format at a time, so the request that is actually in flight changes as
        // it goes. The canceller handed back reads this rather than closing over the first link's,
        // or cancelling would drop a request that had already finished and leave the live one
        // running against a destroyed activity.
        var inFlight: (() -> Unit)? = null
        var settled = false
        // Split, not per format: see [LOAD_BUDGET_MS].
        val perFormatTimeoutMs = LOAD_BUDGET_MS / order.size

        fun step(index: Int) {
            if (settled) return
            if (index >= order.size || activity.isFinishing) {
                settled = true
                inFlight = null
                onResult(null)
                return
            }
            val format = order[index]
            Log.d(TAG, "Loading splash ad ${index + 1}/${order.size}: $format")

            // load() can answer before it returns - a blank unit id does - and the canceller it hands
            // back is then already spent. Publishing it would overwrite the one the next link just
            // set, so it is only published when this link is still outstanding.
            var settledHere = false
            val cancelThis = format.load(activity, config, perFormatTimeoutMs) { loaded ->
                settledHere = true
                if (settled) return@load
                if (loaded != null) {
                    settled = true
                    inFlight = null
                    onResult(loaded)
                } else {
                    step(index + 1)
                }
            }
            if (!settledHere) inFlight = cancelThis
        }

        step(0)
        return {
            settled = true
            inFlight?.invoke()
            inFlight = null
        }
    }

    /**
     * Shows an ad [load] handed back, then runs [onDone] - dismissed or failed to show, the splash
     * moves on either way. [onDone] runs exactly once.
     *
     * [keepBackdrop] leaves the black backdrop up after an app open ad; pass false when the splash
     * stays on screen afterwards instead of routing straight on.
     */
    fun show(activity: Activity, loaded: Loaded, keepBackdrop: Boolean = true, onDone: () -> Unit) {
        when (loaded) {
            is Loaded.AppOpen ->
                AppOpenAdManager.show(activity, loaded.ad, { onDone() }, keepBackdrop = keepBackdrop)

            is Loaded.Interstitial ->
                InterstitialAdManager.show(activity, loaded.ad) { onDone() }
        }
    }

    private fun orderFor(config: AdsConfig): List<Format> {
        val pref = if (config.splashAdPreference == AdsConfig.SPLASH_AD_PREFERENCE_NONE) {
            when {
                isAppOpenOn(config) -> AdsConfig.SPLASH_AD_PREFERENCE_APP_OPEN_FIRST
                isInterstitialOn(config) -> AdsConfig.SPLASH_AD_PREFERENCE_INTER_FIRST
                else -> AdsConfig.SPLASH_AD_PREFERENCE_NONE
            }
        } else {
            config.splashAdPreference
        }
        return when (pref) {
            AdsConfig.SPLASH_AD_PREFERENCE_APP_OPEN_FIRST -> listOf(Format.APP_OPEN, Format.INTERSTITIAL)
            AdsConfig.SPLASH_AD_PREFERENCE_INTER_FIRST -> listOf(Format.INTERSTITIAL, Format.APP_OPEN)
            else -> emptyList()
        }.filter { it.isOn(config) }
    }

    /**
     * Shows whichever format the preference asks for, then runs [onDone]. [onDone] always runs
     * exactly once, on every path - no ad, one that failed, both that failed, or one dismissed.
     */
    fun show(activity: Activity, onDone: () -> Unit) {
        val config = RemoteConfigManager.current()
        val order = orderFor(config)

        if (order.isEmpty()) {
            Log.d(TAG, "No splash full-screen ad: preference=${config.splashAdPreference}")
            onDone()
            return
        }
        showInOrder(activity, config, order, 0, onDone)
    }

    private fun showInOrder(
        activity: Activity,
        config: AdsConfig,
        order: List<Format>,
        index: Int,
        onDone: () -> Unit
    ) {
        if (index >= order.size || activity.isFinishing) {
            onDone()
            return
        }
        val format = order[index]
        Log.d(TAG, "Splash ad ${index + 1}/${order.size}: $format")
        format.show(activity, config) { shown ->
            if (shown) onDone() else showInOrder(activity, config, order, index + 1, onDone)
        }
    }

    /** The splash's own interstitial unit, or the shared one when it is blank. */
    private fun splashInterUnit(config: AdsConfig) = config.splashInterId.ifBlank { config.appInterId }

    private fun isAppOpenOn(config: AdsConfig) =
        config.isShowSplashAppOpen == 1 && AdUnitIds.appOpen(config.splashAppOpenId).isNotBlank()

    private fun isInterstitialOn(config: AdsConfig) =
        config.isShowSplashInter == 1 && AdUnitIds.interstitial(splashInterUnit(config)).isNotBlank()

    private enum class Format {
        APP_OPEN {
            override fun isOn(config: AdsConfig) = isAppOpenOn(config)
            override fun show(activity: Activity, config: AdsConfig, onComplete: (Boolean) -> Unit) =
                AppOpenAdManager.loadAndShow(
                    activity = activity,
                    adUnitId = AdUnitIds.appOpen(config.splashAppOpenId),
                    isEnabled = true,
                    timeoutMs = LOAD_TIMEOUT_MS,
                    onComplete = onComplete,
                )

            override fun load(
                activity: Activity,
                config: AdsConfig,
                timeoutMs: Long,
                onResult: (Loaded?) -> Unit,
            ): () -> Unit = AppOpenAdManager.load(
                activity = activity,
                adUnitId = AdUnitIds.appOpen(config.splashAppOpenId),
                isEnabled = true,
                timeoutMs = timeoutMs,
            ) { ad -> onResult(ad?.let(Loaded::AppOpen)) }
        },
        INTERSTITIAL {
            override fun isOn(config: AdsConfig) = isInterstitialOn(config)
            override fun show(activity: Activity, config: AdsConfig, onComplete: (Boolean) -> Unit) =
                InterstitialAdManager.loadAndShow(
                    activity = activity,
                    adUnitId = AdUnitIds.interstitial(splashInterUnit(config)),
                    isEnabled = true,
                    timeoutMs = LOAD_TIMEOUT_MS,
                    onComplete = onComplete,
                )

            override fun load(
                activity: Activity,
                config: AdsConfig,
                timeoutMs: Long,
                onResult: (Loaded?) -> Unit,
            ): () -> Unit = InterstitialAdManager.load(
                activity = activity,
                adUnitId = AdUnitIds.interstitial(splashInterUnit(config)),
                isEnabled = true,
                timeoutMs = timeoutMs,
            ) { ad -> onResult(ad?.let(Loaded::Interstitial)) }
        };

        abstract fun isOn(config: AdsConfig): Boolean
        abstract fun show(activity: Activity, config: AdsConfig, onComplete: (Boolean) -> Unit)
        abstract fun load(
            activity: Activity,
            config: AdsConfig,
            timeoutMs: Long,
            onResult: (Loaded?) -> Unit,
        ): () -> Unit
    }
}
