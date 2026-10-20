package com.universal.remote.ads

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.asim.jhbatteryapp.ads.AdsConfig
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlin.math.roundToInt

/**
 * Loads a banner ad into [adContainer] in one of five modes (the slot's <screen>_<position>_banner
 * value), driving [shimmer] visibility while the request is in flight. The whole [slotRoot]
 * collapses to GONE if the slot is disabled or the ad fails to load, so it never leaves a blank gap.
 *
 * The slot is as wide as the host's include leaves it (banner slots are edge to edge), the ad is
 * centered in it and its height is fixed per mode before the request goes out (see [sizeSlot]), so
 * the slot never changes size when the ad arrives — [shimmer] fills it in every mode:
 * - [com.asim.jhbatteryapp.ads.AdsConfig.BANNER_MODE_STANDARD]: 50dp, fixed 320x50 ad.
 * - [com.asim.jhbatteryapp.ads.AdsConfig.BANNER_MODE_COLLAPSIBLE_TOP] / [com.asim.jhbatteryapp.ads.AdsConfig.BANNER_MODE_COLLAPSIBLE_BOTTOM]: the
 *   anchored adaptive size for the slot width; the ad arrives collapsed at that height and
 *   expands *over* the content (an overlay), so the slot keeps the collapsed height. The SDK
 *   itself stops the slot's later auto-refreshes from being collapsible again. Google demand only;
 *   mediated ads come back as normal banners.
 * - [com.asim.jhbatteryapp.ads.AdsConfig.BANNER_MODE_RECTANGLE]: 250dp, 300x250 ad centered.
 * - [com.asim.jhbatteryapp.ads.AdsConfig.BANNER_MODE_INLINE_ADAPTIVE]: requests the *inline* adaptive size at the full slot
 *   width and keeps the slot at [INLINE_ADAPTIVE_MAX_HEIGHT_DP] even when the ad comes back
 *   shorter.
 *
 * **Mode 1 and 4 need 320dp / 300dp of slot**: they are fixed creatives, centred without scaling.
 *
 * [screenKey] identifies the screen this slot belongs to. If a banner was already loaded for
 * that key (e.g. the user navigated away and came back), it's re-attached here instead of
 * issuing a new ad request — see [BannerAdCache].
 *
 * [isCurrent] is for a slot shared by several keys (Onboarding: one slot, a key per slide). When
 * it's given, the request's views are held in [pending] (not in the ad callbacks), so:
 * - a second call for a key whose request is still in flight only shows the shimmer and waits for
 *   that request (the latest call's views and [isCurrent] win) — no second request;
 * - an ad that arrives while [isCurrent] is false (the slot now shows another key) goes to
 *   [BannerAdCache] without touching the views, and a failure then leaves the slot alone;
 * - [forgetPending] drops the views (activity destroyed); a late ad is then only cached.
 * [onResult] still gets the load outcome in those cases. Without [isCurrent] nothing changes.
 */
object BannerAdLoader {

    /** Max height (dp) of the inline adaptive banner. To change the height, change only this value. */
    private const val INLINE_ADAPTIVE_MAX_HEIGHT_DP = 250

    /**
     * Views an in-flight request binds into; replaced by a later call for the same key. [activity] is
     * the one that built the request's [AdView] - holding it costs nothing, since [slotRoot] holds it
     * already, and it is what lets a later call tell its own in-flight request from an older
     * instance's.
     */
    private class Target(
        val activity: Activity,
        var slotRoot: View,
        var shimmer: ShimmerFrameLayout,
        var adContainer: FrameLayout,
        var isCurrent: () -> Boolean,
        var onResult: (Boolean) -> Unit
    )

    /** In-flight requests of callers that pass isCurrent, by screen key. */
    private val pending = mutableMapOf<String, Target>()

