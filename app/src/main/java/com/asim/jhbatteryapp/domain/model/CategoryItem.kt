package com.asim.jhbatteryapp.domain.model

import androidx.annotation.StringRes

data class CategoryItem(
    val id: String,
    val title: String,
    val isSelected: Boolean = false,
    /** Translated title; when set it is shown instead of [title]. */
    @StringRes val titleRes: Int = 0
)
