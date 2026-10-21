package com.universal.remote.ads

import android.app.Activity
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.asim.jhbatteryapp.ads.RemoteConfigManager

/**
 * The one entry point that puts the launch steps in the only order that is allowed:
 *
 * 1. [ConsentManager] — resolve consent (Google's User Messaging Platform), showing the form if the
 *    user is somewhere that requires one.
 * 2. [AdsInitializer] — initialise the Mobile Ads SDK, never before step 1.
 *
 * and, started at the same moment as step 1 rather than after step 2:
 *
 * - [com.asim.jhbatteryapp.ads.RemoteConfigManager] — fetch and activate, so [com.asim.jhbatteryapp.ads.AdsConfig] holds real values before any screen
 *   asks for a slot.
 *
 * The bootstrap is "ready" once both are done, and [whenReady] is the only way a slot gets loaded —
 * see [AdSlots]. That makes "no ad is requested before consent is settled" a property of the
 * code rather than of how long the splash screen happens to stay up.
 *
 * **Why the config fetch does not queue behind consent.** It is the app's own configuration, not an
 * ad request: nothing is requested off it until [canRequestAds] says so, and the two network calls
 * have nothing to say to each other. Queued third it arrived *after* the splash had already bound
 * its slots, so on a first run - the one launch with nothing persisted to fall back on - a slot had
 * to put up a placeholder without knowing which size the console had asked for. A 250dp banner
 * shimmered at 50dp and a medium native at the small size, then both resized the moment the fetch
 * landed, and only then filled with the ad: three states on screen where there should be two. Run
 * alongside consent it is normally in hand before the first slot is bound, so the placeholder is the
 * ad's size from the start.
 *
 * Every step's callback fires exactly once whatever happens — UMP reports its errors, the SDK's
 * initialisation callback always runs, and [com.asim.jhbatteryapp.ads.RemoteConfigManager.fetchAndActivate] is explicitly
 * documented to always complete. So the chain cannot stall on a failure, only on a user who leaves
 * the consent form open; see [start]'s caller for how the splash screen handles that.
 *
 * Not thread safe, and does not need to be: UMP, the Mobile Ads SDK and Firebase's Task listeners
 * all call back on the main thread, and both public functions are [MainThread].
 */
object AdsBootstrap {

    private const val TAG = "AdsBootstrap"

    private var started = false
    private var isConsentSettled = false

    /**
     * Whether both halves of the bootstrap are done - consent resolved, the SDK initialised and the
     * config fetched - so [com.asim.jhbatteryapp.ads.RemoteConfigManager.current] is the config this launch fetched rather than
     * whatever an earlier session persisted, or [com.asim.jhbatteryapp.ads.RemoteConfigManager]'s own defaults.
     */
    @Volatile
    var isReady = false
        private set

    /**
     * What UMP says about requesting ads. False until consent has actually been resolved, so a
     * caller that somehow runs early gets "no" rather than an optimistic yes.
     */
    @Volatile
    var canRequestAds = false
        private set

    /**
     * Callbacks waiting on the chain. Both are [ActivityScopedWaiters] rather than plain lists,
     * because a waiting callback closes over the Activity that registered it — and over its views,
     * for the slot callers — so a plain list keeps that Activity alive for as long as the chain
     * takes. On a dead network that is the whole remote config deadline; with a consent form left
     * open it is for ever. See [ActivityScopedWaiters].
     */
    private val consentWaiters = ActivityScopedWaiters<() -> Unit>()
    private val readyWaiters = ActivityScopedWaiters<(Boolean) -> Unit>()

