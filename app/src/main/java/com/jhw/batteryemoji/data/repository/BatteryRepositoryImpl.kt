package com.jhw.batteryemoji.data.repository

import com.jhw.batteryemoji.data.datasource.BatteryLocalDataSource
import com.jhw.batteryemoji.domain.model.BatteryEmojiItem
import com.jhw.batteryemoji.domain.model.CategoryItem
import com.jhw.batteryemoji.domain.model.OnboardingPageItem
import com.jhw.batteryemoji.domain.model.QuickActionItem
import com.jhw.batteryemoji.domain.repository.BatteryRepository

class BatteryRepositoryImpl(
    private val localDataSource: BatteryLocalDataSource = BatteryLocalDataSource()
) : BatteryRepository {

    override fun getCategories(): List<CategoryItem> {
        return localDataSource.getCategories()
    }

    override fun getBatteryEmojis(categoryId: String?): List<BatteryEmojiItem> {
        return localDataSource.getBatteryEmojis(categoryId)
    }

    override fun getQuickActions(): List<QuickActionItem> {
        return localDataSource.getQuickActions()
    }

    override fun searchEmojis(query: String): List<BatteryEmojiItem> {
        return localDataSource.searchEmojis(query)
    }

    override fun getStatusBarGradients(): List<com.jhw.batteryemoji.domain.model.StatusBarThemeItem> {
        return localDataSource.getStatusBarGradients()
    }

    override fun getStatusBarBackgrounds(): List<com.jhw.batteryemoji.domain.model.StatusBarThemeItem> {
        return localDataSource.getStatusBarBackgrounds()
    }

    override fun getStatusBarAnimations(): List<BatteryEmojiItem> {
        return localDataSource.getStatusBarAnimations()
    }

    override fun getStickers(category: String?): List<com.jhw.batteryemoji.domain.model.StickerItem> {
        return localDataSource.getStickers(category)
    }
}
