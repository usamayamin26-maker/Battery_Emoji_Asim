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

        // Default to Pink Blossom (anim_girl) or first mascot as seen in screenshot
        val defaultItem = anims.find { it.id == "anim_girl" } ?: anims.firstOrNull()
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
        preferenceRepository.setAppliedMascotDrawableRes(mascot.previewDrawableRes)
        preferenceRepository.setEmojiBatteryEnabled(true)
        _isBatteryActive.value = true
    }
}
