package com.jhw.batteryemoji.ads

import android.util.Log
import com.universal.remote.ads.AdsController
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull


/**
 * The ads Remote Config JSON.
 *
 * Only the keys **this package itself reads** are typed fields here — the shared units, splash,
 * resume app open, rewarded and native colours — so the file is the same in every project. Every
 * screen key (`<slot>_banner`, `<slot>_native`, `is_show_*`, `admob_<units>_*_id`, `counter_*`) is
 * the app's own and is read by name with [int] / [string] / [unit], straight from [raw]. A new
 * screen or a new placement is then a key in the app's default JSON, never an edit to this file.
 */
@Serializable
data class AdsConfig(
    @SerialName("all_ads_on") val allAdsOn: Int = 1,
    @SerialName("app_id") val appId: String = "",

    // ---- Shared units: the fallback for every placement whose own unit is blank.
    @SerialName("admob_app_banner_id") val appBannerId: String = "",
    @SerialName("admob_app_native_id") val appNativeId: String = "",
    @SerialName("admob_app_inter_id") val appInterId: String = "",
    @SerialName("admob_app_rewarded_id") val appRewardedId: String = "",

    // ---- Splash full-screen ad (see SplashAds).
    @SerialName("admob_splash_app_open_id") val splashAppOpenId: String = "",
    @SerialName("admob_splash_inter_id") val splashInterId: String = "",
    @SerialName("admob_splash_native_id") val splashNativeId: String = "",
    @SerialName("splash_ad_preference") val splashAdPreference: Int = 0,
    @SerialName("is_show_admob_splash_app_open") val isShowSplashAppOpen: Int = 0,
    @SerialName("is_show_admob_splash_inter") val isShowSplashInter: Int = 0,
    @SerialName("is_show_admob_splash_native") val isShowSplashNative: Int = 0,

    // ---- App open ad on return from background.
    @SerialName("admob_resume_app_open_id") val resumeAppOpenId: String = "",
    @SerialName("is_show_admob_resume_app_open") val isShowResumeAppOpen: Int = 0,

    // ---- Native ad colors ("#RRGGBB" / "#AARRGGBB"), applied to every native ad by NativeAdStyler.
    // ---- Blank or invalid values fall back to the native_ad_* defaults in colors_ads.xml.
    @SerialName("native_ad_tag_text_color") val nativeAdTagTextColor: String = "",
    @SerialName("native_ad_tag_bg_color") val nativeAdTagBgColor: String = "",
    @SerialName("native_heading_text_color") val nativeHeadingTextColor: String = "",
    @SerialName("native_description_text_color") val nativeDescriptionTextColor: String = "",
    @SerialName("native_button_text_color") val nativeButtonTextColor: String = "",
    @SerialName("native_button_bg_color") val nativeButtonBgColor: String = "",
    @SerialName("native_ad_bg_color") val nativeAdBgColor: String = "",
    @SerialName("native_ad_border_color") val nativeAdBorderColor: String = "",
    @SerialName("native_ad_tag_border_color") val nativeAdTagBorderColor: String = "",

    /** The whole payload, for the app's own keys. Empty when the payload failed to parse. */
    @Transient val raw: JsonObject = JsonObject(emptyMap()),
) {

    /**
     * An app key's number: a flag (0/1), a banner mode, a native layout or a counter. A missing key,
     * a null or a quoted number reads as [default] — quoted numbers are a console typo, and reading
     * them as "off" is the fail-closed direction.
     */
    fun int(key: String, default: Int = 0): Int {
        val value = lookup(key) as? JsonPrimitive ?: return default
        if (value.isString) return default
        return value.intOrNull ?: default
    }

    /** An app key's text (usually a unit id); missing or not a string reads as "". */
    fun string(key: String): String {
        val value = lookup(key) as? JsonPrimitive ?: return ""
        return if (value.isString) value.content else ""
    }

    /** A flag key is on (== 1). */
    fun isOn(flagKey: String): Boolean {
        if (allAdsOn == 0) return false
        return int(flagKey) == 1
    }

    /**
     * A placement's unit id: [unitKey]'s value, or the shared [fallback] when it is blank, so a
     * console that has not filled a placement's own unit in still serves it from the shared one.
     */
    fun unit(unitKey: String?, fallback: String): String =
        unitKey?.let { string(it) }.orEmpty().ifBlank { fallback }

    /** Unit ids of the slot group [units] (`admob_<units>_banner_id` / `_native_id`), falling back to the app's. */
    fun bannerUnit(units: String?): String = unit(units?.let { "admob_${it}_banner_id" }, appBannerId)
    fun nativeUnit(units: String?): String = unit(units?.let { "admob_${it}_native_id" }, appNativeId)

    private fun lookup(key: String) = raw[key].also {
        // A typo in a key reads as "off" and nothing else says so; in debug builds the app's default
        // JSON is the list of known keys, so an unknown one is logged.
        if (it == null && AdsController.isDebug && !RemoteConfigManager.isKnownKey(key)) {
            Log.w("AdsConfig", "Unknown ads key \"$key\" - not in the app's default ads JSON")
        }
    }

    companion object {

        /** Splash full-screen ad (splash_ad_preference): no app open ad / interstitial on splash. */
        const val SPLASH_AD_PREFERENCE_NONE = 0

        /** Splash full-screen ad: app open first, interstitial as fallback (if both are on). */
        const val SPLASH_AD_PREFERENCE_APP_OPEN_FIRST = 1

        /** Splash full-screen ad: interstitial first, app open as fallback (if both are on). */
        const val SPLASH_AD_PREFERENCE_INTER_FIRST = 2

        /** Banner mode (<slot>_banner): ad hidden. */
        const val BANNER_MODE_NONE = 0

        /** Banner mode: fixed-size standard 320x50 banner. */
        const val BANNER_MODE_STANDARD = 1

        /** Banner mode: anchored adaptive collapsible banner, expands from the top. */
        const val BANNER_MODE_COLLAPSIBLE_TOP = 2

        /** Banner mode: anchored adaptive collapsible banner, expands from the bottom. */
        const val BANNER_MODE_COLLAPSIBLE_BOTTOM = 3

        /** Banner mode: fixed-size 300x250 medium rectangle. */
        const val BANNER_MODE_RECTANGLE = 4

        /** Banner mode: inline adaptive banner (max height in BannerAdLoader). */
        const val BANNER_MODE_INLINE_ADAPTIVE = 5

        /** Native layout (<slot>_native): ad hidden. */
        const val NATIVE_LAYOUT_NONE = 0

        /** Native layout 1: icon + tag/headline + description, CTA on the right. */
        const val NATIVE_LAYOUT_MEDIUM_WITHOUT_MEDIA_RIGHT_CTA = 1

        /** Native layout 2: icon + headline + tag/description, full-width CTA below. */
        const val NATIVE_LAYOUT_MEDIUM_WITHOUT_MEDIA_BOTTOM_CTA = 2

        /** Native layout 3: 190x120 media on the left, icon/text and CTA on the right. */
        const val NATIVE_LAYOUT_MEDIUM_LEFT_MEDIA_RIGHT_CTA = 3

        /** Native layout 4: 200x140 media on the left, icon/text on the right, full-width CTA below. */
        const val NATIVE_LAYOUT_MEDIUM_LEFT_MEDIA_BOTTOM_CTA = 4

        /** Native layout 5: 16:9 media on top of the layout 2 block. */
        const val NATIVE_LAYOUT_LARGE_TOP_MEDIA_BOTTOM_CTA = 5
    }
}
