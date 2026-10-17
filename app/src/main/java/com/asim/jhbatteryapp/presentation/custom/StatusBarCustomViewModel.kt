package com.asim.jhbatteryapp.presentation.custom

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.datasource.BatteryLocalDataSource
import com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.repository.PreferenceRepository

class StatusBarCustomViewModel(
    val preferenceRepository: PreferenceRepository = PreferenceRepositoryImpl(),
    private val batteryDataSource: BatteryLocalDataSource = BatteryLocalDataSource()
) : ViewModel() {

    enum class CustomTab {
        SIZE, APPEARANCE, BATTERY, CUSTOMIZE_ICON
    }

    enum class AppearanceSubTab {
        ICON, BACKGROUND
    }

    private val _currentTab = MutableLiveData(CustomTab.SIZE)
    val currentTab: LiveData<CustomTab> = _currentTab

    private val _appearanceSubTab = MutableLiveData(AppearanceSubTab.BACKGROUND)
    val appearanceSubTab: LiveData<AppearanceSubTab> = _appearanceSubTab

    private val _lastAppliedThemeMode = MutableLiveData(preferenceRepository.getLastAppliedThemeMode())
    val lastAppliedThemeMode: LiveData<String> = _lastAppliedThemeMode

    private val _appliedThemeDrawableRes = MutableLiveData(preferenceRepository.getAppliedThemeDrawableRes())
    val appliedThemeDrawableRes: LiveData<Int> = _appliedThemeDrawableRes

    private val _isAppliedThemeGradient = MutableLiveData(preferenceRepository.isAppliedThemeGradient())
    val isAppliedThemeGradient: LiveData<Boolean> = _isAppliedThemeGradient

    private val _heightDp = MutableLiveData(preferenceRepository.getCustomStatusBarHeight())
    val heightDp: LiveData<Int> = _heightDp

    private val _leftMarginDp = MutableLiveData(preferenceRepository.getCustomLeftMargin())
    val leftMarginDp: LiveData<Int> = _leftMarginDp

    private val _rightMarginDp = MutableLiveData(preferenceRepository.getCustomRightMargin())
    val rightMarginDp: LiveData<Int> = _rightMarginDp

    private val _backgroundColor = MutableLiveData(
        if (preferenceRepository.getCustomBackgroundColor() != 0) {
            preferenceRepository.getCustomBackgroundColor()
        } else {
            Color.parseColor("#F72585")
        }
    )
    val backgroundColor: LiveData<Int> = _backgroundColor

    private val _iconColor = MutableLiveData(preferenceRepository.getCustomIconColor())
    val iconColor: LiveData<Int> = _iconColor

    private val _batteryPercentageSizeSp = MutableLiveData(preferenceRepository.getCustomBatteryTextSize())
    val batteryPercentageSizeSp: LiveData<Int> = _batteryPercentageSizeSp

    private val _batteryIconSizeDp = MutableLiveData(preferenceRepository.getCustomBatteryIconSize())
    val batteryIconSizeDp: LiveData<Int> = _batteryIconSizeDp

    private val _selectedBatteryStyle = MutableLiveData(preferenceRepository.getCustomBatteryStyle())
    val selectedBatteryStyle: LiveData<String> = _selectedBatteryStyle

    private val _selectedBatteryIconRes = MutableLiveData(preferenceRepository.getAppliedBatteryIconRes())
    val selectedBatteryIconRes: LiveData<Int> = _selectedBatteryIconRes

    private val _isBatteryIconTintable = MutableLiveData(preferenceRepository.isBatteryIconTintable())
    val isBatteryIconTintable: LiveData<Boolean> = _isBatteryIconTintable

    private val _enabledCustomIcons = MutableLiveData<Set<String>>(preferenceRepository.getEnabledCustomIcons())
    val enabledCustomIcons: LiveData<Set<String>> = _enabledCustomIcons

    private val _selectedMascotRes = MutableLiveData(preferenceRepository.getAppliedMascotDrawableRes())
    val selectedMascotRes: LiveData<Int> = _selectedMascotRes

    private val _colorSwatches = MutableLiveData<List<ColorSwatchItem>>()
    val colorSwatches: LiveData<List<ColorSwatchItem>> = _colorSwatches

    private val _batteryStyles = MutableLiveData<List<BatteryStyleItem>>()
    val batteryStyles: LiveData<List<BatteryStyleItem>> = _batteryStyles

    private val _statusIcons = MutableLiveData<List<CustomStatusIconItem>>()
    val statusIcons: LiveData<List<CustomStatusIconItem>> = _statusIcons

    private val _batteryMascots = MutableLiveData<List<BatteryEmojiItem>>()
    val batteryMascots: LiveData<List<BatteryEmojiItem>> = _batteryMascots

    private val _selectedLeftEmojiRes = MutableLiveData(preferenceRepository.getLeftStatusEmojiRes())
    val selectedLeftEmojiRes: LiveData<Int> = _selectedLeftEmojiRes

    private val _leftStatusEmojis = MutableLiveData<List<CustomStatusEmojiItem>>()
    val leftStatusEmojis: LiveData<List<CustomStatusEmojiItem>> = _leftStatusEmojis

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // Color Swatches matching snippet
        _colorSwatches.value = listOf(
            ColorSwatchItem("eyedropper", 0, isEyedropper = true),
            ColorSwatchItem("black", Color.parseColor("#181926")),
            ColorSwatchItem("white", Color.WHITE),
            ColorSwatchItem("pink", Color.parseColor("#F72585")),
            ColorSwatchItem("blue", Color.parseColor("#2A86FF")),
            ColorSwatchItem("purple", Color.parseColor("#7B2CBF")),
            ColorSwatchItem("green", Color.parseColor("#38D39F")),
            ColorSwatchItem("orange", Color.parseColor("#FF7A00"))
        )

        // Battery Styles
        _batteryStyles.value = listOf(
            BatteryStyleItem("bolt", R.drawable.ic_battery_style_bolt, isTintable = true),
            BatteryStyleItem("pill", R.drawable.ic_battery_style_pill, isTintable = true),
            BatteryStyleItem("outline", R.drawable.ic_battery_style_outline, isTintable = true),
            BatteryStyleItem("circle", R.drawable.ic_battery_style_circle, isTintable = true),
            BatteryStyleItem("standard", R.drawable.ic_status_battery, isTintable = true),
            BatteryStyleItem("battery1", R.drawable.battery1, isTintable = false),
            BatteryStyleItem("battery2", R.drawable.battery2, isTintable = false),
            BatteryStyleItem("battery3", R.drawable.battery3, isTintable = false),
            BatteryStyleItem("battery4", R.drawable.battery4, isTintable = false),
            BatteryStyleItem("battery5", R.drawable.battery5, isTintable = false),
            BatteryStyleItem("battery6", R.drawable.battery6, isTintable = false)
        )

        // 10 Status Bar Icons from snippet
        _statusIcons.value = listOf(
            CustomStatusIconItem("airplane", "Airplane", R.drawable.ic_airplane),
            CustomStatusIconItem("ringer", "Ringer", R.drawable.ic_ringer_silent),
            CustomStatusIconItem("calendar", "Date & Time", R.drawable.ic_calendar),
            CustomStatusIconItem("hotspot", "Hotspot", R.drawable.ic_hotspot),
            CustomStatusIconItem("emotion", "Emotion", R.drawable.ic_emotion),
            CustomStatusIconItem("wifi", "Wi-Fi", R.drawable.ic_status_wifi),
            CustomStatusIconItem("signal", "SIM / Signal", R.drawable.ic_status_signal),
            CustomStatusIconItem("data", "Data", R.drawable.ic_data_swap),
            CustomStatusIconItem("animation", "Animation", R.drawable.ic_sparkle),
            CustomStatusIconItem("charge", "Charge", R.drawable.ic_status_bolt)
        )

        // Left Status Emojis (shows beside time on the left side of status bar)
        _leftStatusEmojis.value = listOf(
            CustomStatusEmojiItem("none", "None", 0, isNone = true),
            CustomStatusEmojiItem("emojie1", "Emoji 1", R.drawable.emojie1),
            CustomStatusEmojiItem("emojie2", "Emoji 2", R.drawable.emojie2),
            CustomStatusEmojiItem("emojie3", "Emoji 3", R.drawable.emojie3),
            CustomStatusEmojiItem("emojie4", "Emoji 4", R.drawable.emojie4),
            CustomStatusEmojiItem("emojie6", "Emoji 6", R.drawable.emojie6),
            CustomStatusEmojiItem("emojie7", "Emoji 7", R.drawable.emojie7),
            CustomStatusEmojiItem("emojie8", "Emoji 8", R.drawable.emojie8),
            CustomStatusEmojiItem("emojie9", "Emoji 9", R.drawable.emojie9),
            CustomStatusEmojiItem("bunny", "Bunny", R.drawable.ic_blueprint_bunny_blue),
            CustomStatusEmojiItem("girl", "Blossom", R.drawable.ic_blueprint_girl_pink),
            CustomStatusEmojiItem("cat", "Cat", R.drawable.ic_blueprint_cat_gray),
            CustomStatusEmojiItem("frog", "Frog", R.drawable.ic_blueprint_frog_green),
            CustomStatusEmojiItem("stitch", "Stitch", R.drawable.ic_blueprint_stitch_yellow),
            CustomStatusEmojiItem("melody", "Melody", R.drawable.poke),
            CustomStatusEmojiItem("puppy", "Puppy", R.drawable.poke_imojee),
            CustomStatusEmojiItem("bear", "Bear", R.drawable.ic_blueprint_bear_sleeping),
            CustomStatusEmojiItem("love", "Love", R.drawable.ic_reaction_love),
            CustomStatusEmojiItem("happy", "Happy", R.drawable.ic_reaction_happy),
            CustomStatusEmojiItem("sad", "Sad", R.drawable.ic_reaction_sad),
            CustomStatusEmojiItem("sparkle", "Sparkle", R.drawable.ic_sparkle),
            CustomStatusEmojiItem("star", "Star", R.drawable.ic_star_twinkle),
            CustomStatusEmojiItem("heart", "Heart", R.drawable.ic_heart),
            CustomStatusEmojiItem("emoji", "Emoji", R.drawable.ic_emojee)
        )

        // Mascot emojis
        _batteryMascots.value = batteryDataSource.getBatteryEmojis()
    }

    fun setTab(tab: CustomTab) {
        _currentTab.value = tab
    }

    fun setAppearanceSubTab(subTab: AppearanceSubTab) {
        _appearanceSubTab.value = subTab
    }

    fun setHeight(height: Int) {
        _heightDp.value = height
    }

    fun setLeftMargin(margin: Int) {
        _leftMarginDp.value = margin
    }

    fun setRightMargin(margin: Int) {
        _rightMarginDp.value = margin
    }

    fun selectColor(swatch: ColorSwatchItem) {
        if (swatch.isEyedropper) {
            // Default bright gradient color
            if (_appearanceSubTab.value == AppearanceSubTab.BACKGROUND) {
                _backgroundColor.value = Color.parseColor("#F72585")
                _lastAppliedThemeMode.value = "custom"
            } else {
                _iconColor.value = Color.WHITE
            }
            return
        }

        if (_appearanceSubTab.value == AppearanceSubTab.BACKGROUND) {
            _backgroundColor.value = swatch.colorInt
            _lastAppliedThemeMode.value = "custom"
        } else {
            _iconColor.value = swatch.colorInt
        }
    }

    fun setBatteryPercentageSize(sizeSp: Int) {
        _batteryPercentageSizeSp.value = sizeSp
    }

    fun setBatteryIconSize(sizeDp: Int) {
        _batteryIconSizeDp.value = sizeDp
    }

    fun selectBatteryIcon(drawableRes: Int, isTintable: Boolean) {
        _selectedBatteryIconRes.value = drawableRes
        _isBatteryIconTintable.value = isTintable
    }

    fun selectBatteryStyle(styleItem: BatteryStyleItem) {
        _selectedBatteryStyle.value = styleItem.id
        selectBatteryIcon(styleItem.drawableRes, isTintable = styleItem.isTintable)
    }

    fun selectMascot(mascot: BatteryEmojiItem) {
        _selectedMascotRes.value = mascot.previewDrawableRes
        // Applied directly as the battery icon!
        selectBatteryIcon(mascot.previewDrawableRes, isTintable = false)
    }

    fun selectLeftEmoji(emojiItem: CustomStatusEmojiItem) {
        val res = if (emojiItem.isNone) 0 else emojiItem.drawableRes
        _selectedLeftEmojiRes.value = res
        // Removed immediate repository saves to allow "Apply" button logic
    }

    fun refreshEnabledCustomIcons() {
        _enabledCustomIcons.value = preferenceRepository.getEnabledCustomIcons()
        _selectedLeftEmojiRes.value = preferenceRepository.getLeftStatusEmojiRes()
    }

    fun toggleCustomIcon(iconId: String) {
        val current = _enabledCustomIcons.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(iconId)) {
            current.remove(iconId)
        } else {
            current.add(iconId)
        }
        _enabledCustomIcons.value = current
    }

    fun applyCustomizations() {
        val currentSettings = preferenceRepository.getStatusBarSettings()
        val leftEmoji = _selectedLeftEmojiRes.value ?: currentSettings.leftStatusEmojiRes
        val mode = _lastAppliedThemeMode.value ?: "custom"
        val updated = currentSettings.copy(
            heightDp = _heightDp.value ?: currentSettings.heightDp,
            leftMarginDp = _leftMarginDp.value ?: currentSettings.leftMarginDp,
            rightMarginDp = _rightMarginDp.value ?: currentSettings.rightMarginDp,
            backgroundColor = _backgroundColor.value ?: currentSettings.backgroundColor,
            iconColor = _iconColor.value ?: currentSettings.iconColor,
            batteryTextSizeSp = _batteryPercentageSizeSp.value ?: currentSettings.batteryTextSizeSp,
            batteryIconSizeDp = _batteryIconSizeDp.value ?: currentSettings.batteryIconSizeDp,
            batteryStyle = _selectedBatteryStyle.value ?: currentSettings.batteryStyle,
            appliedMascotDrawableRes = if (leftEmoji != 0) leftEmoji else currentSettings.appliedMascotDrawableRes,
            appliedBatteryIconRes = _selectedBatteryIconRes.value ?: currentSettings.appliedBatteryIconRes,
            isBatteryIconTintable = _isBatteryIconTintable.value ?: currentSettings.isBatteryIconTintable,
            enabledCustomIcons = _enabledCustomIcons.value ?: currentSettings.enabledCustomIcons,
            leftStatusEmojiRes = leftEmoji,
            lastAppliedThemeMode = mode
        )
        preferenceRepository.updateStatusBarSettings(updated)
        preferenceRepository.setLeftStatusEmojiRes(leftEmoji)
        if (leftEmoji != 0) {
            preferenceRepository.setAppliedMascotDrawableRes(leftEmoji)
        }
        preferenceRepository.setLastAppliedThemeMode(mode)
    }
}
