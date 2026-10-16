package com.asim.jhbatteryapp.presentation.sticker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.repository.BatteryRepositoryImpl
import com.asim.jhbatteryapp.domain.model.StickerItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository

class StickerDisplayViewModel(
    private val repository: BatteryRepository = BatteryRepositoryImpl()
) : ViewModel() {

    private val _tabs = MutableLiveData<List<Pair<String, String>>>(
        listOf(
            "hot" to "🔥 HOT",
            "pokemon" to "Pokemon",
            "sanrio" to "Sanrio",
            "anime" to "Anime"
        )
    )
    val tabs: LiveData<List<Pair<String, String>>> = _tabs

    private val _selectedTab = MutableLiveData("hot")
    val selectedTab: LiveData<String> = _selectedTab

    private val _gridStickers = MutableLiveData<List<StickerItem>>()
    val gridStickers: LiveData<List<StickerItem>> = _gridStickers

    private val _myStickers = MutableLiveData<MutableList<StickerItem>>(mutableListOf())
    val myStickers: LiveData<MutableList<StickerItem>> = _myStickers

    private val _activeSticker = MutableLiveData<StickerItem?>()
    val activeSticker: LiveData<StickerItem?> = _activeSticker

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val stickers = repository.getStickers("hot")
        _gridStickers.value = stickers

        // Default initial items in "My Sticker" matching "2/5" in design
        val initialMyStickers = mutableListOf(
            StickerItem(
                id = "my_puppy",
                title = "Puppy Mascot",
                category = "sanrio",
                drawableRes = R.drawable.ic_blueprint_puppy,
                sizeDp = 50,
                rotationDeg = 0f
            ),
            StickerItem(
                id = "my_player_blue",
                title = "Blue Striker",
                category = "hot",
                drawableRes = R.drawable.ic_sticker_player_blue,
                sizeDp = 50,
                rotationDeg = 0f
            )
        )
        _myStickers.value = initialMyStickers
        _activeSticker.value = initialMyStickers.firstOrNull()
    }

    fun selectCategory(categoryKey: String) {
        _selectedTab.value = categoryKey
        _gridStickers.value = repository.getStickers(categoryKey)
    }

    fun setActiveSticker(sticker: StickerItem) {
        _activeSticker.value = sticker
    }

    fun addOrSelectSticker(sticker: StickerItem): Boolean {
        val currentList = _myStickers.value ?: mutableListOf()
        val existing = currentList.find { it.id == sticker.id }
        if (existing != null) {
            _activeSticker.value = existing
            return true
        }

        if (currentList.size >= 5) {
            return false // Max limit reached
        }

        val newItem = sticker.copy(id = "my_${sticker.id}_${System.currentTimeMillis()}")
        currentList.add(newItem)
        _myStickers.value = currentList
        _activeSticker.value = newItem
        return true
    }

    fun removeSticker(stickerId: String) {
        val currentList = _myStickers.value ?: return
        val index = currentList.indexOfFirst { it.id == stickerId }
        if (index != -1) {
            currentList.removeAt(index)
            _myStickers.value = currentList
            if (_activeSticker.value?.id == stickerId) {
                _activeSticker.value = currentList.firstOrNull()
            }
        }
    }

    fun updateActiveStickerAdjustment(offsetX: Float, offsetY: Float, sizeDp: Int, rotationDeg: Float) {
        val active = _activeSticker.value ?: return
        active.offsetX = offsetX
        active.offsetY = offsetY
        active.sizeDp = sizeDp
        active.rotationDeg = rotationDeg

        // Trigger update to observers
        _activeSticker.value = active
        _myStickers.value = _myStickers.value
    }
}
