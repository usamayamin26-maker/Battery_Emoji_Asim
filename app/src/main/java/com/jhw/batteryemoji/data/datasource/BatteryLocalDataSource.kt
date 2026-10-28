package com.jhw.batteryemoji.data.datasource

import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.domain.model.BatteryEmojiItem
import com.jhw.batteryemoji.domain.model.CategoryItem
import com.jhw.batteryemoji.domain.model.OnboardingPageItem
import com.jhw.batteryemoji.domain.model.QuickActionItem
import com.jhw.batteryemoji.domain.model.StatusBarThemeItem

class BatteryLocalDataSource {

    fun getCategories(): List<CategoryItem> {
        return listOf(
            CategoryItem(id = "hot", title = "HOT", isSelected = true, titleRes = R.string.tab_hot),
            CategoryItem(id = "pokemon", title = "Pokemon", isSelected = false, titleRes = R.string.tab_pokemon),
            CategoryItem(id = "sanrio", title = "Sanrio", isSelected = false, titleRes = R.string.tab_sanrio),
            CategoryItem(id = "anime", title = "Cartoons", isSelected = false, titleRes = R.string.tab_anime)
        )
    }

    fun getQuickActions(): List<QuickActionItem> {
        return listOf(
            QuickActionItem(
                id = "sticker_shop",
                title = "Sticker Shop",
                subtitle = "",
                iconRes = R.drawable.ic_blueprint_puppy,
                backgroundDrawableRes = R.drawable.bg_quick_sticker_shop
            ),
            QuickActionItem(
                id = "animation",
                title = "Animation",
                subtitle = "",
                iconRes = R.drawable.ic_sparkle,
                backgroundDrawableRes = R.drawable.bg_quick_animation
            ),
            QuickActionItem(
                id = "themes",
                title = "Themes",
                subtitle = "",
                iconRes = R.drawable.ic_layers,
                backgroundDrawableRes = R.drawable.bg_quick_themes
            )
        )
    }

    fun getBatteryEmojis(categoryId: String? = null): List<BatteryEmojiItem> {
        val allItems = listOf(
            BatteryEmojiItem(
                id = "1",
                title = "Cloud Bunny",
                category = "hot",
                previewDrawableRes = R.drawable.ic_blueprint_bunny_blue,
                backgroundColorRes = R.color.card_bg_blue,
                batteryPercentage = 100,
                batteryColorRes = R.color.battery_blue,
                isHot = true,
                isRecent = true,
                isTopPick = true,
                tags = listOf("cute", "bunny", "blue", "heart")
            ),

//            BatteryEmojiItem(
//                id = "battery2",
//                title = "battery2",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_02,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery3",
//                title = "battery3",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_03,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery4",
//                title = "battery4",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_04,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery5",
//                title = "battery5",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_05,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery6",
//                title = "battery6",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_06,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery7",
//                title = "battery7",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_07,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery8",
//                title = "battery8",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_08,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery9",
//                title = "battery9",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_09,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery10",
//                title = "battery10",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_10,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery11",
//                title = "battery11",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_11,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery12",
//                title = "battery12",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_12,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery13",
//                title = "battery13",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_13,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery14",
//                title = "battery14",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_14,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery15",
//                title = "battery15",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_15,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery16",
//                title = "battery16",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_16,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery17",
//                title = "battery17",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_17,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery18",
//                title = "battery18",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_18,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery19",
//                title = "battery19",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_19,
//                isHot = true,
//                isRecent = true
//            ),
//
//            BatteryEmojiItem(
//                id = "battery20",
//                title = "battery20",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_20,
//                isHot = true,
//                isRecent = true
//            ),

//            BatteryEmojiItem(
//                id = "battery21",
//                title = "battery21",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery1,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery22",
//                title = "Battery 2",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery2,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery23",
//                title = "Battery 3",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery3,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery24",
//                title = "Battery 4",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery4,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery5",
//                title = "Battery 5",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery5,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery26",
//                title = "Battery 6",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery6,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery27",
//                title = "Battery 7",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_21,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery28",
//                title = "Battery 8",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_22,
//                isHot = true,
//                isRecent = true
//            ),
//            BatteryEmojiItem(
//                id = "battery29",
//                title = "Battery 9",
//                category = "pokemon",
//                previewDrawableRes = R.drawable.battery_23,
//                isHot = true,
//                isRecent = true
//            )


        )

        return if (categoryId.isNullOrEmpty() || categoryId == "hot") {
            allItems
        } else {
            allItems.filter { it.category.equals(categoryId, ignoreCase = true) }
        }
    }


