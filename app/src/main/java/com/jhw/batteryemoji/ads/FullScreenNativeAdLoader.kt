package com.universal.remote.ads

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.ads.RemoteConfigManager
import com.jhw.batteryemoji.databinding.DialogFullScreenNativeAdBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Shows a full-screen native ad (icon + headline + body + media + CTA) hosted in a plain
 * full-screen [Dialog], with a shimmer "Loading ad.." placeholder while the request is in
 * flight and an always-available close button so the user is never stuck if the network is slow
 * or the ad fails to load.
 */
object FullScreenNativeAdLoader {

    fun loadAndShow(
        activity: Activity,
        adUnitId: String,
        isEnabled: Boolean,
        onComplete: () -> Unit
    ) {
        val placement = activity.javaClass.simpleName
        if (!isEnabled || adUnitId.isBlank()) {
            AdLogger.disabled(AdLogger.FULL_SCREEN_NATIVE, placement)
            onComplete()
            return
        }

        val dialogBinding = DialogFullScreenNativeAdBinding.inflate(activity.layoutInflater)
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(dialogBinding.root)
        // The dialog window draws behind the nav bar, which would cover the CTA at the bottom.
        applyEdgeToEdgeInsets(dialogBinding.root)
        dialog.setCancelable(false)
        // A skeleton may carry a "loading" label (R.id.tvAdLoadingLabel, declared in ids_ads.xml so
        // the id exists even where no skeleton has one); the full-screen dialog never shows it.
        dialogBinding.root.findViewById<View>(R.id.tvAdLoadingLabel)?.visibility = View.GONE

        val completed = AtomicBoolean(false)
        var shownAd: NativeAd? = null
        fun finishOnce() {
            if (completed.compareAndSet(false, true)) {
                FullScreenAdState.end()
                dialog.dismissSafely()
                // Nothing caches a full-screen native: once it is closed, it is done.
                shownAd?.destroy()
                shownAd = null
                onComplete()
            }
        }

        dialogBinding.btnCloseFullScreenNative.setOnClickListener {
            AdLogger.dismissed(AdLogger.FULL_SCREEN_NATIVE, placement)
            finishOnce()
        }
        // Without this flag the window gets a black legacy nav bar strip over the root. On API 35+
        // the strip is transparent (root shows through); below that it takes this color. While the
        // shimmer shows, root (XML) and strip stay native_ad_full_screen_bg; the ad's color comes on load.
        dialog.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            navigationBarColor = activity.getColor(R.color.native_ad_full_screen_bg)
        }
        dialogBinding.shimmerFullScreenNative.startShimmer()
        FullScreenAdState.begin(activity)
        // A full-screen dialog is a full-screen ad, whatever format is inside it: it takes the whole
        // screen and the user has to dismiss it. It starts the gap here, at the point it covers the
        // screen, so an interstitial cannot land seconds later - the loading placeholder is part of
        // the interruption, which is why this is not deferred to the ad arriving.
        dialog.show()

        val adLoader = AdLoader.Builder(activity, adUnitId)
            .forNativeAd { nativeAd ->
                AdLogger.loaded(AdLogger.FULL_SCREEN_NATIVE, placement)
                if (completed.get() || activity.isFinishing) {
                    nativeAd.destroy()
                    return@forNativeAd
                }

                dialogBinding.shimmerFullScreenNative.stopShimmer()
                dialogBinding.shimmerFullScreenNative.visibility = View.GONE

                val adView = LayoutInflater.from(activity)
                    .inflate(R.layout.native_ad_full_screen, dialogBinding.adContainerFullScreenNative, false)
                        as NativeAdView
                bind(adView, nativeAd)
                shownAd = nativeAd

                dialogBinding.adContainerFullScreenNative.removeAllViews()
                dialogBinding.adContainerFullScreenNative.addView(adView)
                dialogBinding.adContainerFullScreenNative.visibility = View.VISIBLE
                // Same color as the ad's root, so the dialog behind it and the nav bar strip match.
                val adColor = NativeAdStyler.fullScreenBackgroundColor(dialogBinding.root, RemoteConfigManager.current())
                dialogBinding.root.setBackgroundColor(adColor)
                dialog.window?.navigationBarColor = adColor
                AdLogger.shown(AdLogger.FULL_SCREEN_NATIVE, placement)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.failed(AdLogger.FULL_SCREEN_NATIVE, placement, error)
                    finishOnce()
                }

                override fun onAdImpression() = AdLogger.impression(AdLogger.FULL_SCREEN_NATIVE, placement)

                override fun onAdClicked() {
                    AdLogger.clicked(AdLogger.FULL_SCREEN_NATIVE, placement)
                    ResumeAppOpenManager.skipNextResume()
                }
            })
            // Top-right is under the close button, which would hide the AdChoices icon.
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
                    .build()
            )
            .build()

        AdLogger.request(AdLogger.FULL_SCREEN_NATIVE, placement, adUnitId)
        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * Pads the dialog's root clear of the status and navigation bars. Kept private here rather
     * than taken from the host app's extensions, so this package depends on no class of the app
     * it is pasted into.
     */
    private fun applyEdgeToEdgeInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun bind(adView: NativeAdView, nativeAd: NativeAd) {
        val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView>(R.id.ad_body)
        val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
        val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
        val mediaView = adView.findViewById<MediaView>(R.id.ad_media_view)

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

        mediaView.setImageScaleType(ImageView.ScaleType.FIT_CENTER)
        adView.mediaView = mediaView

        NativeAdStyler.apply(adView, RemoteConfigManager.current(), fullScreen = true)
        adView.setNativeAd(nativeAd)
    }
}
