package com.jhw.batteryemoji.presentation.animation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhw.batteryemoji.core.Utils.AssetConfig
import com.jhw.batteryemoji.data.repository.BatteryRepositoryImpl
import com.jhw.batteryemoji.data.repository.PreferenceRepositoryImpl
import com.jhw.batteryemoji.data.repository.RemoteAssetRepositoryImpl
import com.jhw.batteryemoji.domain.model.AssetManifest
import com.jhw.batteryemoji.domain.model.BatteryEmojiItem
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import com.jhw.batteryemoji.domain.repository.BatteryRepository
import com.jhw.batteryemoji.domain.repository.PreferenceRepository
import com.jhw.batteryemoji.domain.repository.RemoteAssetRepository
import com.jhw.batteryemoji.presentation.remote.RemoteAssetUiState
import kotlinx.coroutines.launch

class StatusBarAnimationViewModel(
    private val repository: BatteryRepository = BatteryRepositoryImpl(),
    private val preferenceRepository: PreferenceRepository = PreferenceRepositoryImpl(),
    private val remoteAssetRepository: RemoteAssetRepository = RemoteAssetRepositoryImpl()
) : ViewModel() {

    // ── Existing LiveData (unchanged) ──────────────────────────────────────────

    private val _animations = MutableLiveData<List<BatteryEmojiItem>>()
    val animations: LiveData<List<BatteryEmojiItem>> = _animations

    private val _selectedAnimation = MutableLiveData<BatteryEmojiItem>()
    val selectedAnimation: LiveData<BatteryEmojiItem> = _selectedAnimation

    private val _dateSize = MutableLiveData(50) // Default 50dp
    val dateSize: LiveData<Int> = _dateSize

    private val _isBatteryActive = MutableLiveData(false)
    val isBatteryActive: LiveData<Boolean> = _isBatteryActive

    // ── Remote asset LiveData (new) ────────────────────────────────────────────

    private val _remoteAnimationState = MutableLiveData<RemoteAssetUiState>(RemoteAssetUiState.Loading)
    val remoteAnimationState: LiveData<RemoteAssetUiState> = _remoteAnimationState

    private val _remoteAnimations = MutableLiveData<List<RemoteAssetItem>>(emptyList())
    val remoteAnimations: LiveData<List<RemoteAssetItem>> = _remoteAnimations

    /** Non-null while a remote animation (instead of a local one) is selected. */
    private val _selectedRemoteAnimation = MutableLiveData<RemoteAssetItem?>(null)
    val selectedRemoteAnimation: LiveData<RemoteAssetItem?> = _selectedRemoteAnimation

    // ── Init ───────────────────────────────────────────────────────────────────

    init {
        loadAnimations()
        loadRemoteAnimations()
    }

    // ── Existing methods (unchanged) ───────────────────────────────────────────

    private fun loadAnimations() {
        val anims = repository.getStatusBarAnimations()
        _animations.value = anims
        _isBatteryActive.value = preferenceRepository.isEmojiBatteryEnabled()
        // Start the slider at the applied size so re-applying doesn't reset it to the default
        _dateSize.value = preferenceRepository.getStatusBarSettings().mascotSizeDp

        // Default to Cake Run or first item
        val currentAppliedAnimRes = preferenceRepository.getAppliedAnimationRawRes()
        val defaultItem = if (currentAppliedAnimRes != 0) {
            anims.find { it.lottieRawRes == currentAppliedAnimRes }
        } else null ?: anims.find { it.id == "anim_cakerun" } ?: anims.firstOrNull()
        defaultItem?.let { _selectedAnimation.value = it }
    }

    fun selectAnimation(item: BatteryEmojiItem) {
        _selectedRemoteAnimation.value = null
        _selectedAnimation.value = item
    }

    fun selectRemoteAnimation(item: RemoteAssetItem) {
        _selectedRemoteAnimation.value = item
    }

    /** Applies a downloaded animation file; mirrors [applyAnimation] for local items. */
    fun applyRemoteAnimation(path: String) {
        val size = _dateSize.value ?: 50
        preferenceRepository.setAppliedAnimationPath(path)
        // Raw res 0 + lottie flag tells the overlay to play the downloaded file
        preferenceRepository.setAppliedAnimationRawRes(0)
        preferenceRepository.setAppliedAnimationLottie(true)
        preferenceRepository.setAppliedMascotDrawableRes(0)
        preferenceRepository.setLeftStatusEmojiRes(0)
        preferenceRepository.setLeftStatusEmojiPath("")
        preferenceRepository.setMascotSize(size)
        preferenceRepository.setCustomIconSize("animation", size)
        preferenceRepository.setCustomIconEnabled("animation", true)
        preferenceRepository.setEmojiBatteryEnabled(true)
        _isBatteryActive.value = true
    }

    /** Re-selects the applied remote animation (its saved file is named after the asset id). */
    private fun restoreAppliedRemoteSelection(items: List<RemoteAssetItem>) {
        val appliedPath = preferenceRepository.getStatusBarSettings().remoteAnimationPath ?: return
        val appliedId = java.io.File(appliedPath).nameWithoutExtension
        items.find { it.id == appliedId }?.let { _selectedRemoteAnimation.value = it }
    }

    fun selectAnimationById(id: String) {
        _animations.value?.find { it.id == id }?.let {
            _selectedAnimation.value = it
        }
    }

    fun setDateSize(size: Int) {
        _dateSize.value = size
    }

    fun toggleBatteryActive() {
        val newState = !(_isBatteryActive.value ?: false)
        _isBatteryActive.value = newState
        preferenceRepository.setEmojiBatteryEnabled(newState)
    }

    fun applyAnimation(mascot: BatteryEmojiItem) {
        val size = _dateSize.value ?: 50
        if (mascot.isLottie && mascot.lottieRawRes != 0) {
            preferenceRepository.setAppliedAnimationRawRes(mascot.lottieRawRes)
            preferenceRepository.setAppliedAnimationLottie(true)
        } else {
            preferenceRepository.setAppliedAnimationRawRes(0)
            preferenceRepository.setAppliedAnimationLottie(false)
        }
        preferenceRepository.setAppliedMascotDrawableRes(mascot.previewDrawableRes)
        preferenceRepository.setLeftStatusEmojiRes(0)
        preferenceRepository.setLeftStatusEmojiPath("")
        preferenceRepository.setMascotSize(size)
        preferenceRepository.setCustomIconSize("animation", size)
        preferenceRepository.setCustomIconEnabled("animation", true)
        preferenceRepository.setEmojiBatteryEnabled(true)
        _isBatteryActive.value = true
    }

    // ── New remote methods ─────────────────────────────────────────────────────

    /**
     * Fetches remote animations from the manifest.
     * Existing local animation logic is completely unaffected.
     */
    fun loadRemoteAnimations() {
        viewModelScope.launch {
            // Show the manifest saved by an earlier launch right away, then refresh it
            val cached = remoteAssetRepository.loadCachedManifest()
            if (cached != null) {
                showRemoteAnimations(cached, isFromCache = true)
            } else {
                _remoteAnimationState.value = RemoteAssetUiState.Loading
            }
            remoteAssetRepository.fetchManifest().fold(
                onSuccess = { manifest -> showRemoteAnimations(manifest, isFromCache = false) },
                onFailure = { error ->
                    val items = remoteAssetRepository.getAssetsByType(AssetConfig.Type.ANIMATION)
                    _remoteAnimationState.value = if (items.isNotEmpty()) {
                        RemoteAssetUiState.Offline(items)
                    } else {
                        RemoteAssetUiState.Error(
                            message = error.message ?: "Failed to load remote animations",
                            cause = error
                        )
                    }
                }
            )
        }
    }

    private fun showRemoteAnimations(manifest: AssetManifest, isFromCache: Boolean) {
        val items = manifest.getByType(AssetConfig.Type.ANIMATION)
        // Skip identical lists so a background refresh doesn't rebind the grid
        if (_remoteAnimations.value != items) {
            _remoteAnimations.value = items
            restoreAppliedRemoteSelection(items)
        }
        _remoteAnimationState.value = if (items.isEmpty()) {
            RemoteAssetUiState.Empty
        } else {
            RemoteAssetUiState.Success(items, isFromCache)
        }
    }
}
