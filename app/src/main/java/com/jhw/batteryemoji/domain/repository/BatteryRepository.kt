package com.jhw.batteryemoji.domain.repository

import com.jhw.batteryemoji.domain.model.BatteryEmojiItem
import com.jhw.batteryemoji.domain.model.CategoryItem
import com.jhw.batteryemoji.domain.model.OnboardingPageItem
import com.jhw.batteryemoji.domain.model.QuickActionItem

interface BatteryRepository {
    fun getCategories(): List<CategoryItem>
    fun getBatteryEmojis(categoryId: String? = null): List<BatteryEmojiItem>
    fun getQuickActions(): List<QuickActionItem>
    fun searchEmojis(query: String): List<BatteryEmojiItem>
    fun getStatusBarGradients(): List<com.jhw.batteryemoji.domain.model.StatusBarThemeItem>
    fun getStatusBarBackgrounds(): List<com.jhw.batteryemoji.domain.model.StatusBarThemeItem>
    fun getStatusBarAnimations(): List<BatteryEmojiItem>
    fun getStickers(category: String? = null): List<com.jhw.batteryemoji.domain.model.StickerItem>
}

