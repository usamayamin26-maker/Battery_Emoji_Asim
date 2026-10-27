package com.jhw.batteryemoji.presentation.theme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jhw.batteryemoji.data.repository.BatteryRepositoryImpl
import com.jhw.batteryemoji.data.repository.PreferenceRepositoryImpl
import com.jhw.batteryemoji.domain.model.StatusBarThemeItem
import com.jhw.batteryemoji.domain.repository.BatteryRepository
import com.jhw.batteryemoji.domain.repository.PreferenceRepository

class StatusBarThemeViewModel(
    private val repository: BatteryRepository = BatteryRepositoryImpl(),
    private val preferenceRepository: PreferenceRepository = PreferenceRepositoryImpl()
) : ViewModel() {

    enum class ThemeTab { GRADIENT, BACKGROUND }

    private val _currentTab = MutableLiveData(ThemeTab.GRADIENT)
    val currentTab: LiveData<ThemeTab> = _currentTab

    private val _gradientThemes = MutableLiveData<List<StatusBarThemeItem>>()
    val gradientThemes: LiveData<List<StatusBarThemeItem>> = _gradientThemes

    private val _backgroundThemes = MutableLiveData<List<StatusBarThemeItem>>()
    val backgroundThemes: LiveData<List<StatusBarThemeItem>> = _backgroundThemes

    private val _selectedTheme = MutableLiveData<StatusBarThemeItem>()
    val selectedTheme: LiveData<StatusBarThemeItem> = _selectedTheme

    init {
        loadThemes()
    }

    private fun loadThemes() {
        val gradients = repository.getStatusBarGradients()
        val backgrounds = repository.getStatusBarBackgrounds()

        _gradientThemes.value = gradients
        _backgroundThemes.value = backgrounds

        // Default selection: restored from preference or first gradient
        val savedThemeRes = preferenceRepository.getAppliedThemeDrawableRes()
        val savedItem = gradients.find { it.drawableRes == savedThemeRes }
            ?: backgrounds.find { it.drawableRes == savedThemeRes }
            ?: gradients.firstOrNull()

        savedItem?.let { _selectedTheme.value = it }
    }

    fun setTab(tab: ThemeTab) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab

        val list = if (tab == ThemeTab.GRADIENT) _gradientThemes.value else _backgroundThemes.value
        list?.firstOrNull()?.let {
            _selectedTheme.value = it
        }
    }

    fun selectTheme(item: StatusBarThemeItem) {
        _selectedTheme.value = item
    }

    fun applyTheme(theme: StatusBarThemeItem) {
        val current = preferenceRepository.getStatusBarSettings()
        val updated = current.copy(
            appliedThemeDrawableRes = theme.drawableRes,
            isAppliedThemeGradient = theme.isGradient,
            lastAppliedThemeMode = "theme"
        )
        preferenceRepository.updateStatusBarSettings(updated)
    }
}
