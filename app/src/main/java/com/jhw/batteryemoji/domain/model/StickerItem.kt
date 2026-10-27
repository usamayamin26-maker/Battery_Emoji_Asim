package com.jhw.batteryemoji.domain.model

import androidx.annotation.DrawableRes

data class StickerItem(
    val id: String,
    val title: String,
    val category: String,
    @field:DrawableRes val drawableRes: Int,
    /** Downloaded file of a remote sticker; when set it is drawn instead of [drawableRes]. */
    val imagePath: String? = null,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    var sizeDp: Int = 50,
    var rotationDeg: Float = 0f
)
