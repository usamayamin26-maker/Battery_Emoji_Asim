package com.asim.jhbatteryapp.domain.model

import androidx.annotation.DrawableRes

data class StickerItem(
    val id: String,
    val title: String,
    val category: String,
    @field:DrawableRes val drawableRes: Int,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var sizeDp: Int = 50,
    var rotationDeg: Float = 0f
)
