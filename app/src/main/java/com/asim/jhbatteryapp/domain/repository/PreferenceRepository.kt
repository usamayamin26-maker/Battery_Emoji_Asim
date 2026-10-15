package com.asim.jhbatteryapp.domain.repository

interface PreferenceRepository {
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
    fun isEmojiBatteryEnabled(): Boolean
    fun setEmojiBatteryEnabled(enabled: Boolean)
    fun getAppliedThemeDrawableRes(): Int
    fun setAppliedThemeDrawableRes(res: Int)
    fun isAppliedThemeGradient(): Boolean
    fun setAppliedThemeGradient(isGradient: Boolean)
    fun getAppliedMascotDrawableRes(): Int
    fun setAppliedMascotDrawableRes(res: Int)

    // Status Bar Customization Settings
    fun getCustomStatusBarHeight(): Int
    fun setCustomStatusBarHeight(heightDp: Int)
    fun getCustomLeftMargin(): Int
    fun setCustomLeftMargin(marginDp: Int)
    fun getCustomRightMargin(): Int
    fun setCustomRightMargin(marginDp: Int)
    fun getCustomBackgroundColor(): Int
    fun setCustomBackgroundColor(color: Int)
    fun getCustomIconColor(): Int
    fun setCustomIconColor(color: Int)
    fun getCustomBatteryTextSize(): Int
    fun setCustomBatteryTextSize(sizeSp: Int)
    fun getCustomBatteryStyle(): String
    fun setCustomBatteryStyle(style: String)
    fun getAppliedBatteryIconRes(): Int
    fun setAppliedBatteryIconRes(res: Int)
    fun isBatteryIconTintable(): Boolean
    fun setBatteryIconTintable(isTintable: Boolean)
    fun getCustomBatteryIconSize(): Int
    fun setCustomBatteryIconSize(sizeDp: Int)

    // Multi-Selected Custom Status Bar Icons & Specific Icon Settings
    fun getEnabledCustomIcons(): Set<String>
    fun setEnabledCustomIcons(icons: Set<String>)
    fun isCustomIconEnabled(iconId: String): Boolean
    fun setCustomIconEnabled(iconId: String, enabled: Boolean)
    fun getCustomIconSize(iconId: String): Int
    fun setCustomIconSize(iconId: String, sizeDp: Int)
    fun getCustomIconColor(iconId: String): Int
    fun setCustomIconColor(iconId: String, color: Int)
    fun getEmotionStyle(): String
    fun setEmotionStyle(style: String)
    fun getDateFormat(): String
    fun setDateFormat(format: String)
    fun getDateStyle(): Int
    fun setDateStyle(style: Int)
    fun getLastAppliedThemeMode(): String
    fun setLastAppliedThemeMode(mode: String)
    fun getLeftStatusEmojiRes(): Int
    fun setLeftStatusEmojiRes(res: Int)

    // Centralized Settings & Observable Stream
    fun getStatusBarSettings(): com.asim.jhbatteryapp.domain.model.StatusBarSettings
    fun updateStatusBarSettings(settings: com.asim.jhbatteryapp.domain.model.StatusBarSettings)
    fun observeStatusBarSettings(): kotlinx.coroutines.flow.StateFlow<com.asim.jhbatteryapp.domain.model.StatusBarSettings>
}
