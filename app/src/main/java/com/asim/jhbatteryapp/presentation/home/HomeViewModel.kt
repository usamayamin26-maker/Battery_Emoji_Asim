package com.asim.jhbatteryapp.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.asim.jhbatteryapp.data.repository.BatteryRepositoryImpl
import com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.model.CategoryItem
import com.asim.jhbatteryapp.domain.model.QuickActionItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository
import com.asim.jhbatteryapp.domain.repository.PreferenceRepository

class HomeViewModel(
    private val batteryRepository: BatteryRepository = BatteryRepositoryImpl(),
    private val preferenceRepository: PreferenceRepository = PreferenceRepositoryImpl()
) : ViewModel() {

    private val _categories = MutableLiveData<List<CategoryItem>>()
    val categories: LiveData<List<CategoryItem>> = _categories

    private val _emojis = MutableLiveData<List<BatteryEmojiItem>>()
    val emojis: LiveData<List<BatteryEmojiItem>> = _emojis

    private val _quickActions = MutableLiveData<List<QuickActionItem>>()
    val quickActions: LiveData<List<QuickActionItem>> = _quickActions

    private val _isEmojiBatteryEnabled = MutableLiveData<Boolean>()
    val isEmojiBatteryEnabled: LiveData<Boolean> = _isEmojiBatteryEnabled

    private val _isSearchMode = MutableLiveData<Boolean>(false)
    val isSearchMode: LiveData<Boolean> = _isSearchMode

    private val _recentEmojis = MutableLiveData<List<BatteryEmojiItem>>()
    val recentEmojis: LiveData<List<BatteryEmojiItem>> = _recentEmojis

    private val _topPickEmojis = MutableLiveData<List<BatteryEmojiItem>>()
    val topPickEmojis: LiveData<List<BatteryEmojiItem>> = _topPickEmojis

    private var currentCategoryId: String = "hot"

    init {
        loadData()
    }

    private fun loadData() {
        _categories.value = batteryRepository.getCategories()
        _quickActions.value = batteryRepository.getQuickActions()
        _isEmojiBatteryEnabled.value = preferenceRepository.isEmojiBatteryEnabled()

        val all = batteryRepository.getBatteryEmojis()
        _emojis.value = all
        _recentEmojis.value = all.filter { it.isRecent }
        _topPickEmojis.value = all.filter { it.isTopPick }
    }

    fun selectCategory(categoryId: String) {
        currentCategoryId = categoryId
        _emojis.value = batteryRepository.getBatteryEmojis(categoryId)
    }

    fun toggleEmojiBattery() {
        val newState = !(_isEmojiBatteryEnabled.value ?: true)
        preferenceRepository.setEmojiBatteryEnabled(newState)
        _isEmojiBatteryEnabled.value = newState
    }

    fun refreshEmojiBatteryState() {
        val current = preferenceRepository.isEmojiBatteryEnabled()
        if (_isEmojiBatteryEnabled.value != current) {
            _isEmojiBatteryEnabled.value = current
        }
    }

    fun setSearchMode(enabled: Boolean) {
        _isSearchMode.value = enabled
        if (!enabled) {
            selectCategory(currentCategoryId)
        }
    }

    fun search(query: String) {
        val results = batteryRepository.searchEmojis(query)
        _emojis.value = results
        _recentEmojis.value = results.filter { it.isRecent }
        _topPickEmojis.value = results.filter { it.isTopPick }
    }
}
