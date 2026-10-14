package com.asim.jhbatteryapp.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.model.CategoryItem
import com.asim.jhbatteryapp.domain.model.QuickActionItem
import com.asim.jhbatteryapp.domain.usecase.GetBatteryEmojisUseCase
import com.asim.jhbatteryapp.domain.usecase.GetCategoriesUseCase
import com.asim.jhbatteryapp.domain.usecase.GetQuickActionsUseCase
import com.asim.jhbatteryapp.domain.usecase.SearchEmojisUseCase
import com.asim.jhbatteryapp.domain.usecase.ToggleEmojiBatteryUseCase

class HomeViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getBatteryEmojisUseCase: GetBatteryEmojisUseCase,
    private val getQuickActionsUseCase: GetQuickActionsUseCase,
    private val searchEmojisUseCase: SearchEmojisUseCase,
    private val toggleEmojiBatteryUseCase: ToggleEmojiBatteryUseCase
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
        _categories.value = getCategoriesUseCase()
        _quickActions.value = getQuickActionsUseCase()
        _isEmojiBatteryEnabled.value = toggleEmojiBatteryUseCase.isEnabled()

        val all = getBatteryEmojisUseCase()
        _emojis.value = all
        _recentEmojis.value = all.filter { it.isRecent }
        _topPickEmojis.value = all.filter { it.isTopPick }
    }

    fun selectCategory(categoryId: String) {
        currentCategoryId = categoryId
        _emojis.value = getBatteryEmojisUseCase(categoryId)
    }

    fun toggleEmojiBattery() {
        val newState = !(_isEmojiBatteryEnabled.value ?: true)
        toggleEmojiBatteryUseCase(newState)
        _isEmojiBatteryEnabled.value = newState
    }

    fun setSearchMode(enabled: Boolean) {
        _isSearchMode.value = enabled
        if (!enabled) {
            selectCategory(currentCategoryId)
        }
    }

    fun search(query: String) {
        val results = searchEmojisUseCase(query)
        _emojis.value = results
        _recentEmojis.value = results.filter { it.isRecent }
        _topPickEmojis.value = results.filter { it.isTopPick }
    }
}
