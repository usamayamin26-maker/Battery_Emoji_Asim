package com.jhw.batteryemoji.domain.model

data class StatusBarThemeItem(
    val id: String,
    val title: String,
    val isGradient: Boolean,
    val drawableRes: Int,
    val isSelected: Boolean = false,
    val timeText: String = "20:05",
    val batteryPercent: String = "58%"
)
