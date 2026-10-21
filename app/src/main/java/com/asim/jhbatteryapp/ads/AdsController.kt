package com.universal.remote.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.MainThread
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.asim.jhbatteryapp.ads.AdsConfig
import com.asim.jhbatteryapp.ads.RemoteConfigManager

/**
 * What one app tells this package about itself. Everything else in the package is the same in
 * every project.
 *
 * @param remoteConfigKey the Remote Config parameter holding the ads JSON (e.g. "MyAppAds").
 * @param defaultJson that parameter's default: every key the app uses, every flag 0, every unit
 *   blank, so a build that never reaches Remote Config shows no ads. Also the list of known keys.
 * @param isDebug the app's `BuildConfig.DEBUG`: test unit ids, no fetch throttle, consent debug.
 * @param noResumeAdOver simple class names of activities the resume app open ad never shows over
 *   (the splash shows its own).
 * @param allowResumeAd the app's own one-shot veto on the resume app open ad, checked on every
 *   return from background (e.g. back from a permission screen without granting it).
 */
class AdsSetup(
    val remoteConfigKey: String,
    val defaultJson: String,
    val isDebug: Boolean,
    val noResumeAdOver: Set<String> = setOf("SplashActivity", "WelcomeActivity"),
    val allowResumeAd: () -> Boolean = { true },
)

