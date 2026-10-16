package com.asim.jhbatteryapp.data.datasource

import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.model.CategoryItem
import com.asim.jhbatteryapp.domain.model.OnboardingPageItem
import com.asim.jhbatteryapp.domain.model.QuickActionItem
import com.asim.jhbatteryapp.domain.model.StatusBarThemeItem

class BatteryLocalDataSource {

    fun getCategories(): List<CategoryItem> {
        return listOf(
            CategoryItem(id = "hot", title = "🔥 HOT", isSelected = true),
            CategoryItem(id = "pokemon", title = "Pokemon", isSelected = false),
            CategoryItem(id = "sanrio", title = "Sanrio", isSelected = false),
            CategoryItem(id = "anime", title = "Anime", isSelected = false)
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
            BatteryEmojiItem(
                id = "2",
                title = "Pink Blossom",
                category = "hot",
                previewDrawableRes = R.drawable.ic_blueprint_girl_pink,
                backgroundColorRes = R.color.card_bg_pink,
                batteryPercentage = 85,
                batteryColorRes = R.color.battery_pink,
                isHot = true,
                isRecent = true,
                isTopPick = true,
                tags = listOf("anime", "pink", "girl", "cute")
            ),
            BatteryEmojiItem(
                id = "3",
                title = "Scholar Cat",
                category = "hot",
                previewDrawableRes = R.drawable.battery_pupy,
                backgroundColorRes = R.color.card_bg_gray,
                batteryPercentage = 60,
                batteryColorRes = R.color.battery_gray,
                isHot = true,
                isRecent = true,
                isTopPick = true,
                tags = listOf("cat", "gray", "smart", "black")
            ),
            BatteryEmojiItem(
                id = "4",
                title = "Lucky Frog",
                category = "pokemon",
                previewDrawableRes = R.drawable.ic_blueprint_frog_green,
                backgroundColorRes = R.color.card_bg_green,
                batteryPercentage = 95,
                batteryColorRes = R.color.battery_green,
                isHot = true,
                isRecent = true,
                isTopPick = false,
                tags = listOf("frog", "green", "lucky", "nature")
            ),
            BatteryEmojiItem(
                id = "5",
                title = "Starry Buddy",
                category = "anime",
                previewDrawableRes = R.drawable.ic_blueprint_stitch_yellow,
                backgroundColorRes = R.color.card_bg_yellow,
                batteryPercentage = 75,
                batteryColorRes = R.color.battery_yellow,
                isHot = true,
                isRecent = false,
                isTopPick = true,
                tags = listOf("stitch", "yellow", "blue", "alien")
            ),
            BatteryEmojiItem(
                id = "6",
                title = "Lilac Melody",
                category = "sanrio",
                previewDrawableRes = R.drawable.ic_blueprint_melody_purple,
                backgroundColorRes = R.color.card_bg_purple,
                batteryPercentage = 90,
                batteryColorRes = R.color.battery_purple,
                isHot = true,
                isRecent = true,
                isTopPick = true,
                tags = listOf("sanrio", "purple", "melody", "bunny")
            ),
            // Repeat to form a full grid as seen in the screenshot
            BatteryEmojiItem(
                id = "7",
                title = "Cloud Bunny II",
                category = "sanrio",
                previewDrawableRes = R.drawable.ic_blueprint_bunny_blue,
                backgroundColorRes = R.color.card_bg_blue,
                batteryPercentage = 100,
                batteryColorRes = R.color.battery_blue,
                isHot = false,
                isRecent = false,
                isTopPick = false,
                tags = listOf("cute", "bunny", "blue")
            ),
            BatteryEmojiItem(
                id = "8",
                title = "Pink Blossom II",
                category = "anime",
                previewDrawableRes = R.drawable.ic_blueprint_girl_pink,
                backgroundColorRes = R.color.card_bg_pink,
                batteryPercentage = 80,
                batteryColorRes = R.color.battery_pink,
                isHot = false,
                isRecent = false,
                isTopPick = false,
                tags = listOf("anime", "pink")
            ),
            BatteryEmojiItem(
                id = "9",
                title = "Scholar Cat II",
                category = "pokemon",
                previewDrawableRes = R.drawable.ic_blueprint_cat_gray,
                backgroundColorRes = R.color.card_bg_gray,
                batteryPercentage = 50,
                batteryColorRes = R.color.battery_gray,
                isHot = false,
                isRecent = false,
                isTopPick = false,
                tags = listOf("cat", "gray")
            ),
            BatteryEmojiItem(
                id = "7",
                title = "Cloud Bunny II",
                category = "sanrio",
                previewDrawableRes = R.drawable.ic_blueprint_bunny_blue,
                backgroundColorRes = R.color.card_bg_blue,
                batteryPercentage = 100,
                batteryColorRes = R.color.battery_blue,
                isHot = false,
                isRecent = false,
                isTopPick = false,
                tags = listOf("cute", "bunny", "blue")
            ),
            BatteryEmojiItem(
                id = "8",
                title = "Pink Blossom II",
                category = "anime",
                previewDrawableRes = R.drawable.ic_blueprint_girl_pink,
                backgroundColorRes = R.color.card_bg_pink,
                batteryPercentage = 80,
                batteryColorRes = R.color.battery_pink,
                isHot = false,
                isRecent = false,
                isTopPick = false,
                tags = listOf("anime", "pink")
            ),
            BatteryEmojiItem(
                id = "9",
                title = "Scholar Cat II",
                category = "pokemon",
                previewDrawableRes = R.drawable.ic_blueprint_cat_gray,
                backgroundColorRes = R.color.card_bg_gray,
                batteryPercentage = 50,
                batteryColorRes = R.color.battery_gray,
                isHot = false,
                isRecent = false,
                isTopPick = false,
                tags = listOf("cat", "gray")
            ),
            BatteryEmojiItem(
                id = "7",
                title = "Cloud Bunny II",
                category = "sanrio",
                previewDrawableRes = R.drawable.ic_blueprint_bunny_blue,
                backgroundColorRes = R.color.card_bg_blue,
                batteryPercentage = 100,
                batteryColorRes = R.color.battery_blue,
                isHot = false,
                isRecent = false,
                isTopPick = false,
                tags = listOf("cute", "bunny", "blue")
            ),
            BatteryEmojiItem(
                id = "8",
                title = "Pink Blossom II",
                category = "anime",
                previewDrawableRes = R.drawable.ic_blueprint_girl_pink,
                backgroundColorRes = R.color.card_bg_pink,
                batteryPercentage = 80,
                batteryColorRes = R.color.battery_pink,
                isHot = false,
                isRecent = false,
                isTopPick = false,
                tags = listOf("anime", "pink")
            ),
            BatteryEmojiItem(
                id = "9",
                title = "Scholar Cat II",
                category = "pokemon",
                previewDrawableRes = R.drawable.ic_blueprint_cat_gray,
                backgroundColorRes = R.color.card_bg_gray,
                batteryPercentage = 50,
                batteryColorRes = R.color.battery_gray,
                isHot = false,
                isRecent = false,
                isTopPick = false,
                tags = listOf("cat", "gray")
            )
        )

        return if (categoryId.isNullOrEmpty() || categoryId == "hot") {
            allItems
        } else {
            allItems.filter { it.category.equals(categoryId, ignoreCase = true) }
        }
    }

