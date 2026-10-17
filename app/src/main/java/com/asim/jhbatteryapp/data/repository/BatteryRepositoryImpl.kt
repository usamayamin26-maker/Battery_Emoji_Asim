package com.asim.jhbatteryapp.data.repository

import com.asim.jhbatteryapp.data.datasource.BatteryLocalDataSource
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.model.CategoryItem
import com.asim.jhbatteryapp.domain.model.OnboardingPageItem
import com.asim.jhbatteryapp.domain.model.QuickActionItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository

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

    override fun getStatusBarGradients(): List<com.asim.jhbatteryapp.domain.model.StatusBarThemeItem> {
        return localDataSource.getStatusBarGradients()
    }

    override fun getStatusBarBackgrounds(): List<com.asim.jhbatteryapp.domain.model.StatusBarThemeItem> {
        return localDataSource.getStatusBarBackgrounds()
    }

    override fun getStatusBarAnimations(): List<BatteryEmojiItem> {
        return localDataSource.getStatusBarAnimations()
    }

    override fun getStickers(category: String?): List<com.asim.jhbatteryapp.domain.model.StickerItem> {
        return localDataSource.getStickers(category)
    }
}
