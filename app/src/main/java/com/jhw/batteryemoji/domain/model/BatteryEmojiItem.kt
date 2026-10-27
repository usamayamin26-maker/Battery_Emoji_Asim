package com.jhw.batteryemoji.domain.model

import com.jhw.batteryemoji.R

data class BatteryEmojiItem(
    val id: String,
    val title: String,
    val category: String = "animation",
    val previewDrawableRes: Int = R.drawable.ic_sparkle,
    val backgroundColorRes: Int = R.color.card_bg_pink,
    val batteryPercentage: Int = 100,
    val batteryColorRes: Int = R.color.battery_pink,
    val isHot: Boolean = false,
    val isRecent: Boolean = false,
    val isTopPick: Boolean = false,
    val tags: List<String> = emptyList(),
    val isLottie: Boolean = false,
    val lottieRawRes: Int = 0
) {
    companion object {
        fun lottie(
            id: String,
            title: String,
            lottieRawRes: Int,
            category: String = "animation"
        ): BatteryEmojiItem = BatteryEmojiItem(
            id = id,
            title = title,
            category = category,
            isLottie = true,
            lottieRawRes = lottieRawRes
        )
    }
}
