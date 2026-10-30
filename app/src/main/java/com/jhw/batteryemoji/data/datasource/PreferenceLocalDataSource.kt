package com.jhw.batteryemoji.data.datasource

import android.content.Context
import android.content.SharedPreferences
import com.jhw.batteryemoji.JHBatteryApplication
import com.jhw.batteryemoji.R

class PreferenceLocalDataSource(
    context: Context = JHBatteryApplication.instance
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isLanguageSelected(): Boolean {
        return prefs.getBoolean(KEY_LANGUAGE_SELECTED, false)
    }

    fun setLanguageSelected(selected: Boolean) {
        prefs.edit().putBoolean(KEY_LANGUAGE_SELECTED, selected).apply()
    }

    fun isEmojiBatteryEnabled(): Boolean {
        return prefs.getBoolean(KEY_EMOJI_BATTERY_ENABLED, true)
    }

    fun setEmojiBatteryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EMOJI_BATTERY_ENABLED, enabled).apply()
    }

    fun getAppliedThemeDrawableRes(): Int {
        return prefs.getInt(KEY_APPLIED_THEME_RES, R.drawable.bg_theme_grad_pink)
    }

    fun setAppliedThemeDrawableRes(res: Int) {
        prefs.edit().putInt(KEY_APPLIED_THEME_RES, res).apply()
    }

    fun getAppliedThemePath(): String {
        return prefs.getString(KEY_APPLIED_THEME_PATH, "") ?: ""
    }

    fun setAppliedThemePath(path: String) {
        prefs.edit().putString(KEY_APPLIED_THEME_PATH, path).apply()
    }

    fun isAppliedThemeGradient(): Boolean {
        return prefs.getBoolean(KEY_APPLIED_THEME_IS_GRADIENT, true)
    }

    fun setAppliedThemeGradient(isGradient: Boolean) {
        prefs.edit().putBoolean(KEY_APPLIED_THEME_IS_GRADIENT, isGradient).apply()
    }

    fun getAppliedMascotDrawableRes(): Int {
        return prefs.getInt(KEY_APPLIED_MASCOT_RES, R.drawable.ic_blueprint_girl_pink)
    }

    fun setAppliedMascotDrawableRes(res: Int) {
        prefs.edit().putInt(KEY_APPLIED_MASCOT_RES, res).apply()
    }

    // Status Bar Customization
    fun getCustomStatusBarHeight(): Int {
        return prefs.getInt(KEY_CUSTOM_STATUS_BAR_HEIGHT, 40)
    }

    fun setCustomStatusBarHeight(heightDp: Int) {
        prefs.edit().putInt(KEY_CUSTOM_STATUS_BAR_HEIGHT, heightDp).apply()
    }

    fun getCustomLeftMargin(): Int {
        return prefs.getInt(KEY_CUSTOM_LEFT_MARGIN, 0)
    }

    fun setCustomLeftMargin(marginDp: Int) {
        prefs.edit().putInt(KEY_CUSTOM_LEFT_MARGIN, marginDp).apply()
    }

    fun getCustomRightMargin(): Int {
        return prefs.getInt(KEY_CUSTOM_RIGHT_MARGIN, 16)
    }

    fun setCustomRightMargin(marginDp: Int) {
        prefs.edit().putInt(KEY_CUSTOM_RIGHT_MARGIN, marginDp).apply()
    }

    fun getCustomBackgroundColor(): Int {
        return prefs.getInt(KEY_CUSTOM_BG_COLOR, 0) // 0 means default gradient theme
    }

    fun setCustomBackgroundColor(color: Int) {
        prefs.edit().putInt(KEY_CUSTOM_BG_COLOR, color).apply()
    }

    fun getCustomIconColor(): Int {
        return prefs.getInt(KEY_CUSTOM_ICON_COLOR, 0xFF9333EA.toInt())
    }

    fun setCustomIconColor(color: Int) {
        prefs.edit().putInt(KEY_CUSTOM_ICON_COLOR, color).apply()
    }

    fun getCustomBatteryTextSize(): Int {
        return prefs.getInt(KEY_CUSTOM_BATTERY_TEXT_SIZE, 12)
    }

    fun setCustomBatteryTextSize(sizeSp: Int) {
        prefs.edit().putInt(KEY_CUSTOM_BATTERY_TEXT_SIZE, sizeSp).apply()
    }

    fun getCustomBatteryStyle(): String {
        return prefs.getString(KEY_CUSTOM_BATTERY_STYLE, "default") ?: "default"
    }

    fun setCustomBatteryStyle(style: String) {
        prefs.edit().putString(KEY_CUSTOM_BATTERY_STYLE, style).apply()
    }

    fun getAppliedBatteryIconRes(): Int {
        return prefs.getInt(KEY_APPLIED_BATTERY_ICON_RES, R.drawable.ic_status_battery)
    }

    fun setAppliedBatteryIconRes(res: Int) {
        prefs.edit().putInt(KEY_APPLIED_BATTERY_ICON_RES, res).apply()
    }

    /** Absolute path of a downloaded remote battery icon; used only while the applied icon res is 0. */
    fun getAppliedBatteryIconPath(): String {
        return prefs.getString(KEY_APPLIED_BATTERY_ICON_PATH, "") ?: ""
    }

    fun setAppliedBatteryIconPath(path: String) {
        prefs.edit().putString(KEY_APPLIED_BATTERY_ICON_PATH, path).apply()
    }

    /** Keys of recently applied emojis, newest first ("local:<id>" or "remote:<id>"). */
    fun getRecentEmojiKeys(): List<String> {
        val raw = prefs.getString(KEY_RECENT_EMOJIS, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split(RECENT_SEPARATOR)
    }

    fun addRecentEmojiKey(key: String) {
        val updated = (listOf(key) + getRecentEmojiKeys().filter { it != key }).take(MAX_RECENT_EMOJIS)
        prefs.edit().putString(KEY_RECENT_EMOJIS, updated.joinToString(RECENT_SEPARATOR)).apply()
    }

    /** Absolute path of a downloaded remote animation; used only while the applied animation res is 0. */
    fun getAppliedAnimationPath(): String {
        return prefs.getString(KEY_APPLIED_ANIMATION_PATH, "") ?: ""
    }

    fun setAppliedAnimationPath(path: String) {
        prefs.edit().putString(KEY_APPLIED_ANIMATION_PATH, path).apply()
    }

    fun isBatteryIconTintable(): Boolean {
        return prefs.getBoolean(KEY_IS_BATTERY_ICON_TINTABLE, true)
    }

    fun setBatteryIconTintable(isTintable: Boolean) {
        prefs.edit().putBoolean(KEY_IS_BATTERY_ICON_TINTABLE, isTintable).apply()
    }

    fun getCustomBatteryIconSize(): Int {
        return prefs.getInt(KEY_CUSTOM_BATTERY_ICON_SIZE, 20)
    }

    fun setCustomBatteryIconSize(sizeDp: Int) {
        prefs.edit().putInt(KEY_CUSTOM_BATTERY_ICON_SIZE, sizeDp).apply()
    }

    fun getEnabledCustomIcons(): Set<String> {
        return prefs.getStringSet(KEY_ENABLED_CUSTOM_ICONS, emptySet()) ?: emptySet()
    }

    fun setEnabledCustomIcons(icons: Set<String>) {
        prefs.edit().putStringSet(KEY_ENABLED_CUSTOM_ICONS, icons).apply()
    }

    fun isCustomIconEnabled(iconId: String): Boolean {
        return getEnabledCustomIcons().contains(iconId)
    }

    fun setCustomIconEnabled(iconId: String, enabled: Boolean) {
        val current = getEnabledCustomIcons().toMutableSet()
        if (enabled) {
            current.add(iconId)
        } else {
            current.remove(iconId)
        }
        setEnabledCustomIcons(current)
    }

    fun getCustomIconSize(iconId: String): Int {
        return prefs.getInt(KEY_CUSTOM_ICON_SIZE_PREFIX + iconId, 16)
    }

    fun setCustomIconSize(iconId: String, sizeDp: Int) {
        prefs.edit().putInt(KEY_CUSTOM_ICON_SIZE_PREFIX + iconId, sizeDp).apply()
    }

    fun getCustomIconColor(iconId: String): Int {
        return prefs.getInt(KEY_CUSTOM_ICON_COLOR_PREFIX + iconId, 0xFF9333EA.toInt())
    }

    fun setCustomIconColor(iconId: String, color: Int) {
        prefs.edit().putInt(KEY_CUSTOM_ICON_COLOR_PREFIX + iconId, color).apply()
    }

    fun getEmotionStyle(): String {
        return prefs.getString(KEY_EMOTION_STYLE, "love") ?: "love"
    }

    fun setEmotionStyle(style: String) {
        prefs.edit().putString(KEY_EMOTION_STYLE, style).apply()
    }

    fun getDateFormat(): String {
        return prefs.getString(KEY_DATE_FORMAT, "EEE, MMM dd") ?: "EEE, MMM dd"
    }

    fun setDateFormat(format: String) {
        prefs.edit().putString(KEY_DATE_FORMAT, format).apply()
    }

    fun getDateStyle(): Int {
        return prefs.getInt(KEY_DATE_STYLE, 0)
    }

    fun setDateStyle(style: Int) {
        prefs.edit().putInt(KEY_DATE_STYLE, style).apply()
    }

    fun getLastAppliedThemeMode(): String {
        return prefs.getString(KEY_LAST_APPLIED_THEME_MODE, "custom") ?: "custom"
    }

    fun setLastAppliedThemeMode(mode: String) {
        prefs.edit().putString(KEY_LAST_APPLIED_THEME_MODE, mode).apply()
    }

    fun getLeftStatusEmojiRes(): Int {
        return prefs.getInt(KEY_LEFT_STATUS_EMOJI_RES, 0)
    }

    fun setLeftStatusEmojiRes(res: Int) {
        prefs.edit().putInt(KEY_LEFT_STATUS_EMOJI_RES, res).apply()
    }

    fun getEmotionPath(): String {
        return prefs.getString(KEY_EMOTION_PATH, "") ?: ""
    }

    fun setEmotionPath(path: String) {
        prefs.edit().putString(KEY_EMOTION_PATH, path).apply()
    }

    fun getLeftStatusEmojiPath(): String {
        return prefs.getString(KEY_LEFT_STATUS_EMOJI_PATH, "") ?: ""
    }

    fun setLeftStatusEmojiPath(path: String) {
        prefs.edit().putString(KEY_LEFT_STATUS_EMOJI_PATH, path).apply()
    }

    fun getAppliedAnimationRawRes(): Int {
        return prefs.getInt(KEY_APPLIED_ANIMATION_RAW_RES, 0)
    }

    fun setAppliedAnimationRawRes(res: Int) {
        prefs.edit().putInt(KEY_APPLIED_ANIMATION_RAW_RES, res).apply()
    }

    fun isAppliedAnimationLottie(): Boolean {
        return prefs.getBoolean(KEY_IS_APPLIED_ANIMATION_LOTTIE, false)
    }

    fun setAppliedAnimationLottie(isLottie: Boolean) {
        prefs.edit().putBoolean(KEY_IS_APPLIED_ANIMATION_LOTTIE, isLottie).apply()
    }

    fun getMascotSize(): Int {
        return prefs.getInt(KEY_MASCOT_SIZE_DP, 20)
    }

    fun setMascotSize(sizeDp: Int) {
        prefs.edit().putInt(KEY_MASCOT_SIZE_DP, sizeDp).apply()
    }

    fun isFloatingStickerEnabled(): Boolean {
        return prefs.getBoolean(KEY_FLOATING_STICKER_ENABLED, false)
    }

    fun setFloatingStickerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLOATING_STICKER_ENABLED, enabled).apply()
    }

    fun getFloatingStickerDrawableRes(): Int {
        return prefs.getInt(KEY_FLOATING_STICKER_RES, R.drawable.ic_sticker_player_blue)
    }

    fun setFloatingStickerDrawableRes(res: Int) {
        prefs.edit().putInt(KEY_FLOATING_STICKER_RES, res).apply()
    }

    fun getFloatingStickerImagePath(): String {
        return prefs.getString(KEY_FLOATING_STICKER_PATH, "") ?: ""
    }

    fun setFloatingStickerImagePath(path: String) {
        prefs.edit().putString(KEY_FLOATING_STICKER_PATH, path).apply()
    }

    fun getFloatingStickerX(): Int {
        return prefs.getInt(KEY_FLOATING_STICKER_X, -1)
    }

    fun setFloatingStickerX(x: Int) {
        prefs.edit().putInt(KEY_FLOATING_STICKER_X, x).apply()
    }

    fun getFloatingStickerY(): Int {
        return prefs.getInt(KEY_FLOATING_STICKER_Y, -1)
    }

    fun setFloatingStickerY(y: Int) {
        prefs.edit().putInt(KEY_FLOATING_STICKER_Y, y).apply()
    }

    fun getFloatingStickerSize(): Int {
        return prefs.getInt(KEY_FLOATING_STICKER_SIZE, 60)
    }

    fun setFloatingStickerSize(sizeDp: Int) {
        prefs.edit().putInt(KEY_FLOATING_STICKER_SIZE, sizeDp).apply()
    }

    fun getFloatingStickerRotation(): Float {
        return prefs.getFloat(KEY_FLOATING_STICKER_ROTATION, 0f)
    }

    fun setFloatingStickerRotation(rotDeg: Float) {
        prefs.edit().putFloat(KEY_FLOATING_STICKER_ROTATION, rotDeg).apply()
    }

    companion object {
        private const val PREFS_NAME = "jh_battery_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_LANGUAGE_SELECTED = "key_language_selected"
        private const val KEY_EMOJI_BATTERY_ENABLED = "key_emoji_battery_enabled"
        private const val KEY_APPLIED_THEME_RES = "key_applied_theme_res"
        private const val KEY_APPLIED_THEME_PATH = "key_applied_theme_path"
        private const val KEY_APPLIED_THEME_IS_GRADIENT = "key_applied_theme_is_gradient"
        private const val KEY_APPLIED_MASCOT_RES = "key_applied_mascot_res"
        private const val KEY_MASCOT_SIZE_DP = "key_mascot_size_dp"

        private const val KEY_FLOATING_STICKER_ENABLED = "key_floating_sticker_enabled"
        private const val KEY_FLOATING_STICKER_RES = "key_floating_sticker_res"
        private const val KEY_FLOATING_STICKER_PATH = "key_floating_sticker_path"
        private const val KEY_LEFT_STATUS_EMOJI_PATH = "key_left_status_emoji_path"
        private const val KEY_EMOTION_PATH = "key_emotion_path"
        private const val KEY_FLOATING_STICKER_X = "key_floating_sticker_x"
        private const val KEY_FLOATING_STICKER_Y = "key_floating_sticker_y"
        private const val KEY_FLOATING_STICKER_SIZE = "key_floating_sticker_size"
        private const val KEY_FLOATING_STICKER_ROTATION = "key_floating_sticker_rotation"

        private const val KEY_CUSTOM_STATUS_BAR_HEIGHT = "key_custom_status_bar_height"
        private const val KEY_CUSTOM_LEFT_MARGIN = "key_custom_left_margin"
        private const val KEY_CUSTOM_RIGHT_MARGIN = "key_custom_right_margin"
        private const val KEY_CUSTOM_BG_COLOR = "key_custom_bg_color"
        private const val KEY_CUSTOM_ICON_COLOR = "key_custom_icon_color"
        private const val KEY_CUSTOM_BATTERY_TEXT_SIZE = "key_custom_battery_text_size"
        private const val KEY_CUSTOM_BATTERY_STYLE = "key_custom_battery_style"
        private const val KEY_APPLIED_BATTERY_ICON_RES = "key_applied_battery_icon_res"
        private const val KEY_APPLIED_BATTERY_ICON_PATH = "key_applied_battery_icon_path"
        private const val KEY_APPLIED_ANIMATION_PATH = "key_applied_animation_path"
        private const val KEY_IS_BATTERY_ICON_TINTABLE = "key_is_battery_icon_tintable"
        private const val KEY_CUSTOM_BATTERY_ICON_SIZE = "key_custom_battery_icon_size"
        private const val KEY_ENABLED_CUSTOM_ICONS = "key_enabled_custom_icons"
        private const val KEY_CUSTOM_ICON_SIZE_PREFIX = "key_custom_icon_size_"
        private const val KEY_CUSTOM_ICON_COLOR_PREFIX = "key_custom_icon_color_"
        private const val KEY_EMOTION_STYLE = "key_emotion_style"
        private const val KEY_DATE_FORMAT = "key_date_format"
        private const val KEY_DATE_STYLE = "key_date_style"
        private const val KEY_LAST_APPLIED_THEME_MODE = "key_last_applied_theme_mode"
        private const val KEY_LEFT_STATUS_EMOJI_RES = "key_left_status_emoji_res"
        private const val KEY_APPLIED_ANIMATION_RAW_RES = "key_applied_animation_raw_res"
        private const val KEY_IS_APPLIED_ANIMATION_LOTTIE = "key_is_applied_animation_lottie"
        private const val KEY_RECENT_EMOJIS = "key_recent_emojis"
        private const val RECENT_SEPARATOR = "|"
        private const val MAX_RECENT_EMOJIS = 9
    }
}
