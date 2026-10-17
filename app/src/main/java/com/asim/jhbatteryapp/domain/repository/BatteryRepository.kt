package com.asim.jhbatteryapp.domain.repository

import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.model.CategoryItem
import com.asim.jhbatteryapp.domain.model.OnboardingPageItem
import com.asim.jhbatteryapp.domain.model.QuickActionItem

interface BatteryRepository {
    fun getCategories(): List<CategoryItem>
    fun getBatteryEmojis(categoryId: String? = null): List<BatteryEmojiItem>
    fun getQuickActions(): List<QuickActionItem>
    fun searchEmojis(query: String): List<BatteryEmojiItem>
    fun getStatusBarGradients(): List<com.asim.jhbatteryapp.domain.model.StatusBarThemeItem>
    fun getStatusBarBackgrounds(): List<com.asim.jhbatteryapp.domain.model.StatusBarThemeItem>
    fun getStatusBarAnimations(): List<BatteryEmojiItem>
    fun getStickers(category: String? = null): List<com.asim.jhbatteryapp.domain.model.StickerItem>
}

