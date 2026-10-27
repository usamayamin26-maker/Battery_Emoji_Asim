package com.universal.remote.ads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.ads.AdsConfig
import com.jhw.batteryemoji.ads.RemoteConfigManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Loads an inline native ad into [adContainer] using the layout picked by [load]'s `layoutType`
 * (1..5, [com.jhw.batteryemoji.ads.AdsConfig.NATIVE_LAYOUT_MEDIUM_WITHOUT_MEDIA_RIGHT_CTA] … [com.jhw.batteryemoji.ads.AdsConfig.NATIVE_LAYOUT_LARGE_TOP_MEDIA_BOTTOM_CTA];
 * unknown values fall back to layout 1), driving [shimmer] visibility with a matching-size skeleton.
 * Collapses [slotRoot] to GONE when disabled ([com.jhw.batteryemoji.ads.AdsConfig.NATIVE_LAYOUT_NONE]) or on failure.
 *
 * [screenKey] identifies the screen this slot belongs to. If a native ad was already loaded for
 * that key (e.g. the user navigated away and came back), its assets are rebound into a fresh
 * view here instead of issuing a new ad request — see [NativeAdCache].
 *
 * [isCurrent] works as in [BannerAdLoader]: for a slot shared by several keys, an in-flight key
 * isn't requested again, an ad that arrives for a key the slot no longer shows is only cached,
 * and [forgetPending] drops a destroyed caller's views.
 */
object NativeAdLoader {

    /** Views an in-flight request binds into; replaced by a later call for the same key. */
    private class Target(
        var slotRoot: View,
        var shimmer: ShimmerFrameLayout,
        var adContainer: FrameLayout,
        var isCurrent: () -> Boolean,
        var onResult: (Boolean) -> Unit
    )

    /** In-flight requests of callers that pass isCurrent, by screen key. */
    private val pending = mutableMapOf<String, Target>()

    fun load(
        context: Context,
        slotRoot: View,
        shimmer: ShimmerFrameLayout,
        adContainer: FrameLayout,
        adUnitId: String,
        layoutType: Int,
        screenKey: String,
        isCurrent: (() -> Boolean)? = null,
        onResult: (Boolean) -> Unit
    ) {
        if (layoutType <= AdsConfig.NATIVE_LAYOUT_NONE || adUnitId.isBlank()) {
            AdLogger.disabled(AdLogger.NATIVE, screenKey)
            slotRoot.visibility = View.GONE
            onResult(false)
            return
        }

        val cachedNativeAd = NativeAdCache.get(screenKey)
        if (cachedNativeAd != null) {
            AdLogger.cacheHit(AdLogger.NATIVE, screenKey)
            slotRoot.visibility = View.VISIBLE
            shimmer.stopShimmer()
            shimmer.visibility = View.GONE

            val adView = LayoutInflater.from(context)
                .inflate(adLayoutRes(layoutType), adContainer, false) as NativeAdView
            bind(adView, cachedNativeAd)

            adContainer.removeAllViews()
            adContainer.addView(adView)
            adContainer.visibility = View.VISIBLE
            onResult(true)
            return
        }

        // The slot is already the ad's exact size while loading. A shared slot can hold another
        // layout's skeleton from another key, so layout 1 gets its own back explicitly.
        installSkeleton(context, shimmer, layoutType)

        slotRoot.visibility = View.VISIBLE
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()
        adContainer.visibility = View.GONE

        if (isCurrent != null) {
            val inFlight = pending[screenKey]
            if (inFlight != null) {
                inFlight.slotRoot = slotRoot
                inFlight.shimmer = shimmer
                inFlight.adContainer = adContainer
                inFlight.isCurrent = isCurrent
                inFlight.onResult = onResult
                AdLogger.skipped(AdLogger.NATIVE, screenKey, "request in flight")
                return
            }
        }

        // Without isCurrent the callbacks keep these views, as before. With it they look the
        // views up in pending, so a forgotten (destroyed) caller's views aren't held or touched.
        var ownTarget = if (isCurrent == null) Target(slotRoot, shimmer, adContainer, { true }, onResult) else null
        var pendingTarget = if (isCurrent != null) Target(slotRoot, shimmer, adContainer, isCurrent, onResult) else null
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

        // Application context on purpose: the loaded ad is cached process-wide, so it must not be
        // able to hold the Activity. The ad's *views* are still inflated from `context` below, which
        // is what needs the themed resources and what a click routes through.
        val adLoader = AdLoader.Builder(context.applicationContext, adUnitId)
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(adChoicesPlacement(layoutType))
                    .build()
            )
            .forNativeAd { nativeAd ->
                AdLogger.loaded(AdLogger.NATIVE, screenKey)
                NativeAdCache.put(screenKey, nativeAd)
                val target = takeTarget()
                if (target == null || !target.isCurrent()) {
                    AdLogger.skipped(AdLogger.NATIVE, screenKey, "slide changed, ad cached")
                    target?.onResult?.invoke(true)
                    return@forNativeAd
                }
                target.shimmer.stopShimmer()
                target.shimmer.visibility = View.GONE

                // The target's own context, not the requesting one: a request adopted by a second
                // call (a recreated screen) must inflate from the container that is on screen now,
                // or the ad's views come off an Activity that is going away.
                val adView = LayoutInflater.from(target.adContainer.context)
                    .inflate(adLayoutRes(layoutType), target.adContainer, false) as NativeAdView
                bind(adView, nativeAd)

                target.adContainer.removeAllViews()
                target.adContainer.addView(adView)
                target.adContainer.visibility = View.VISIBLE
                target.onResult(true)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.failed(AdLogger.NATIVE, screenKey, error)
                    val target = takeTarget() ?: return
                    if (target.isCurrent()) {
                        target.shimmer.stopShimmer()
                        target.slotRoot.visibility = View.GONE
                    }
                    target.onResult(false)
                }

