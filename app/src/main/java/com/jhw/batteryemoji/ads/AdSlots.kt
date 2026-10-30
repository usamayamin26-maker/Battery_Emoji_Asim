package com.universal.remote.ads

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.ads.AdsConfig
import com.jhw.batteryemoji.ads.RemoteConfigManager
import com.facebook.shimmer.ShimmerFrameLayout

/**
 * Inline ad slots. A slot is any FrameLayout on a screen (top, mid or bottom) and shows **either a
 * banner or a native ad**, whichever the console asks for, from two keys:
 *
 * - `<slotKey>_banner` — 0 off, 1 standard 320x50, 2 collapsible top, 3 collapsible bottom,
 *   4 rectangle 300x250, 5 inline adaptive (AdsConfig.BANNER_MODE_*)
 * - `<slotKey>_native` — 0 off, 1..5 layouts (AdsConfig.NATIVE_LAYOUT_*)
 *
 * **Native wins** when both are on; both 0 collapses the slot. [slotKey] is `"<screen>_<position>"`
 * (e.g. "home_top", "home_mid", "home_bottom") and must be unique per slot — it is how a slot's
 * already-loaded ad is recognised and re-attached instead of reloaded when the user comes back. The
 * loaders cache under `"${slotKey}_banner"` / `"${slotKey}_native"`, so a slot switched from banner to
 * native in the console keeps both apart. See [BannerAdCache] / [NativeAdCache].
 *
 * The container is either
 * - an [AdSlotView] placed in the screen's XML: it already holds both parts (banner edge to edge,
 *   native with `app:nativeMarginHorizontal`), and the whole view is the slot that collapses; or
 * - any other FrameLayout: it gets one part at a time — layout_ad_slot_banner (edge to edge) or
 *   layout_ad_slot_native (with the gutter) — inflated on demand.
 * Either way a slot the console moves from banner to native, or one slot with a key per onboarding
 * slide, swaps the part in place.
 *
 * Everything waits on [AdsBootstrap]: consent is resolved, the SDK is initialised and remote config is
 * fetched before a single ad is requested, and the two keys are read after the fetch — which is also
 * when banner-or-native is decided. If consent says ads may not be requested, the slot collapses.
 */
internal object AdSlots {

    /** The three views a loader binds into: the part's root (collapsed by the loader), its shimmer and container. */
    private class SlotViews(val root: View, val shimmer: ShimmerFrameLayout, val container: FrameLayout)

    /** Positions a screen's slots can have; [forget] clears all of them. */
    private val POSITIONS = listOf("top", "mid", "center", "bottom")

    /** See [AdsController.slot]. */
    fun slot(
        activity: Activity,
        container: FrameLayout,
        slotKey: String,
        units: String?,
        isCurrent: (() -> Boolean)?,
        onResult: (Boolean) -> Unit,
    ) {
        val config = RemoteConfigManager.current()
        val allAdsOn = config.allAdsOn == 1
        val layoutNow = if (allAdsOn) config.int("${slotKey}_native") else AdsConfig.NATIVE_LAYOUT_NONE
        val modeNow = if (allAdsOn) config.int("${slotKey}_banner") else AdsConfig.BANNER_MODE_NONE
        val slotIsOn = mayShowAnAd(config, slotKey, units)

        if (allAdsOn && container !is AdSlotView) {
            container.visibility = View.VISIBLE
        }

        when {
            layoutNow > AdsConfig.NATIVE_LAYOUT_NONE -> {
                val views = nativeViews(activity, container)
                NativeAdLoader.installSkeleton(activity, views.shimmer, layoutNow)
                showShimmerEarly(views, slotIsOn)
            }
            modeNow > AdsConfig.BANNER_MODE_NONE -> {
                val views = bannerViews(activity, container)
                // Sized before the shimmer goes up: the include ships at 50dp, and a 250dp slot
                // sized only inside the loader showed a thin placeholder that then grew.
                BannerAdLoader.sizeSlot(activity, modeNow, views.root, views.shimmer, views.container)
                showShimmerEarly(views, slotIsOn)
            }
            // An AdSlotView is visible in XML: with both keys 0 it goes now, so no placeholder flashes.
            container is AdSlotView -> container.hide()
            else -> container.visibility = View.GONE
        }

        val claimed = claimSlot(container, slotKey)
        val stillCurrent = if (isCurrent == null) claimed else ({ claimed() && isCurrent() })

        AdsBootstrap.whenReady(activity) { canRequestAds ->
            if (activity.isFinishing) return@whenReady

            // Read after the fetch, never before: this is the whole point of waiting.
            val fetched = RemoteConfigManager.current()
            val fetchedAllAdsOn = fetched.allAdsOn == 1
            val layout = if (fetchedAllAdsOn) fetched.int("${slotKey}_native") else AdsConfig.NATIVE_LAYOUT_NONE
            val mode = if (fetchedAllAdsOn) fetched.int("${slotKey}_banner") else AdsConfig.BANNER_MODE_NONE
            when {
                canRequestAds && layout > AdsConfig.NATIVE_LAYOUT_NONE -> {
                    val views = nativeViews(activity, container)
                    NativeAdLoader.load(
                        context = activity,
                        slotRoot = views.root,
                        shimmer = views.shimmer,
                        adContainer = views.container,
                        adUnitId = AdUnitIds.native(fetched.nativeUnit(units)),
                        layoutType = layout,
                        screenKey = "${slotKey}_native",
                        isCurrent = stillCurrent,
                        onResult = onResult,
                    )
                }

                canRequestAds && mode > AdsConfig.BANNER_MODE_NONE -> {
                    val views = bannerViews(activity, container)
                    BannerAdLoader.load(
                        activity = activity,
                        slotRoot = views.root,
                        shimmer = views.shimmer,
                        adContainer = views.container,
                        adUnitId = AdUnitIds.banner(fetched.bannerUnit(units), mode),
                        mode = mode,
                        screenKey = "${slotKey}_banner",
                        isCurrent = stillCurrent,
                        onResult = onResult,
                    )
                }

                // Both keys 0 (or no consent or all_ads_on == 0): no loader call, the slot just collapses.
                else -> {
                    if (stillCurrent()) {
                        hideParts(container)
                        if (container !is AdSlotView) container.visibility = View.GONE
                    }
                    onResult(false)
                }
            }
        }
    }

