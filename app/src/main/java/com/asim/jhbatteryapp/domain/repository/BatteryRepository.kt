package com.asim.jhbatteryapp.domain.repository

import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.model.CategoryItem
import com.asim.jhbatteryapp.domain.model.OnboardingPageItem
import com.asim.jhbatteryapp.domain.model.QuickActionItem

interface BatteryRepository {
    fun getCategories(): List<CategoryItem>
    fun getBatteryEmojis(categoryId: String? = null): List<BatteryEmojiItem>
    fun getQuickActions(): List<QuickActionItem>
    fun getOnboardingPages(): List<OnboardingPageItem>
    fun searchEmojis(query: String): List<BatteryEmojiItem>
}
