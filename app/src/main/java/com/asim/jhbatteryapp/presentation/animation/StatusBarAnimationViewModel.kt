package com.asim.jhbatteryapp.presentation.animation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.asim.jhbatteryapp.data.repository.BatteryRepositoryImpl
import com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository
import com.asim.jhbatteryapp.domain.repository.PreferenceRepository

class StatusBarAnimationViewModel(
    private val repository: BatteryRepository = BatteryRepositoryImpl(),
    private val preferenceRepository: PreferenceRepository = PreferenceRepositoryImpl()
) : ViewModel() {

    private val _animations = MutableLiveData<List<BatteryEmojiItem>>()
    val animations: LiveData<List<BatteryEmojiItem>> = _animations

    private val _selectedAnimation = MutableLiveData<BatteryEmojiItem>()
    val selectedAnimation: LiveData<BatteryEmojiItem> = _selectedAnimation

    private val _dateSize = MutableLiveData(50) // Default 50dp
    val dateSize: LiveData<Int> = _dateSize

    private val _isBatteryActive = MutableLiveData(false)
    val isBatteryActive: LiveData<Boolean> = _isBatteryActive

    init {
        loadAnimations()
    }

    private fun loadAnimations() {
        val anims = repository.getStatusBarAnimations()
        _animations.value = anims
        _isBatteryActive.value = preferenceRepository.isEmojiBatteryEnabled()

        // Default to Cake Run or first item
        val currentAppliedAnimRes = preferenceRepository.getAppliedAnimationRawRes()
        val defaultItem = if (currentAppliedAnimRes != 0) {
            anims.find { it.lottieRawRes == currentAppliedAnimRes }
        } else null ?: anims.find { it.id == "anim_cakerun" } ?: anims.firstOrNull()
        defaultItem?.let { _selectedAnimation.value = it }
    }

    fun selectAnimation(item: BatteryEmojiItem) {
        _selectedAnimation.value = item
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
        preferenceRepository.setMascotSize(size)
        preferenceRepository.setCustomIconSize("animation", size)
        preferenceRepository.setCustomIconEnabled("animation", true)
        preferenceRepository.setEmojiBatteryEnabled(true)
        _isBatteryActive.value = true
    }
}