    fun load(
        activity: Activity,
        slotRoot: View,
        shimmer: ShimmerFrameLayout,
        adContainer: FrameLayout,
        adUnitId: String,
        mode: Int,
        screenKey: String,
        isCurrent: (() -> Boolean)? = null,
        onResult: (Boolean) -> Unit
    ) {
        if (mode == AdsConfig.BANNER_MODE_NONE || adUnitId.isBlank()) {
            AdLogger.disabled(AdLogger.BANNER, screenKey)
            slotRoot.visibility = View.GONE
            onResult(false)
            return
        }

        sizeSlot(activity, mode, slotRoot, shimmer, adContainer)

        // Scoped to this activity: a banner cached by an earlier instance is destroyed, not reused.
        val cachedAdView = BannerAdCache.get(screenKey, activity, mode)
        if (cachedAdView != null) {
            AdLogger.cacheHit(AdLogger.BANNER, screenKey)
            slotRoot.visibility = View.VISIBLE
            shimmer.stopShimmer()
            shimmer.visibility = View.GONE
            (cachedAdView.parent as? ViewGroup)?.removeView(cachedAdView)
            adContainer.removeAllViews()
            adContainer.addView(cachedAdView, centeredAdParams())
            adContainer.visibility = View.VISIBLE
            onResult(true)
            return
        }

        slotRoot.visibility = View.VISIBLE
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()
        adContainer.visibility = View.GONE

        // Adaptive sizing (inline and anchored) needs slotRoot's laid-out width; adContainer
        // itself stays GONE (zero width) until the ad loads, so it can't be used for measurement.
        // Wait a layout pass — sizeSlot then runs again with the real width too.
        if (needsSlotWidth(mode) && slotRoot.width == 0) {
            slotRoot.post {
                // The slot may show another key by now (fast swipe); that call has set it up.
                if (isCurrent?.invoke() == false) return@post
                load(activity, slotRoot, shimmer, adContainer, adUnitId, mode, screenKey, isCurrent, onResult)
            }
            return
        }

        val adSize = resolveAdSize(activity, slotRoot, mode)

        if (isCurrent != null) {
            val inFlight = pending[screenKey]
            // Only this activity's own request may be adopted. An AdView is built with the Activity it
            // routes a click and an expansion through, so one requested by an earlier instance - a
            // recreate while the request was still out - cannot be re-attached here: it would hand the
            // new screen a view holding a dead Activity. Dropped instead, which leaves the arriving ad
            // to BannerAdCache, and that refuses it for this activity and destroys it.
            if (inFlight != null && inFlight.activity !== activity) {
                pending.remove(screenKey)
                AdLogger.skipped(AdLogger.BANNER, screenKey, "in-flight request belongs to an older activity")
            } else if (inFlight != null) {
                inFlight.slotRoot = slotRoot
                inFlight.shimmer = shimmer
                inFlight.adContainer = adContainer
                inFlight.isCurrent = isCurrent
                inFlight.onResult = onResult
                AdLogger.skipped(AdLogger.BANNER, screenKey, "request in flight")
                return
            }
        }

        // Without isCurrent the callbacks keep these views, as before. With it they look the
        // views up in pending, so a forgotten (destroyed) caller's views aren't held or touched.
        var ownTarget = if (isCurrent == null) Target(activity, slotRoot, shimmer, adContainer, { true }, onResult) else null
        var pendingTarget = if (isCurrent != null) Target(activity, slotRoot, shimmer, adContainer, isCurrent, onResult) else null
        pendingTarget?.let { pending[screenKey] = it }
        // Ends the request: its target if still wanted (null once forgotten), removed from pending.
        // One-shot: the listener lives as long as the cached ad, so it must not keep these views.
        fun takeTarget(): Target? {
            val target = ownTarget
                ?: pending[screenKey]?.takeIf { it === pendingTarget }?.also { pending.remove(screenKey) }
            ownTarget = null
            pendingTarget = null
            return target
        }
        // The AdView auto-refreshes and calls the listener again each time; only the first
        // callback is this request's result. A refresh must not touch the views: the ad may be
        // re-attached to a newer slot by now (cache hit), and a failed refresh keeps the old creative.
        var settled = false

        val adView = AdView(activity)
        adView.adUnitId = adUnitId
        adView.setAdSize(adSize)
        adContainer.removeAllViews()
        adContainer.addView(adView, centeredAdParams())

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                AdLogger.loaded(
                    AdLogger.BANNER, screenKey,
                    adSize = "${adView.adSize?.width}x${adView.adSize?.height}dp " +
                        "collapsible=${adView.isCollapsible}"
                )
                BannerAdCache.put(screenKey, adView, activity, mode)
                if (settled) return
                settled = true
                val target = takeTarget()
                if (target == null || !target.isCurrent()) {
                    AdLogger.skipped(AdLogger.BANNER, screenKey, "slide changed, ad cached")
                    target?.onResult?.invoke(true)
                    return
                }
                // The screen is gone (AdSlotView took the AdView off when it left the window):
                // re-adding it would tie the cached AdView to the dead view tree again.
                if (!target.adContainer.isAttachedToWindow) {
                    AdLogger.skipped(AdLogger.BANNER, screenKey, "screen gone, ad cached")
                    target.onResult(true)
                    return
                }
                // Another key may have used the slot meanwhile and removed this view.
                if (adView.parent !== target.adContainer) {
                    (adView.parent as? ViewGroup)?.removeView(adView)
                    target.adContainer.removeAllViews()
                    target.adContainer.addView(adView, centeredAdParams())
                }
                target.shimmer.stopShimmer()
                target.shimmer.visibility = View.GONE
                target.adContainer.visibility = View.VISIBLE
                target.onResult(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                AdLogger.failed(AdLogger.BANNER, screenKey, error)
                if (settled) return
                settled = true
                val target = takeTarget() ?: return
                if (target.isCurrent()) {
                    target.shimmer.stopShimmer()
                    target.slotRoot.visibility = View.GONE
                }
                target.onResult(false)
            }

            override fun onAdImpression() = AdLogger.impression(AdLogger.BANNER, screenKey)

            override fun onAdClicked() {
                AdLogger.clicked(AdLogger.BANNER, screenKey)
                ResumeAppOpenManager.skipNextResume()
            }
        }
        AdLogger.request(AdLogger.BANNER, screenKey, adUnitId, adSize = describeSize(adSize, mode))

