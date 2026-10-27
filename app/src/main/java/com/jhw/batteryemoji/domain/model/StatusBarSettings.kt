package com.jhw.batteryemoji.domain.model

import com.jhw.batteryemoji.R

/**
 * Centralized domain model representing the complete status bar configuration,
 * including geometry (height, margins), colors, typography, battery icon style,
 * and enabled custom status bar indicators.
 */
data class StatusBarSettings(
    val heightDp: Int = 40,
    val leftMarginDp: Int = 0,
    val rightMarginDp: Int = 16,
    val backgroundColor: Int = 0, // 0 = default theme gradient/color
    val iconColor: Int = 0xFF9333EA.toInt(),
    val batteryTextSizeSp: Int = 12,
    val batteryStyle: String = "circle",
    val appliedBatteryIconRes: Int = R.drawable.ic_status_battery,
    val isBatteryIconTintable: Boolean = true,
    val batteryIconSizeDp: Int = 20,
    val enabledCustomIcons: Set<String> = emptySet(),
    val iconColors: Map<String, Int> = emptyMap(),
    val iconSizes: Map<String, Int> = emptyMap(),
    val emotionStyle: String = "love",
    val dateFormat: String = "EEE, MMM d",
    val dateStyle: Int = 0,
    val lastAppliedThemeMode: String = "theme", // "theme" or "custom"
    val appliedThemeDrawableRes: Int = R.drawable.bg_theme_grad_pink,
    val isAppliedThemeGradient: Boolean = true,
    val appliedMascotDrawableRes: Int = R.drawable.ic_blueprint_girl_pink,
    val mascotSizeDp: Int = 20,
    val isEmojiBatteryEnabled: Boolean = true,
    val leftStatusEmojiRes: Int = 0,
    val appliedAnimationRawRes: Int = 0,
    val isAppliedAnimationLottie: Boolean = false,
    val appliedBatteryIconPath: String = "",
    val appliedAnimationPath: String = "",
    val leftStatusEmojiPath: String = "",
    val emotionPath: String = ""
) {
    /**
     * Downloaded battery icon to draw, or null. A remote icon is applied by setting the res to 0,
     * so applying any built-in icon (non-zero res) automatically takes precedence over it.
     */
    val remoteBatteryIconPath: String?
        get() = appliedBatteryIconPath.takeIf { appliedBatteryIconRes == 0 && it.isNotEmpty() }

    /** Downloaded left status emoji to draw, or null. Same res == 0 rule as [remoteBatteryIconPath]. */
    val remoteLeftEmojiPath: String?
        get() = leftStatusEmojiPath.takeIf { leftStatusEmojiRes == 0 && it.isNotEmpty() }

    /** Downloaded emotion icon to draw, or null; a remote emotion is [EMOTION_STYLE_REMOTE] + a file. */
    val remoteEmotionPath: String?
        get() = emotionPath.takeIf { emotionStyle == EMOTION_STYLE_REMOTE && it.isNotEmpty() }

    /** Downloaded Lottie/image animation to draw, or null. Same res == 0 rule as [remoteBatteryIconPath]. */
    val remoteAnimationPath: String?
        get() = appliedAnimationPath.takeIf {
            isAppliedAnimationLottie && appliedAnimationRawRes == 0 && it.isNotEmpty()
        }

    companion object {
        const val EMOTION_STYLE_REMOTE = "remote"
    }

    fun getEffectiveIconColor(iconId: String): Int {
        if (iconId == "sim" || iconId == "signal") {
            return iconColors["signal"] ?: iconColors["sim"] ?: iconColor
        }
        return iconColors[iconId] ?: iconColor
    }

    fun getEffectiveIconSize(iconId: String): Int {
        if (iconId == "sim" || iconId == "signal") {
            return iconSizes["signal"] ?: iconSizes["sim"] ?: 16
        }
        return iconSizes[iconId] ?: 16
    }
}