                override fun onAdImpression() = AdLogger.impression(AdLogger.NATIVE, screenKey)

                override fun onAdClicked() {
                    AdLogger.clicked(AdLogger.NATIVE, screenKey)
                    ResumeAppOpenManager.skipNextResume()
                }
            })
            .build()

        AdLogger.request(AdLogger.NATIVE, screenKey, adUnitId, layoutType)
        adLoader.loadAd(AdRequest.Builder().build())
    }

    /** Drops the views of these keys' in-flight requests (call from onDestroy); late ads are only cached. */
    fun forgetPending(screenKeys: Collection<String>) {
        screenKeys.forEach { pending.remove(it) }
    }

    /**
     * Puts the skeleton matching [layoutType] inside [shimmer], and does nothing when the right one is
     * already there.
     *
     * The slot include ships holding the layout 1 skeleton, so "no tag" means layout 1; the tag then
     * records whichever was swapped in after it. Internal because [AdSlots] calls it before it starts
     * the shimmer: a taller layout otherwise shimmered at the layout 1 size, resized when this loader
     * finally ran, and only then filled with the ad.
     */
    internal fun installSkeleton(context: Context, shimmer: ShimmerFrameLayout, layoutType: Int) {
        val wanted = skeletonLayoutRes(layoutType)
        if (wanted == shimmer.tag as? Int) return
        shimmer.removeAllViews()
        LayoutInflater.from(context)
            .inflate(wanted ?: R.layout.layout_native_ad_skeleton_medium_without_media_right_cta, shimmer, true)
        shimmer.tag = wanted
    }

    private fun bind(adView: NativeAdView, nativeAd: NativeAd) {
        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
        val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)

        headlineView.text = nativeAd.headline
        adView.headlineView = headlineView

        val body = nativeAd.body
        if (body.isNullOrBlank()) {
            bodyView.visibility = View.GONE
        } else {
            bodyView.visibility = View.VISIBLE
            bodyView.text = body
        }
        adView.bodyView = bodyView

        val icon = nativeAd.icon
        if (icon == null) {
            iconView.visibility = View.GONE
        } else {
            iconView.visibility = View.VISIBLE
            iconView.setImageDrawable(icon.drawable)
        }
        adView.iconView = iconView

        val cta = nativeAd.callToAction
        if (cta.isNullOrBlank()) {
            ctaView.visibility = View.GONE
        } else {
            ctaView.visibility = View.VISIBLE
            ctaView.text = cta
        }
        adView.callToActionView = ctaView

        // Only layouts 3/4/5 have media; FIT_CENTER letterboxes instead of cropping the creative.
        adView.findViewById<MediaView>(R.id.ad_media_view)?.let { mediaView ->
            mediaView.setImageScaleType(ImageView.ScaleType.FIT_CENTER)
            adView.mediaView = mediaView
        }

        NativeAdStyler.apply(adView, RemoteConfigManager.current())
        adView.setNativeAd(nativeAd)
    }

    @LayoutRes
    private fun adLayoutRes(layoutType: Int): Int = when (layoutType) {
        AdsConfig.NATIVE_LAYOUT_MEDIUM_WITHOUT_MEDIA_BOTTOM_CTA -> R.layout.native_ad_medium_without_media_bottom_cta
        AdsConfig.NATIVE_LAYOUT_MEDIUM_LEFT_MEDIA_RIGHT_CTA -> R.layout.native_ad_medium_left_media_right_cta
        AdsConfig.NATIVE_LAYOUT_MEDIUM_LEFT_MEDIA_BOTTOM_CTA -> R.layout.native_ad_medium_left_media_bottom_cta
        AdsConfig.NATIVE_LAYOUT_LARGE_TOP_MEDIA_BOTTOM_CTA -> R.layout.native_ad_large_top_media_bottom_cta
        else -> R.layout.native_ad_medium_without_media_right_cta
    }

    /**
     * Layout 1's top-right corner is its CTA, so the SDK's default top-right AdChoices overlay would
     * land on a clickable view (AdMob policy). That layout alone gets it on the top left, where only
     * the root padding next to the (rounded) icon is; layouts 2/3/4 keep a 20dp gap for it at the top
     * right of the headline and layout 5 has the media view there.
     */
    private fun adChoicesPlacement(layoutType: Int): Int =
        if (adLayoutRes(layoutType) == R.layout.native_ad_medium_without_media_right_cta) {
            NativeAdOptions.ADCHOICES_TOP_LEFT
        } else {
            NativeAdOptions.ADCHOICES_TOP_RIGHT
        }

    /** Null for layout 1: layout_native_ad_shimmer_placeholder already holds its skeleton. */
    @LayoutRes
    private fun skeletonLayoutRes(layoutType: Int): Int? = when (layoutType) {
        AdsConfig.NATIVE_LAYOUT_MEDIUM_WITHOUT_MEDIA_BOTTOM_CTA -> R.layout.layout_native_ad_skeleton_medium_without_media_bottom_cta
        AdsConfig.NATIVE_LAYOUT_MEDIUM_LEFT_MEDIA_RIGHT_CTA -> R.layout.layout_native_ad_skeleton_medium_left_media_right_cta
        AdsConfig.NATIVE_LAYOUT_MEDIUM_LEFT_MEDIA_BOTTOM_CTA -> R.layout.layout_native_ad_skeleton_medium_left_media_bottom_cta
        AdsConfig.NATIVE_LAYOUT_LARGE_TOP_MEDIA_BOTTOM_CTA -> R.layout.layout_native_ad_skeleton_large_top_media_bottom_cta
        else -> null
    }
}
