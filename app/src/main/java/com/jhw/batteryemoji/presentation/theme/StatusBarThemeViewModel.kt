package com.jhw.batteryemoji.presentation.theme

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhw.batteryemoji.core.Utils.AssetConfig
import com.jhw.batteryemoji.data.repository.BatteryRepositoryImpl
import com.jhw.batteryemoji.data.repository.PreferenceRepositoryImpl
import com.jhw.batteryemoji.data.repository.RemoteAssetRepositoryImpl
import com.jhw.batteryemoji.domain.model.AssetManifest
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import com.jhw.batteryemoji.domain.model.StatusBarThemeItem
import com.jhw.batteryemoji.domain.repository.BatteryRepository
import com.jhw.batteryemoji.domain.repository.PreferenceRepository
import com.jhw.batteryemoji.domain.repository.RemoteAssetRepository
import com.jhw.batteryemoji.presentation.remote.RemoteAssetUiState
import kotlinx.coroutines.launch

class StatusBarThemeViewModel(
    private val repository: BatteryRepository = BatteryRepositoryImpl(),
    private val preferenceRepository: PreferenceRepository = PreferenceRepositoryImpl(),
    private val remoteAssetRepository: RemoteAssetRepository = RemoteAssetRepositoryImpl()
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

    // Remote themes
    private val _remoteThemeState = MutableLiveData<RemoteAssetUiState>(RemoteAssetUiState.Loading)
    val remoteThemeState: LiveData<RemoteAssetUiState> = _remoteThemeState

    private val _remoteGradientThemes = MutableLiveData<List<RemoteAssetItem>>(emptyList())
    val remoteGradientThemes: LiveData<List<RemoteAssetItem>> = _remoteGradientThemes

    private val _remoteBackgroundThemes = MutableLiveData<List<RemoteAssetItem>>(emptyList())
    val remoteBackgroundThemes: LiveData<List<RemoteAssetItem>> = _remoteBackgroundThemes

    private val _selectedRemoteTheme = MutableLiveData<RemoteAssetItem?>(null)
    val selectedRemoteTheme: LiveData<RemoteAssetItem?> = _selectedRemoteTheme

    init {
        loadThemes()
        loadRemoteThemes()
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
            _selectedRemoteTheme.value = null
            _selectedTheme.value = it
        }
    }

    fun selectTheme(item: StatusBarThemeItem) {
        _selectedRemoteTheme.value = null
        _selectedTheme.value = item
    }

    fun selectRemoteTheme(item: RemoteAssetItem) {
        _selectedRemoteTheme.value = item
    }

    fun applyTheme(theme: StatusBarThemeItem) {
        preferenceRepository.setAppliedThemePath("")
        preferenceRepository.setAppliedThemeDrawableRes(theme.drawableRes)
        preferenceRepository.setAppliedThemeGradient(theme.isGradient)
        preferenceRepository.setLastAppliedThemeMode("theme")
        val current = preferenceRepository.getStatusBarSettings()
        val updated = current.copy(
            appliedThemePath = "",
            appliedThemeDrawableRes = theme.drawableRes,
            isAppliedThemeGradient = theme.isGradient,
            lastAppliedThemeMode = "theme"
        )
        preferenceRepository.updateStatusBarSettings(updated)
    }

    fun applyRemoteTheme(path: String, isGradient: Boolean) {
        preferenceRepository.setAppliedThemePath(path)
        preferenceRepository.setAppliedThemeDrawableRes(0)
        preferenceRepository.setAppliedThemeGradient(isGradient)
        preferenceRepository.setLastAppliedThemeMode("theme")
        val current = preferenceRepository.getStatusBarSettings()
        val updated = current.copy(
            appliedThemePath = path,
            appliedThemeDrawableRes = 0,
            isAppliedThemeGradient = isGradient,
            lastAppliedThemeMode = "theme"
        )
        preferenceRepository.updateStatusBarSettings(updated)
    }

    fun loadRemoteThemes() {
        viewModelScope.launch {
            val cached = remoteAssetRepository.loadCachedManifest()
            if (cached != null) {
                showRemoteThemes(cached, isFromCache = true)
            } else {
                _remoteThemeState.value = RemoteAssetUiState.Loading
            }
            remoteAssetRepository.fetchManifest().fold(
                onSuccess = { manifest -> showRemoteThemes(manifest, isFromCache = false) },
                onFailure = { error ->
                    val gradients = filterThemes(
                        remoteAssetRepository.getAssetsByType(AssetConfig.Type.THEME) +
                                remoteAssetRepository.getAssetsByType(AssetConfig.Type.GRADIENT),
                        isGradient = true
                    )
                    val backgrounds = filterThemes(
                        remoteAssetRepository.getAssetsByType(AssetConfig.Type.THEME) +
                                remoteAssetRepository.getAssetsByType(AssetConfig.Type.BACKGROUND),
                        isGradient = false
                    )
                    val allThemes = gradients + backgrounds
                    _remoteThemeState.value = if (allThemes.isNotEmpty()) {
                        RemoteAssetUiState.Offline(allThemes)
                    } else {
                        RemoteAssetUiState.Error(
                            message = error.message ?: "Failed to load remote themes",
                            cause = error
                        )
                    }
                }
            )
        }
    }

    private fun showRemoteThemes(manifest: AssetManifest, isFromCache: Boolean) {
        val allThemeAssets = manifest.assets.filter {
            it.type.equals(AssetConfig.Type.THEME, ignoreCase = true) ||
            it.type.equals(AssetConfig.Type.GRADIENT, ignoreCase = true) ||
            it.type.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true) ||
            it.category.equals(AssetConfig.Type.THEME, ignoreCase = true) ||
            it.category.equals(AssetConfig.Type.GRADIENT, ignoreCase = true) ||
            it.category.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true)
        }

        val gradients = allThemeAssets.filter {
            it.category.equals(AssetConfig.Type.GRADIENT, ignoreCase = true) ||
            it.type.equals(AssetConfig.Type.GRADIENT, ignoreCase = true) ||
            (!it.category.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true) && !it.type.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true))
        }

        val backgrounds = allThemeAssets.filter {
            it.category.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true) ||
            it.type.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true)
        }

        _remoteGradientThemes.value = gradients
        _remoteBackgroundThemes.value = backgrounds

        restoreAppliedRemoteSelection(allThemeAssets)

        _remoteThemeState.value = if (allThemeAssets.isEmpty()) {
            RemoteAssetUiState.Empty
        } else {
            RemoteAssetUiState.Success(allThemeAssets, isFromCache)
        }
    }

    private fun filterThemes(items: List<RemoteAssetItem>, isGradient: Boolean): List<RemoteAssetItem> {
        return if (isGradient) {
            items.filter {
                it.category.equals(AssetConfig.Type.GRADIENT, ignoreCase = true) ||
                it.type.equals(AssetConfig.Type.GRADIENT, ignoreCase = true) ||
                (!it.category.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true) && !it.type.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true))
            }
        } else {
            items.filter {
                it.category.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true) ||
                it.type.equals(AssetConfig.Type.BACKGROUND, ignoreCase = true)
            }
        }
    }

    private fun restoreAppliedRemoteSelection(items: List<RemoteAssetItem>) {
        val appliedPath = preferenceRepository.getAppliedThemePath()
        if (appliedPath.isEmpty()) return
        val appliedId = java.io.File(appliedPath).nameWithoutExtension
        items.find { it.id == appliedId }?.let {
            _selectedRemoteTheme.value = it
        }
    }
}
