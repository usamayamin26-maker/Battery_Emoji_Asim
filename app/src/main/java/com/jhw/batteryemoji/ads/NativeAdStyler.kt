package com.universal.remote.ads

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.ads.AdsConfig
import com.google.android.gms.ads.nativead.NativeAdView
import kotlin.math.roundToInt

/**
 * Applies the Remote Config native ad colors (card background/border, Ad tag text/border/background,
 * headline, description, CTA text/background) to any native ad layout, so a single parameter change
 * restyles every native ad in the app. Views missing from a layout are skipped.
 */
object NativeAdStyler {

    private const val TAG = "NativeAdStyler"

    /** Must match the stroke width in bg_native_ad_tag.xml. */
    private const val TAG_BORDER_WIDTH_DP = 0.8f

    /** Must match the stroke width in bg_ad_placeholder.xml. */
    private const val CARD_BORDER_WIDTH_DP = 1f

    /**
     * [fullScreen]: native_ad_full_screen's root is a plain color (no card outline), so only
     * native_ad_bg_color applies there, defaulting to native_ad_full_screen_bg.
     */
    fun apply(adView: NativeAdView, config: AdsConfig, fullScreen: Boolean = false) {
        val tagTextColor = resolve(adView, config.nativeAdTagTextColor, R.color.native_ad_tag_text)
        val tagBgColor = resolve(adView, config.nativeAdTagBgColor, R.color.native_ad_tag_bg)
        val tagBorderColor = resolve(config.nativeAdTagBorderColor, tagTextColor)
        val headingColor = resolve(adView, config.nativeHeadingTextColor, R.color.native_ad_heading)
        val descriptionColor = resolve(adView, config.nativeDescriptionTextColor, R.color.native_ad_description)
        val buttonTextColor = resolve(adView, config.nativeButtonTextColor, R.color.native_ad_button_text)
        val buttonBgColor = resolve(adView, config.nativeButtonBgColor, R.color.native_ad_button_bg)

        if (fullScreen) {
            adView.setBackgroundColor(fullScreenBackgroundColor(adView, config))
        } else {
            styleCard(adView, config)
        }

        adView.findViewById<TextView>(R.id.tvAdBadge)?.let { tag ->
            tag.setTextColor(tagTextColor)
            tag.mutableShape()?.apply {
                setColor(tagBgColor)
                setStroke(tag.dpToPx(TAG_BORDER_WIDTH_DP), tagBorderColor)
            }
        }

        adView.findViewById<TextView>(R.id.ad_headline)?.setTextColor(headingColor)
        adView.findViewById<TextView>(R.id.ad_body)?.setTextColor(descriptionColor)

        adView.findViewById<TextView>(R.id.ad_call_to_action)?.let { button ->
            button.setTextColor(buttonTextColor)
            button.mutableShape()?.setColor(buttonBgColor)
        }
    }

    /**
     * native_ad_bg_color for full-screen roots (the ad's root, and the dialog root + nav bar once
     * the ad has loaded — not while the shimmer shows), defaulting to native_ad_full_screen_bg.
     */
    fun fullScreenBackgroundColor(view: View, config: AdsConfig): Int =
        resolve(view, config.nativeAdBgColor, R.color.native_ad_full_screen_bg)

    /**
     * Card fill and outline for the native ad roots (background bg_ad_placeholder). Not used on
     * shimmer placeholders: they keep the XML default colors, only the real ad gets the config colors.
     */
    fun styleCard(card: View, config: AdsConfig) {
        card.mutableShape()?.apply {
            setColor(resolve(card, config.nativeAdBgColor, R.color.native_ad_card_bg))
            setStroke(
                card.dpToPx(CARD_BORDER_WIDTH_DP),
                resolve(card, config.nativeAdBorderColor, R.color.native_ad_card_border)
            )
        }
    }

    /** Mutated so recoloring one ad never leaks into other views sharing the same drawable. */
    private fun View.mutableShape(): GradientDrawable? = background?.mutate() as? GradientDrawable

    private fun View.dpToPx(dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).roundToInt()

    // The parameter stays nullable although AdsConfig's colour fields no longer can be. It was a
    // Gson workaround — reflection could leave a missing key null despite the Kotlin default once
    // R8 stripped the no-arg constructor. kotlinx.serialization generates the serializer at compile
    // time, so that cannot happen any more; the null branch is kept only because it costs nothing
    // and callers may pass a value from elsewhere.
    private fun resolve(view: View, value: String?, @ColorRes defaultRes: Int): Int =
        resolve(value, view.context.getColor(defaultRes))

    private fun resolve(value: String?, @ColorInt default: Int): Int {
        if (value.isNullOrBlank()) return default
        return try {
            Color.parseColor(value.trim())
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid native ad color \"$value\", using default")
            default
        }
    }
}
