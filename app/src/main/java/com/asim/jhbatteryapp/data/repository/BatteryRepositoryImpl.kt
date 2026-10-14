package com.asim.jhbatteryapp.data.repository

import com.asim.jhbatteryapp.data.datasource.BatteryLocalDataSource
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.model.CategoryItem
import com.asim.jhbatteryapp.domain.model.OnboardingPageItem
import com.asim.jhbatteryapp.domain.model.QuickActionItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository

class BatteryRepositoryImpl(
    private val localDataSource: BatteryLocalDataSource
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

    override fun getOnboardingPages(): List<OnboardingPageItem> {
        return localDataSource.getOnboardingPages()
    }

    override fun searchEmojis(query: String): List<BatteryEmojiItem> {
        return localDataSource.searchEmojis(query)
    }
}