    /**
     * Drops the views of [screen]'s in-flight requests, every slot of it ([POSITIONS]) and both
     * types. A late ad is then only cached; nothing is written into views that are gone.
     */
    fun forget(screen: String) {
        val slotKeys = POSITIONS.map { "${screen}_$it" }
        BannerAdLoader.forgetPending(slotKeys.map { "${it}_banner" })
        NativeAdLoader.forgetPending(slotKeys.map { "${it}_native" })
    }

    /**
     * Whether this slot would even ask for an ad, answered from the config in hand. The same tests
     * the loaders open with, so this never answers yes to a value they call "off".
     */
    fun mayShowAnAd(config: AdsConfig, slotKey: String, units: String?): Boolean {
        if (config.allAdsOn == 0) return false
        val mode = config.int("${slotKey}_banner")
        val banner = mode > AdsConfig.BANNER_MODE_NONE &&
            AdUnitIds.banner(config.bannerUnit(units), mode).isNotBlank()
        val native = config.int("${slotKey}_native") > AdsConfig.NATIVE_LAYOUT_NONE &&
            AdUnitIds.native(config.nativeUnit(units)).isNotBlank()
        return banner || native
    }

    /** The banner part of [container], inflated (and the native part removed) if it isn't there. */
    private fun bannerViews(activity: Activity, container: FrameLayout): SlotViews {
        if (container is AdSlotView) {
            container.showBanner()
            return SlotViews(container, container.bannerShimmer, container.bannerContainer)
        }
        val root = container.findViewById<View>(R.id.adSlotBannerRoot)
            ?: run {
                container.removeAllViews()
                LayoutInflater.from(activity).inflate(R.layout.layout_ad_slot_banner, container, false)
                    .also { container.addView(it) }
            }
        return SlotViews(root, root.findViewById(R.id.shimmerBannerAd), root.findViewById(R.id.containerBannerAd))
    }

    /** The native part of [container], inflated (and the banner part removed) if it isn't there. */
    private fun nativeViews(activity: Activity, container: FrameLayout): SlotViews {
        if (container is AdSlotView) {
            container.showNative()
            return SlotViews(container, container.nativeShimmer, container.nativeContainer)
        }
        val root = container.findViewById<View>(R.id.adSlotNativeRoot)
            ?: run {
                container.removeAllViews()
                LayoutInflater.from(activity).inflate(R.layout.layout_ad_slot_native, container, false)
                    .also { container.addView(it) }
            }
        return SlotViews(root, root.findViewById(R.id.shimmerNativeAd), root.findViewById(R.id.containerNativeAd))
    }

    /** Both keys off: whichever part is in the container collapses. */
    private fun hideParts(container: FrameLayout) {
        if (container is AdSlotView) return container.hide()
        for (i in 0 until container.childCount) {
            val part = container.getChildAt(i)
            part.findViewById<ShimmerFrameLayout>(R.id.shimmerBannerAd)?.stopShimmer()
            part.findViewById<ShimmerFrameLayout>(R.id.shimmerNativeAd)?.stopShimmer()
            part.visibility = View.GONE
        }
    }

    /**
     * The loaders only show the shimmer once the bootstrap is ready, which left the slot blank for a
     * second or two. If the config already in hand (this launch's, or the one Firebase persisted last
     * time) says the slot is on, the shimmer goes up straight away; the loader then swaps in the ad,
     * or hides the slot if it turns out to be off.
     *
     * A slot whose size is not known yet shows nothing: on a first run nothing is persisted, so every
     * key reads 0 until the fetch lands, and a placeholder put up anyway would resize as it landed.
     */
    private fun showShimmerEarly(views: SlotViews, slotIsOn: Boolean) {
        // Never before consent: until the user has answered the consent form, no slot shows anything.
        if (!AdsBootstrap.canRequestAds) return
        if (!slotIsOn) return
        if (views.root.visibility == View.VISIBLE) return
        views.root.visibility = View.VISIBLE
        views.shimmer.visibility = View.VISIBLE
        views.shimmer.startShimmer()
    }

    /**
     * Marks the container as showing [slotKey] and hands back the loaders' `isCurrent` for it, which
     * is what puts a request in the loaders' `pending` map (so [forget] has something to drop) and
     * lets a container shared by several keys — onboarding's one slot with a key per slide —
     * recognise that an arriving ad is no longer the one on screen; that ad is then only cached.
     */
    private fun claimSlot(container: View, slotKey: String): () -> Boolean {
        container.tag = slotKey
        return { container.tag == slotKey }
    }
}