/**
 * **The one entry point screens use for ads.** Every placement is driven by Remote Config keys, so a
 * screen only says *where* an ad may go and *what happens after it*; whether an ad shows, which kind
 * and how often is the console's decision.
 *
 * ```
 * // Application.onCreate
 * AdsController.init(this, AdsSetup("MyAppAds", AppAds.DEFAULT_JSON, BuildConfig.DEBUG))
 *
 * // Splash: consent -> SDK -> config
 * AdsController.start(activity) { /* consent settled */ }
 *
 * // Inline slot, banner or native: home_top_banner / home_top_native, units admob_home_*_id
 * AdsController.slot(activity, binding.topAd, "home_top", units = "home")   // FrameLayout or AdSlotView
 * AdsController.forgetSlots("home")                       // onDestroy / onDestroyView
 *
 * // Interstitial on a button / tab / launch button
 * AdsController.interstitial(activity, "is_show_see_all_btn_inter") { navigate() }
 * AdsController.interstitial(activity, "is_show_home_btn_inter", Counter.BOTTOM_NAV) { navigate() }
 * AdsController.launchInterstitial(activity, "is_show_admob_language_done_btn_inter",
 *     unitKey = "admob_language_done_btn_inter_id") { next() }
 *
 * // Back out of an inner screen (back button and system back)
 * AdsController.routeSystemBack(fragment, binding.backButton)
 * binding.backButton.setOnClickListener {
 *     AdsController.back(fragment, "is_show_guide_back_inter") { findNavController().navigateUp() }
 * }
 *
 * // Rewarded / full-screen native
 * AdsController.rewarded(activity, "is_show_home_download_rewarded") { download() }
 * AdsController.fullScreenNative(activity, "is_show_admob_onboarding_full_screen_native",
 *     unitKey = "admob_onboarding_full_screen_native_id")
 * ```
 *
 * Splash full-screen ad: [SplashAds]. Unit ids: a placement's own unit key, falling back to the
 * shared `admob_app_*_id` when blank; debug builds always use Google's test units ([AdUnitIds]).
 */
object AdsController {

    /** The usual `counter_*` keys; any other key works the same (see [InterstitialClickGates]). */
    object Counter {
        /** Bottom-navigation tabs. */
        const val BOTTOM_NAV = "counter_bottom_nav"

        /** Buttons that open an inner screen (cards, See all, tiles…). */
        const val SCREEN_BUTTONS = "counter_screen_buttons"

        /** Back out of an inner screen. */
        const val BACK = "counter_back_btn"
    }

    /**
     * Shorter than [InterstitialAdManager]'s eight seconds: the launch buttons run between two
     * screens of a first session the user is trying to get through.
     */
    private const val LAUNCH_TIMEOUT_MS = 5_000L

    private var _setup: AdsSetup? = null

    internal val setup: AdsSetup
        get() = _setup ?: error("AdsController.init() must be called from Application.onCreate")

    internal val isDebug: Boolean get() = _setup?.isDebug == true

    /**
     * Call once from `Application.onCreate`. Registers the activity callbacks the package needs: the
     * resume app open ad on a real return from background, and [FullScreenAdState] forgetting an ad
     * whose activity was destroyed.
     */
    fun init(application: Application, setup: AdsSetup) {
        if (_setup != null) return
        _setup = setup
        application.registerActivityLifecycleCallbacks(LifecycleCallbacks())
    }

    // ------------------------------------------------------------------ bootstrap

    /** Starts consent -> Mobile Ads init, with the config fetch alongside. See [AdsBootstrap.start]. */
    @MainThread
    fun start(activity: Activity, onConsentSettled: () -> Unit = {}) = AdsBootstrap.start(activity, onConsentSettled)

    /** Runs [block] once consent, the SDK and the config are all ready. See [AdsBootstrap.whenReady]. */
    @MainThread
    fun whenReady(activity: Activity, block: (canRequestAds: Boolean) -> Unit) = AdsBootstrap.whenReady(activity, block)

    /** The ads config in hand: the fetched one, or the one an earlier session persisted, or the defaults. */
    fun config(): AdsConfig = RemoteConfigManager.current()

    /** A flag key is on (== 1). */
    fun isOn(flag: String): Boolean = config().isOn(flag)

    // ------------------------------------------------------------------ inline slots

    /**
     * Banner or native in [container] — an [AdSlotView] or any FrameLayout — from `<slotKey>_banner` /
     * `<slotKey>_native`; see [AdSlots]. [units] picks the unit ids `admob_<units>_banner_id` /
     * `admob_<units>_native_id` (null or blank: the shared `admob_app_*_id`).
     *
     * [isCurrent] narrows when an arriving ad may still go into this container (e.g. a sheet's
     * `{ dialog.isShowing }`; otherwise the ad is only cached), and [onResult] reports whether the
     * slot ended up with an ad.
     */
    fun slot(
        activity: Activity,
        container: FrameLayout,
        slotKey: String,
        units: String? = null,
        isCurrent: (() -> Boolean)? = null,
        onResult: (Boolean) -> Unit = {},
    ) = AdSlots.slot(activity, container, slotKey, units, isCurrent, onResult)

    /**
     * Whether [slotKey] would ask for an ad at all (a key on and its unit id not blank), answered now
     * from the config in hand — e.g. to skip waiting on a slot that is switched off.
     */
    fun isSlotOn(slotKey: String, units: String? = null): Boolean = AdSlots.mayShowAnAd(config(), slotKey, units)

    /** Drops the in-flight requests of every slot of [screen]; call where its views die. */
    fun forgetSlots(vararg screens: String) = screens.forEach(AdSlots::forget)

    // ------------------------------------------------------------------ full-screen

    /**
     * Interstitial on a tap, then [onDone]: shown when [flag] is 1 and the [counter] group allows it
     * (null: no counter). [unitKey] is the placement's own unit, falling back to `admob_app_inter_id`.
     */
    fun interstitial(
        activity: Activity,
        flag: String,
        counter: String? = Counter.SCREEN_BUTTONS,
        unitKey: String? = null,
        onDone: () -> Unit,
    ) = FullScreenAds.interstitial(activity, flag, counter, unitKey, timeoutMs = null, onDone)

    /** A launch-flow button (language Done, onboarding Get Started / Skip): no counter, a shorter wait. */
    fun launchInterstitial(activity: Activity, flag: String, unitKey: String? = null, onDone: () -> Unit) =
        FullScreenAds.interstitial(activity, flag, counter = null, unitKey, LAUNCH_TIMEOUT_MS, onDone)

    /** Whether [launchInterstitial] would show an ad right now, without starting one. */
    fun willShowLaunchInterstitial(flag: String, unitKey: String? = null): Boolean =
        FullScreenAds.willShowInterstitial(flag, unitKey)

    /** Back out of an inner screen: [action] (usually navigateUp) after the back interstitial. */
    fun back(fragment: Fragment, flag: String, action: () -> Unit) {
        val activity = fragment.activity ?: return
        FullScreenAds.interstitial(activity, flag, Counter.BACK, unitKey = null, timeoutMs = null) {
            if (fragment.isAdded && fragment.view != null) action()
        }
    }

    /** Back out of an activity: [action] (usually finish) after the back interstitial. */
    fun back(activity: Activity, flag: String, action: () -> Unit) =
        FullScreenAds.interstitial(activity, flag, Counter.BACK, unitKey = null, timeoutMs = null, action)

    /** System back does exactly what the screen's own back button does. */
    fun routeSystemBack(fragment: Fragment, backButton: View) {
        fragment.requireActivity().onBackPressedDispatcher.addCallback(
            fragment.viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backButton.performClick()
                }
            },
        )
    }

    /** Rewarded ad, then [onDone] whatever happened. [unitKey] falls back to `admob_app_rewarded_id`. */
    fun rewarded(activity: Activity, flag: String, unitKey: String? = null, onDone: () -> Unit) =
        FullScreenAds.rewarded(activity, flag, unitKey, onDone)

    /**
     * Full-screen native when [flag] is 1, then [onDone] (closed, failed or off). [unitKey] falls back
     * to `admob_app_native_id`.
     */
    fun fullScreenNative(activity: Activity, flag: String, unitKey: String? = null, onDone: () -> Unit = {}) =
        FullScreenAds.fullScreenNative(activity, flag, unitKey, onDone)

    // ------------------------------------------------------------------ misc

    /** The next return from background shows no app open ad — call right before leaving on purpose. */
    fun skipNextResume() = ResumeAppOpenManager.skipNextResume()

    /** A full-screen ad is on screen (or loading behind its dialog). */
    val isFullScreenAdShowing: Boolean get() = FullScreenAdState.isShowing

    /** Hides a slot while the keyboard is open (a form needs that space), and shows it again after. */
    fun hideWhileKeyboardOpen(screenRoot: View, slot: View) {
        ViewCompat.setOnApplyWindowInsetsListener(screenRoot) { _, insets ->
            val keyboardOpen = insets.isVisible(WindowInsetsCompat.Type.ime())
            slot.visibility = if (keyboardOpen) View.GONE else View.VISIBLE
            insets
        }
    }

    /**
     * Foreground detection for the resume app open ad: activities between onStart and onStop, and
     * 0 -> 1 is "the app came to the foreground" — unless the activity that took the count to 0
     * stopped for a configuration change (a language change recreates the whole stack, as do
     * rotation and day/night), which would otherwise show the ad to a user who never left.
     */
    private class LifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        private var startedActivities = 0
        private var stoppedForConfigChange = false

        override fun onActivityStarted(activity: Activity) {
            startedActivities++
            val wasRecreation = stoppedForConfigChange
            stoppedForConfigChange = false
            if (startedActivities == 1 && !wasRecreation) ResumeAppOpenManager.onAppForeground(activity)
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities = (startedActivities - 1).coerceAtLeast(0)
            if (startedActivities == 0) stoppedForConfigChange = activity.isChangingConfigurations
        }

        override fun onActivityResumed(activity: Activity) = ResumeAppOpenManager.onActivityResumed(activity)
        override fun onActivityDestroyed(activity: Activity) = FullScreenAdState.onActivityDestroyed(activity)
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }
}