    /**
     * Starts the chain, or joins one already running. [onConsentSettled] fires once consent is
     * resolved — which is the point the caller's activity is free to finish, because the consent
     * form, if there was one, has been dealt with. The SDK init and the config fetch carry on in the
     * background after it.
     */
    @MainThread
    fun start(activity: Activity, onConsentSettled: () -> Unit = {}) {
        if (isConsentSettled) onConsentSettled() else consentWaiters.add(activity, onConsentSettled)
        if (started) return
        started = true

        // Held instead of `activity`, so the callbacks below do not close over it: only consent
        // genuinely needs an Activity, and UMP holds that one itself while the form is up.
        val appContext = activity.applicationContext

        // Both halves report in here, and the later of the two is what makes the bootstrap ready.
        var configFetched = false
        var sdkInitialised = false
        fun markReadyWhenBothDone() {
            if (isReady || !configFetched || !sdkInitialised) return
            Log.d(TAG, "config fetched and SDK initialised, ads bootstrap ready")
            isReady = true
            readyWaiters.drain().forEach { it(canRequestAds) }
        }

        // Alongside consent, not behind it - see this object's own comment for why.
        Log.d(TAG, "fetching remote config")
        RemoteConfigManager.fetchAndActivate {
            Log.d(TAG, "remote config resolved")
            configFetched = true
            markReadyWhenBothDone()
        }

        Log.d(TAG, "1/2 gathering consent")
        ConsentManager.gatherConsent(activity) { consented ->
            canRequestAds = consented
            isConsentSettled = true
            Log.d(TAG, "1/2 consent settled, canRequestAds=$consented")
            consentWaiters.drain().forEach { it() }

            Log.d(TAG, "2/2 initialising the Mobile Ads SDK")
            AdsInitializer.initializeIfNeeded(appContext) {
                Log.d(TAG, "2/2 Mobile Ads SDK initialised")
                // Release cached native ads when the system asks for memory back.
                NativeAdCache.watchMemory(appContext)
                sdkInitialised = true
                markReadyWhenBothDone()
            }
        }
    }

    /**
     * Runs [block] once all three steps are done, with whether ads may be requested at all. If the
     * chain has not been started — the process was restored straight into an inner screen and the
     * splash screen never ran — this starts it, so a slot can never wait for something that is
     * never going to happen.
     *
     * A [block] registered by an Activity that is then destroyed is dropped, not invoked: its views
     * are gone, and holding it would hold the Activity.
     */
    @MainThread
    fun whenReady(activity: Activity, block: (canRequestAds: Boolean) -> Unit) {
        if (isReady) {
            block(canRequestAds)
            return
        }
        readyWaiters.add(activity, block)
        start(activity)
    }

    /**
     * Whether an ad may be requested *right now* - the bootstrap has finished and consent allows it.
     *
     * For a placement that cannot wait: an interstitial on a tap the user is standing behind, where
     * holding the app until consent, the SDK and the config fetch are all done would be worse than
     * skipping one impression. [whenReady] is still the rule for an inline slot, which can simply
     * shimmer until its ad arrives.
     *
     * A `false` also starts the chain, so the *next* tap finds it ready: a process restored straight
     * into an inner screen never ran the splash, and nothing else would have started it. That is the
     * case this closes - before it, those placements read [RemoteConfigManager.current], found the
     * config an earlier session had persisted (every flag as the console set it), and requested an ad
     * with consent unresolved and the SDK not initialised.
     */
    @MainThread
    fun mayRequestNow(activity: Activity): Boolean {
        if (isReady) return canRequestAds || AdsController.isDebug
        start(activity)
        return AdsController.isDebug
    }

    /**
     * [mayRequestNow] without the side effect, for a caller that is only *asking* whether a placement
     * would show an ad - see [AdsController.willShowLaunchInterstitial]. It must answer the same way, or the
     * caller's "no ad is coming" is a different answer from the one the placement will give.
     */
    fun mayRequestNow(): Boolean = (isReady && canRequestAds) || AdsController.isDebug

    /** Test seam: the object's state would otherwise carry between tests. */
    @VisibleForTesting
    internal fun resetForTest() {
        started = false
        isConsentSettled = false
        isReady = false
        canRequestAds = false
        consentWaiters.drain()
        readyWaiters.drain()
    }

    @VisibleForTesting
    internal fun waiterCounts(): Pair<Int, Int> = consentWaiters.size to readyWaiters.size
}
