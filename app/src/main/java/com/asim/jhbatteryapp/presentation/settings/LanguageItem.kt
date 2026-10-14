package com.asim.jhbatteryapp.presentation.settings

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagRes: Int,
    var isSelected: Boolean = false
)
