package com.asim.jhbatteryapp.data.repository

import com.asim.jhbatteryapp.data.datasource.PreferenceLocalDataSource
import com.asim.jhbatteryapp.domain.model.StatusBarSettings
import com.asim.jhbatteryapp.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceRepositoryImpl(
    private val preferenceLocalDataSource: PreferenceLocalDataSource = PreferenceLocalDataSource()
) : PreferenceRepository {

    companion object {
        private var _sharedSettingsFlow: MutableStateFlow<StatusBarSettings>? = null
    }

    private fun getSharedFlow(): MutableStateFlow<StatusBarSettings> {
        val flow = _sharedSettingsFlow
        if (flow != null) {
            return flow
        }
        val newFlow = MutableStateFlow(buildStatusBarSettings())
        _sharedSettingsFlow = newFlow
        return newFlow
    }

    override fun observeStatusBarSettings(): StateFlow<StatusBarSettings> =
        getSharedFlow().asStateFlow()

    override fun getStatusBarSettings(): StatusBarSettings {
        val latest = buildStatusBarSettings()
        getSharedFlow().value = latest
        return latest
    }

    private fun buildStatusBarSettings(): StatusBarSettings {
        val enabledIcons = preferenceLocalDataSource.getEnabledCustomIcons()
        val colors = mutableMapOf<String, Int>()
        val sizes = mutableMapOf<String, Int>()
        for (id in listOf("calendar", "airplane", "ringer", "hotspot", "emotion", "animation", "data", "signal", "sim", "wifi", "charge")) {
            colors[id] = preferenceLocalDataSource.getCustomIconColor(id)
            sizes[id] = preferenceLocalDataSource.getCustomIconSize(id)
        }
        val mascotSize = preferenceLocalDataSource.getMascotSize()
        val customAnimSize = sizes["animation"] ?: 16
        val effectiveAnimSize = if (customAnimSize in 10..80 && customAnimSize != 16) customAnimSize else if (mascotSize in 10..80) mascotSize else 30
        sizes["animation"] = effectiveAnimSize

        return StatusBarSettings(
            heightDp = preferenceLocalDataSource.getCustomStatusBarHeight(),
            leftMarginDp = preferenceLocalDataSource.getCustomLeftMargin(),
            rightMarginDp = preferenceLocalDataSource.getCustomRightMargin(),
            backgroundColor = preferenceLocalDataSource.getCustomBackgroundColor(),
            iconColor = preferenceLocalDataSource.getCustomIconColor(),
            batteryTextSizeSp = preferenceLocalDataSource.getCustomBatteryTextSize(),
            batteryStyle = preferenceLocalDataSource.getCustomBatteryStyle(),
            appliedBatteryIconRes = preferenceLocalDataSource.getAppliedBatteryIconRes(),
            isBatteryIconTintable = preferenceLocalDataSource.isBatteryIconTintable(),
            batteryIconSizeDp = preferenceLocalDataSource.getCustomBatteryIconSize(),
            enabledCustomIcons = enabledIcons,
            iconColors = colors,
            iconSizes = sizes,
            emotionStyle = preferenceLocalDataSource.getEmotionStyle(),
            dateFormat = preferenceLocalDataSource.getDateFormat(),
            dateStyle = preferenceLocalDataSource.getDateStyle(),
            lastAppliedThemeMode = preferenceLocalDataSource.getLastAppliedThemeMode(),
            appliedThemeDrawableRes = preferenceLocalDataSource.getAppliedThemeDrawableRes(),
            isAppliedThemeGradient = preferenceLocalDataSource.isAppliedThemeGradient(),
            appliedMascotDrawableRes = preferenceLocalDataSource.getAppliedMascotDrawableRes(),
            mascotSizeDp = effectiveAnimSize,
            isEmojiBatteryEnabled = preferenceLocalDataSource.isEmojiBatteryEnabled(),
            leftStatusEmojiRes = preferenceLocalDataSource.getLeftStatusEmojiRes(),
            appliedAnimationRawRes = preferenceLocalDataSource.getAppliedAnimationRawRes(),
            isAppliedAnimationLottie = preferenceLocalDataSource.isAppliedAnimationLottie()
        )
    }

    private fun notifySettingsChanged() {
        getSharedFlow().value = buildStatusBarSettings()
    }

    override fun updateStatusBarSettings(settings: StatusBarSettings) {
        preferenceLocalDataSource.setCustomStatusBarHeight(settings.heightDp)
        preferenceLocalDataSource.setCustomLeftMargin(settings.leftMarginDp)
        preferenceLocalDataSource.setCustomRightMargin(settings.rightMarginDp)
        preferenceLocalDataSource.setCustomBackgroundColor(settings.backgroundColor)
        preferenceLocalDataSource.setCustomIconColor(settings.iconColor)
        preferenceLocalDataSource.setCustomBatteryTextSize(settings.batteryTextSizeSp)
        preferenceLocalDataSource.setCustomBatteryStyle(settings.batteryStyle)
        preferenceLocalDataSource.setAppliedBatteryIconRes(settings.appliedBatteryIconRes)
        preferenceLocalDataSource.setBatteryIconTintable(settings.isBatteryIconTintable)
        preferenceLocalDataSource.setCustomBatteryIconSize(settings.batteryIconSizeDp)
        preferenceLocalDataSource.setEnabledCustomIcons(settings.enabledCustomIcons)
        settings.iconColors.forEach { (id, color) ->
            preferenceLocalDataSource.setCustomIconColor(id, color)
        }
        settings.iconSizes.forEach { (id, size) ->
            preferenceLocalDataSource.setCustomIconSize(id, size)
        }
        val animMascotSize = settings.iconSizes["animation"] ?: settings.mascotSizeDp
        preferenceLocalDataSource.setMascotSize(animMascotSize)
        preferenceLocalDataSource.setEmotionStyle(settings.emotionStyle)
        preferenceLocalDataSource.setDateFormat(settings.dateFormat)
        preferenceLocalDataSource.setDateStyle(settings.dateStyle)
        preferenceLocalDataSource.setLastAppliedThemeMode(settings.lastAppliedThemeMode)
        preferenceLocalDataSource.setAppliedThemeDrawableRes(settings.appliedThemeDrawableRes)
        preferenceLocalDataSource.setAppliedThemeGradient(settings.isAppliedThemeGradient)
        preferenceLocalDataSource.setAppliedMascotDrawableRes(settings.appliedMascotDrawableRes)
        preferenceLocalDataSource.setEmojiBatteryEnabled(settings.isEmojiBatteryEnabled)
        preferenceLocalDataSource.setLeftStatusEmojiRes(settings.leftStatusEmojiRes)
        preferenceLocalDataSource.setAppliedAnimationRawRes(settings.appliedAnimationRawRes)
        preferenceLocalDataSource.setAppliedAnimationLottie(settings.isAppliedAnimationLottie)
        notifySettingsChanged()
    }

    override fun isOnboardingCompleted(): Boolean {
        return preferenceLocalDataSource.isOnboardingCompleted()
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        preferenceLocalDataSource.setOnboardingCompleted(completed)
    }

    override fun isLanguageSelected(): Boolean {
        return preferenceLocalDataSource.isLanguageSelected()
    }

    override fun setLanguageSelected(selected: Boolean) {
        preferenceLocalDataSource.setLanguageSelected(selected)
    }

    override fun isEmojiBatteryEnabled(): Boolean {
        return preferenceLocalDataSource.isEmojiBatteryEnabled()
    }

    override fun setEmojiBatteryEnabled(enabled: Boolean) {
        preferenceLocalDataSource.setEmojiBatteryEnabled(enabled)
        notifySettingsChanged()
    }

    override fun getAppliedThemeDrawableRes(): Int {
        return preferenceLocalDataSource.getAppliedThemeDrawableRes()
    }

    override fun setAppliedThemeDrawableRes(res: Int) {
        preferenceLocalDataSource.setAppliedThemeDrawableRes(res)
        notifySettingsChanged()
    }

    override fun isAppliedThemeGradient(): Boolean {
        return preferenceLocalDataSource.isAppliedThemeGradient()
    }

    override fun setAppliedThemeGradient(isGradient: Boolean) {
        preferenceLocalDataSource.setAppliedThemeGradient(isGradient)
        notifySettingsChanged()
    }

    override fun getAppliedMascotDrawableRes(): Int {
        return preferenceLocalDataSource.getAppliedMascotDrawableRes()
    }

    override fun setAppliedMascotDrawableRes(res: Int) {
        preferenceLocalDataSource.setAppliedMascotDrawableRes(res)
        notifySettingsChanged()
    }

    override fun getCustomStatusBarHeight(): Int {
        return preferenceLocalDataSource.getCustomStatusBarHeight()
    }

    override fun setCustomStatusBarHeight(heightDp: Int) {
        preferenceLocalDataSource.setCustomStatusBarHeight(heightDp)
        notifySettingsChanged()
    }

    override fun getCustomLeftMargin(): Int {
        return preferenceLocalDataSource.getCustomLeftMargin()
    }

    override fun setCustomLeftMargin(marginDp: Int) {
        preferenceLocalDataSource.setCustomLeftMargin(marginDp)
        notifySettingsChanged()
    }

    override fun getCustomRightMargin(): Int {
        return preferenceLocalDataSource.getCustomRightMargin()
    }

    override fun setCustomRightMargin(marginDp: Int) {
        preferenceLocalDataSource.setCustomRightMargin(marginDp)
        notifySettingsChanged()
    }

    override fun getCustomBackgroundColor(): Int {
        return preferenceLocalDataSource.getCustomBackgroundColor()
    }

    override fun setCustomBackgroundColor(color: Int) {
        preferenceLocalDataSource.setCustomBackgroundColor(color)
        notifySettingsChanged()
    }

    override fun getCustomIconColor(): Int {
        return preferenceLocalDataSource.getCustomIconColor()
    }

    override fun setCustomIconColor(color: Int) {
        preferenceLocalDataSource.setCustomIconColor(color)
        notifySettingsChanged()
    }

    override fun getCustomBatteryTextSize(): Int {
        return preferenceLocalDataSource.getCustomBatteryTextSize()
    }

    override fun setCustomBatteryTextSize(sizeSp: Int) {
        preferenceLocalDataSource.setCustomBatteryTextSize(sizeSp)
        notifySettingsChanged()
    }

    override fun getCustomBatteryStyle(): String {
        return preferenceLocalDataSource.getCustomBatteryStyle()
    }

    override fun setCustomBatteryStyle(style: String) {
        preferenceLocalDataSource.setCustomBatteryStyle(style)
        notifySettingsChanged()
    }

    override fun getAppliedBatteryIconRes(): Int {
        return preferenceLocalDataSource.getAppliedBatteryIconRes()
    }

    override fun setAppliedBatteryIconRes(res: Int) {
        preferenceLocalDataSource.setAppliedBatteryIconRes(res)
        notifySettingsChanged()
    }

    override fun isBatteryIconTintable(): Boolean {
        return preferenceLocalDataSource.isBatteryIconTintable()
    }

    override fun setBatteryIconTintable(isTintable: Boolean) {
        preferenceLocalDataSource.setBatteryIconTintable(isTintable)
        notifySettingsChanged()
    }

    override fun getCustomBatteryIconSize(): Int {
        return preferenceLocalDataSource.getCustomBatteryIconSize()
    }

    override fun setCustomBatteryIconSize(sizeDp: Int) {
        preferenceLocalDataSource.setCustomBatteryIconSize(sizeDp)
        notifySettingsChanged()
    }

    override fun getEnabledCustomIcons(): Set<String> {
        return preferenceLocalDataSource.getEnabledCustomIcons()
    }

    override fun setEnabledCustomIcons(icons: Set<String>) {
        preferenceLocalDataSource.setEnabledCustomIcons(icons)
        notifySettingsChanged()
    }

    override fun isCustomIconEnabled(iconId: String): Boolean {
        return preferenceLocalDataSource.isCustomIconEnabled(iconId)
    }

    override fun setCustomIconEnabled(iconId: String, enabled: Boolean) {
        preferenceLocalDataSource.setCustomIconEnabled(iconId, enabled)
        notifySettingsChanged()
    }

    override fun getCustomIconSize(iconId: String): Int {
        return preferenceLocalDataSource.getCustomIconSize(iconId)
    }

    override fun setCustomIconSize(iconId: String, sizeDp: Int) {
        preferenceLocalDataSource.setCustomIconSize(iconId, sizeDp)
        notifySettingsChanged()
    }

    override fun getCustomIconColor(iconId: String): Int {
        return preferenceLocalDataSource.getCustomIconColor(iconId)
    }

    override fun setCustomIconColor(iconId: String, color: Int) {
        preferenceLocalDataSource.setCustomIconColor(iconId, color)
        notifySettingsChanged()
    }

    override fun getEmotionStyle(): String {
        return preferenceLocalDataSource.getEmotionStyle()
    }

    override fun setEmotionStyle(style: String) {
        preferenceLocalDataSource.setEmotionStyle(style)
        notifySettingsChanged()
    }

    override fun getDateFormat(): String {
        return preferenceLocalDataSource.getDateFormat()
    }

    override fun setDateFormat(format: String) {
        preferenceLocalDataSource.setDateFormat(format)
        notifySettingsChanged()
    }

    override fun getDateStyle(): Int {
        return preferenceLocalDataSource.getDateStyle()
    }

    override fun setDateStyle(style: Int) {
        preferenceLocalDataSource.setDateStyle(style)
        notifySettingsChanged()
    }

    override fun getLastAppliedThemeMode(): String {
        return preferenceLocalDataSource.getLastAppliedThemeMode()
    }

    override fun setLastAppliedThemeMode(mode: String) {
        preferenceLocalDataSource.setLastAppliedThemeMode(mode)
        notifySettingsChanged()
    }

    override fun getLeftStatusEmojiRes(): Int {
        return preferenceLocalDataSource.getLeftStatusEmojiRes()
    }

    override fun setLeftStatusEmojiRes(res: Int) {
        preferenceLocalDataSource.setLeftStatusEmojiRes(res)
        notifySettingsChanged()
    }

    override fun getAppliedAnimationRawRes(): Int {
        return preferenceLocalDataSource.getAppliedAnimationRawRes()
    }

    override fun setAppliedAnimationRawRes(res: Int) {
        preferenceLocalDataSource.setAppliedAnimationRawRes(res)
        notifySettingsChanged()
    }

    override fun isAppliedAnimationLottie(): Boolean {
        return preferenceLocalDataSource.isAppliedAnimationLottie()
    }

    override fun setAppliedAnimationLottie(isLottie: Boolean) {
        preferenceLocalDataSource.setAppliedAnimationLottie(isLottie)
        notifySettingsChanged()
    }

    override fun getMascotSize(): Int {
        return preferenceLocalDataSource.getMascotSize()
    }

    override fun setMascotSize(sizeDp: Int) {
        preferenceLocalDataSource.setMascotSize(sizeDp)
        notifySettingsChanged()
    }

    override fun isFloatingStickerEnabled(): Boolean {
        return preferenceLocalDataSource.isFloatingStickerEnabled()
    }

    override fun setFloatingStickerEnabled(enabled: Boolean) {
        preferenceLocalDataSource.setFloatingStickerEnabled(enabled)
    }

    override fun getFloatingStickerDrawableRes(): Int {
        return preferenceLocalDataSource.getFloatingStickerDrawableRes()
    }

    override fun setFloatingStickerDrawableRes(res: Int) {
        preferenceLocalDataSource.setFloatingStickerDrawableRes(res)
    }

    override fun getFloatingStickerX(): Int {
        return preferenceLocalDataSource.getFloatingStickerX()
    }

    override fun setFloatingStickerX(x: Int) {
        preferenceLocalDataSource.setFloatingStickerX(x)
    }

    override fun getFloatingStickerY(): Int {
        return preferenceLocalDataSource.getFloatingStickerY()
    }

    override fun setFloatingStickerY(y: Int) {
        preferenceLocalDataSource.setFloatingStickerY(y)
    }

    override fun getFloatingStickerSize(): Int {
        return preferenceLocalDataSource.getFloatingStickerSize()
    }

    override fun setFloatingStickerSize(sizeDp: Int) {
        preferenceLocalDataSource.setFloatingStickerSize(sizeDp)
    }

    override fun getFloatingStickerRotation(): Float {
        return preferenceLocalDataSource.getFloatingStickerRotation()
    }

    override fun setFloatingStickerRotation(rotDeg: Float) {
        preferenceLocalDataSource.setFloatingStickerRotation(rotDeg)
    }
}
