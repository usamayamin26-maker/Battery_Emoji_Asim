package com.jhw.batteryemoji.domain.model

data class QuickActionItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val iconRes: Int,
    val backgroundDrawableRes: Int
)