    fun searchEmojis(query: String): List<BatteryEmojiItem> {
        val trimmed = query.trim().lowercase()
        val all = getBatteryEmojis()
        if (trimmed.isEmpty()) return all
        return all.filter { item ->
            item.title.lowercase().contains(trimmed) ||
                    item.category.lowercase().contains(trimmed) ||
                    item.tags.any { it.contains(trimmed) }
        }
    }

    fun getStatusBarGradients(): List<StatusBarThemeItem> {
        return listOf(
            StatusBarThemeItem(
                id = "grad_pink",
                title = "Hot Pink",
                isGradient = true,
                drawableRes = R.drawable.bg_theme_grad_pink,
                isSelected = true
            ),
            StatusBarThemeItem(
                id = "grad_lavender",
                title = "Soft Lavender",
                isGradient = true,
                drawableRes = R.drawable.bg_theme_grad_lavender,
                isSelected = false
            ),
            StatusBarThemeItem(
                id = "grad_cyan",
                title = "Ocean Cyan",
                isGradient = true,
                drawableRes = R.drawable.bg_theme_grad_cyan,
                isSelected = false
            ),
            StatusBarThemeItem(
                id = "grad_orange",
                title = "Sunset Orange",
                isGradient = true,
                drawableRes = R.drawable.bg_theme_grad_orange,
                isSelected = false
            ),
            StatusBarThemeItem(
                id = "grad_peach",
                title = "Peach Gold",
                isGradient = true,
                drawableRes = R.drawable.bg_theme_grad_peach,
                isSelected = false
            ),
            StatusBarThemeItem(
                id = "grad_plum",
                title = "Plum Velvet",
                isGradient = true,
                drawableRes = R.drawable.bg_theme_grad_plum,
                isSelected = false
            )
        )
    }

    fun getStatusBarBackgrounds(): List<StatusBarThemeItem> {
        return listOf(
            StatusBarThemeItem(
                id = "bg_pink",
                title = "Cute Pink",
                isGradient = false,
                drawableRes = R.drawable.status_png1,
                isSelected = true
            ),
            StatusBarThemeItem(
                id = "bg_cream",
                title = "Soft Cream",
                isGradient = false,
                drawableRes = R.drawable.status_png2,
                isSelected = false
            ),
            StatusBarThemeItem(
                id = "bg_green",
                title = "Meadow Frog",
                isGradient = false,
                drawableRes = R.drawable.status_png3,
                isSelected = false
            ),
            StatusBarThemeItem(
                id = "bg_checkered",
                title = "Blue Gingham",
                isGradient = false,
                drawableRes = R.drawable.status_png4,
                isSelected = false
            ),
            StatusBarThemeItem(
                id = "bg_yellow",
                title = "Sunny Pastel",
                isGradient = false,
                drawableRes = R.drawable.status_png5,
                isSelected = false
            ),
            StatusBarThemeItem(
                id = "bg_stripes",
                title = "Blue Stripes",
                isGradient = false,
                drawableRes = R.drawable.status_png6,
                isSelected = false
            )
        )
    }

    fun getStatusBarAnimations(): List<BatteryEmojiItem> {
        return listOf(
            BatteryEmojiItem(
                id = "anim_heart",
                title = "Heart",
                category = "animation",
                isLottie = true,
                lottieRawRes = R.raw.red_heart
            ),
            BatteryEmojiItem(
                id = "anim_doraemon",
                title = "Doraemon",
                category = "animation",
                isLottie = true,
                lottieRawRes = R.raw.rabit
            )
        )
    }

    fun getStickers(category: String? = null): List<com.jhw.batteryemoji.domain.model.StickerItem> {
        val allStickers = listOf(
            com.jhw.batteryemoji.domain.model.StickerItem(
                id = "stk_player_blue",
                title = "Blue Striker",
                category = "hot",
                drawableRes = R.drawable.ic_sticker_player_blue
            ),
            com.jhw.batteryemoji.domain.model.StickerItem(
                id = "stk_player_red",
                title = "Red Striker",
                category = "hot",
                drawableRes = R.drawable.ic_sticker_player_red
            ),
            com.jhw.batteryemoji.domain.model.StickerItem(
                id = "stk_player_trophy",
                title = "Champion Gold",
                category = "hot",
                drawableRes = R.drawable.ic_sticker_player_trophy
            ),
            com.jhw.batteryemoji.domain.model.StickerItem(
                id = "stk_player_celebrate",
                title = "Victory Cheers",
                category = "hot",
                drawableRes = R.drawable.ic_sticker_player_celebrate
            ),
            com.jhw.batteryemoji.domain.model.StickerItem(
                id = "stk_puppy",
                title = "Puppy Friend",
                category = "sanrio",
                drawableRes = R.drawable.ic_blueprint_puppy
            ),
        )
        return if (category.isNullOrEmpty() || category.equals("hot", ignoreCase = true)) {
            allStickers
        } else {
            allStickers.filter { it.category.equals(category, ignoreCase = true) }
        }
    }
}