    fun getOnboardingPages(): List<OnboardingPageItem> {
        return listOf(
            OnboardingPageItem(
                id = 1,
                title = "A little cuter.\nA little more you.",
                subtitle = "Give your phone a playful personal touch,\nand take a little joy everywhere.",
                puppyMascotRes = R.drawable.ic_blueprint_puppy,
                previewBearRes = R.drawable.ic_blueprint_bear_sleeping
            ),
            OnboardingPageItem(
                id = 2,
                title = "Live Battery Emojis.\nRight on Your Screen.",
                subtitle = "Watch cute animals cuddle, play, and chill\naccording to your battery status.",
                puppyMascotRes = R.drawable.ic_blueprint_puppy,
                previewBearRes = R.drawable.ic_blueprint_bunny_blue
            ),
            OnboardingPageItem(
                id = 3,
                title = "Custom Animations\n& Endless Themes.",
                subtitle = "Unlock exclusive Sanrio, Pokemon & Anime packs\nwith vibrant interactive styles.",
                puppyMascotRes = R.drawable.ic_blueprint_puppy,
                previewBearRes = R.drawable.ic_blueprint_girl_pink
            )
        )
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
//            BatteryEmojiItem(
//                id = "anim_lottie_battery",
//                title = "Lottie Battery",
//                category = "lottie",
//                previewDrawableRes = R.drawable.ic_status_battery,
//                backgroundColorRes = R.color.card_bg_blue,
//                batteryPercentage = 100,
//                batteryColorRes = R.color.battery_blue,
//                isLottie = true,
//                lottieRes = R.raw.battery
//            ),
        )
    }

    fun getStickers(category: String? = null): List<com.asim.jhbatteryapp.domain.model.StickerItem> {
        val allStickers = listOf(
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_player_blue",
                title = "Blue Striker",
                category = "hot",
                drawableRes = R.drawable.ic_sticker_player_blue
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_player_red",
                title = "Red Striker",
                category = "hot",
                drawableRes = R.drawable.ic_sticker_player_red
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_player_trophy",
                title = "Champion Gold",
                category = "hot",
                drawableRes = R.drawable.ic_sticker_player_trophy
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_player_celebrate",
                title = "Victory Cheers",
                category = "hot",
                drawableRes = R.drawable.ic_sticker_player_celebrate
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_puppy",
                title = "Puppy Friend",
                category = "sanrio",
                drawableRes = R.drawable.ic_blueprint_puppy
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_girl",
                title = "Pink Blossom",
                category = "anime",
                drawableRes = R.drawable.ic_blueprint_girl_pink
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_cat",
                title = "Smart Scholar",
                category = "anime",
                drawableRes = R.drawable.ic_blueprint_cat_gray
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_frog",
                title = "Lucky Frog",
                category = "pokemon",
                drawableRes = R.drawable.ic_blueprint_frog_green
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_stitch",
                title = "Starry Stitch",
                category = "pokemon",
                drawableRes = R.drawable.ic_blueprint_stitch_yellow
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_melody",
                title = "Purple Melody",
                category = "sanrio",
                drawableRes = R.drawable.ic_blueprint_melody_purple
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_poke",
                title = "Electric Mouse",
                category = "pokemon",
                drawableRes = R.drawable.poke
            ),
            com.asim.jhbatteryapp.domain.model.StickerItem(
                id = "stk_bunny",
                title = "Cloud Bunny",
                category = "sanrio",
                drawableRes = R.drawable.ic_blueprint_bunny_blue
            )
        )
        return if (category.isNullOrEmpty() || category.equals("hot", ignoreCase = true)) {
            allStickers
        } else {
            allStickers.filter { it.category.equals(category, ignoreCase = true) }
        }
    }
}
