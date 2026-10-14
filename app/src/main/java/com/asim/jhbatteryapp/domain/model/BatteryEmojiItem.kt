package com.asim.jhbatteryapp.domain.model

data class BatteryEmojiItem(
    val id: String,
    val title: String,
    val category: String,
    val previewDrawableRes: Int,
    val backgroundColorRes: Int,
    val batteryPercentage: Int = 100,
    val batteryColorRes: Int,
    val isHot: Boolean = false,
    val isRecent: Boolean = false,
    val isTopPick: Boolean = false,
    val tags: List<String> = emptyList()
)
