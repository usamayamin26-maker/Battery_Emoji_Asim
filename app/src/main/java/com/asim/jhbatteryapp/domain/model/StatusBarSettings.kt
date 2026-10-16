package com.asim.jhbatteryapp.domain.model

import com.asim.jhbatteryapp.R

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
    val leftStatusEmojiRes: Int = 0
) {
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