        val adRequest = AdRequest.Builder()
            .apply {
                // Google demand only; mediated ads just come back as normal banners.
                collapsiblePlacement(mode)?.let { placement ->
                    addNetworkExtrasBundle(
                        AdMobAdapter::class.java,
                        Bundle().apply { putString("collapsible", placement) }
                    )
                }
            }
            .build()
        adView.loadAd(adRequest)
    }

    /** Drops the views of these keys' in-flight requests (call from onDestroy); late ads are only cached. */
    fun forgetPending(screenKeys: Collection<String>) {
        screenKeys.forEach { pending.remove(it) }
    }

    /**
     * Fixes the slot's height for [mode] (see the class docs): [shimmer] and [adContainer] both
     * fill the slot's width at that height. The slot itself stays wrap_content, so it takes the
     * same height before and after the ad loads. A collapsible ad's expanded state is an overlay,
     * so its slot stays at the collapsed (anchored) height.
     *
     * Internal rather than private, so a slot that shows its shimmer before the bootstrap has
     * finished can size it first - see [AdSlots]. Calling it twice with the same mode is a
     * no-op, which is what the early call plus this loader's own call amounts to.
     */
    internal fun sizeSlot(activity: Activity, mode: Int, slotRoot: View, shimmer: ShimmerFrameLayout, adContainer: FrameLayout) {
        val heightDp = when (mode) {
            AdsConfig.BANNER_MODE_COLLAPSIBLE_TOP,
            AdsConfig.BANNER_MODE_COLLAPSIBLE_BOTTOM -> anchoredAdSize(activity, slotRoot).height
            AdsConfig.BANNER_MODE_RECTANGLE -> AdSize.MEDIUM_RECTANGLE.height
            AdsConfig.BANNER_MODE_INLINE_ADAPTIVE -> INLINE_ADAPTIVE_MAX_HEIGHT_DP
            else -> AdSize.BANNER.height
        }
        val heightPx = (heightDp * activity.resources.displayMetrics.density).roundToInt()
        listOf(shimmer, adContainer).forEach { view ->
            view.layoutParams = view.layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = heightPx
            }
        }
    }

    /** The AdView keeps its own size and sits in the middle of the fixed-height slot. */
    private fun centeredAdParams() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        Gravity.CENTER
    )

    private fun resolveAdSize(activity: Activity, slotRoot: View, mode: Int): AdSize = when (mode) {
        AdsConfig.BANNER_MODE_COLLAPSIBLE_TOP,
        AdsConfig.BANNER_MODE_COLLAPSIBLE_BOTTOM -> anchoredAdSize(activity, slotRoot)
        AdsConfig.BANNER_MODE_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
        AdsConfig.BANNER_MODE_INLINE_ADAPTIVE ->
            AdSize.getInlineAdaptiveBannerAdSize(slotWidthDp(activity, slotRoot), INLINE_ADAPTIVE_MAX_HEIGHT_DP)
        else -> AdSize.BANNER
    }

    /** The collapsed size of a collapsible banner: the anchored adaptive size for the slot width. */
    private fun anchoredAdSize(activity: Activity, slotRoot: View): AdSize =
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, slotWidthDp(activity, slotRoot))

    /** Slot width in dp, falling back to the display width before the slot is laid out. */
    private fun slotWidthDp(activity: Activity, slotRoot: View): Int {
        val displayMetrics: DisplayMetrics = activity.resources.displayMetrics
        val widthPixels = slotRoot.width.toFloat().takeIf { it > 0f } ?: displayMetrics.widthPixels.toFloat()
        return (widthPixels / displayMetrics.density).toInt()
    }

    /** Modes whose ad size is derived from the slot's width. */
    private fun needsSlotWidth(mode: Int): Boolean =
        mode == AdsConfig.BANNER_MODE_COLLAPSIBLE_TOP ||
            mode == AdsConfig.BANNER_MODE_COLLAPSIBLE_BOTTOM ||
            mode == AdsConfig.BANNER_MODE_INLINE_ADAPTIVE

    /** The "collapsible" network extra for [mode], or null for the non-collapsible modes. */
    private fun collapsiblePlacement(mode: Int): String? = when (mode) {
        AdsConfig.BANNER_MODE_COLLAPSIBLE_TOP -> "top"
        AdsConfig.BANNER_MODE_COLLAPSIBLE_BOTTOM -> "bottom"
        else -> null
    }

    private fun describeSize(adSize: AdSize, mode: Int): String = when (mode) {
        AdsConfig.BANNER_MODE_COLLAPSIBLE_TOP ->
            "${adSize.width}x${adSize.height}dp ANCHORED_COLLAPSIBLE_TOP"
        AdsConfig.BANNER_MODE_COLLAPSIBLE_BOTTOM ->
            "${adSize.width}x${adSize.height}dp ANCHORED_COLLAPSIBLE_BOTTOM"
        AdsConfig.BANNER_MODE_RECTANGLE -> "${adSize.width}x${adSize.height}dp RECTANGLE"
        // Inline adaptive AdSize reports height 0 until the ad loads, so log the max height instead.
        AdsConfig.BANNER_MODE_INLINE_ADAPTIVE ->
            "${adSize.width}x${INLINE_ADAPTIVE_MAX_HEIGHT_DP}dp(max) INLINE_ADAPTIVE"
        else -> "${adSize.width}x${adSize.height}dp STANDARD"
    }
}
