package com.asim.jhbatteryapp.service

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.RoundedCorner
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.remote.RemoteImageLoader
import com.asim.jhbatteryapp.databinding.LayoutStatusBarOverlayBinding
import com.asim.jhbatteryapp.domain.model.StatusBarSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated renderer responsible for styling and updating the LayoutStatusBarOverlayBinding
 * based on StatusBarSettings and HardwareState.
 * Completely isolates view rendering from the Service lifecycle.
 */
class StatusBarOverlayRenderer {

    /** Hardware state of the previous render; icons only animate in/out when this changes. */
    private var lastHardwareState: HardwareState? = null

    /**
     * @param suppressAnimation true while the bar is faded out (full screen / landscape): the
     *   Lottie animation is left paused instead of being restarted.
     */
    fun render(
        context: Context,
        binding: LayoutStatusBarOverlayBinding,
        settings: StatusBarSettings,
        hardwareState: HardwareState,
        windowManager: WindowManager?,
        systemStatusBarHeight: Int,
        suppressAnimation: Boolean = false
    ) {
        if (!settings.isEmojiBatteryEnabled) {
            binding.clOverlayRoot.visibility = View.GONE
            lastHardwareState = null
            return
        }
        val wasVisible = binding.clOverlayRoot.visibility == View.VISIBLE
        binding.clOverlayRoot.visibility = View.VISIBLE

        // Icons appearing/disappearing because the phone's state changed (charger plugged in,
        // Wi-Fi lost, airplane mode...) slide and fade instead of popping. Settings changes from
        // the sliders apply instantly so they track the finger.
        val previousHardware = lastHardwareState
        lastHardwareState = hardwareState
        if (wasVisible && previousHardware != null && previousHardware != hardwareState &&
            binding.clOverlayRoot.isLaidOut && binding.clOverlayRoot.alpha > 0f
        ) {
            TransitionManager.beginDelayedTransition(
                binding.clOverlayRoot,
                AutoTransition().apply { duration = ICON_TRANSITION_MS }
            )
        }

        val density = context.resources.displayMetrics.density

        // 1. Layout Height
        val targetHeightPx = (settings.heightDp * density).toInt()
        val effectiveHeight = if (targetHeightPx > systemStatusBarHeight) targetHeightPx else systemStatusBarHeight

        val rootParams = binding.root.layoutParams as? WindowManager.LayoutParams
        if (rootParams != null && rootParams.height != effectiveHeight) {
            rootParams.height = effectiveHeight
            try {
                windowManager?.updateViewLayout(binding.root, rootParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Background and Base Content Color
        val effectiveContentColor: Int
        if (settings.lastAppliedThemeMode == "theme") {
            val rectRes = getOverlayRectDrawable(settings.appliedThemeDrawableRes)
            val drawable = ContextCompat.getDrawable(context, rectRes)?.mutate()
            if (drawable is GradientDrawable) {
                drawable.cornerRadius = 0f
            }
            if (drawable != null) {
                binding.clOverlayRoot.background = drawable
            } else {
                binding.clOverlayRoot.setBackgroundResource(rectRes)
            }
            effectiveContentColor = if (settings.isAppliedThemeGradient) Color.WHITE else settings.iconColor
        } else {
            // "custom" mode
            if (settings.backgroundColor != 0) {
                binding.clOverlayRoot.background = ColorDrawable(settings.backgroundColor)
            } else {
                binding.clOverlayRoot.background = ColorDrawable(Color.parseColor("#F72585"))
            }
            effectiveContentColor = settings.iconColor
        }

        // 3. Margins (plus room for a corner punch-hole camera and rounded screen corners)
        val safe = safeSideInsets(binding.clOverlayRoot)
        val lmPx = (settings.leftMarginDp * density).toInt() + safe.first
        val rmPx = (settings.rightMarginDp * density).toInt() + safe.second
        binding.clOverlayRoot.setPadding(lmPx, 0, rmPx, 0)

        // 4. Base Typography & Percentage
        binding.tcOverlayTime.setTextColor(effectiveContentColor)
        binding.tvOverlayBattery.setTextColor(effectiveContentColor)
        binding.tvOverlayBattery.textSize = settings.batteryTextSizeSp.toFloat()
        binding.tvOverlayBattery.text = "${hardwareState.batteryLevel}%"

        // 5. Multi-Select Custom Icons (Only visible if corresponding system function is active)
        val enabledIcons = settings.enabledCustomIcons

        // Left: Emoji Mascot (beside time on left side)
        val mascotToDisplay = when {
            settings.leftStatusEmojiRes != 0 -> settings.leftStatusEmojiRes
            settings.lastAppliedThemeMode == "custom" -> settings.leftStatusEmojiRes
            settings.appliedMascotDrawableRes != 0 && !settings.isAppliedAnimationLottie -> settings.appliedMascotDrawableRes
            else -> 0
        }
        val remoteLeftEmojiPath = settings.remoteLeftEmojiPath
        if (remoteLeftEmojiPath != null || mascotToDisplay != 0) {
            binding.ivOverlayLeftEmoji.visibility = View.VISIBLE
            binding.ivOverlayLeftEmoji.imageTintList = null
            val emojiSize = (16 * density).toInt().coerceIn((12 * density).toInt(), (26 * density).toInt())
            val params = binding.ivOverlayLeftEmoji.layoutParams
            params.width = (emojiSize * 1.35f).toInt()
            params.height = emojiSize
            binding.ivOverlayLeftEmoji.layoutParams = params
            if (remoteLeftEmojiPath != null) {
                binding.ivOverlayLeftEmoji.setImageBitmap(
                    RemoteImageLoader.decodeFileForDisplay(remoteLeftEmojiPath, params.width, params.height)
                )
            } else {
                binding.ivOverlayLeftEmoji.setImageResource(mascotToDisplay)
            }
        } else {
            binding.ivOverlayLeftEmoji.visibility = View.GONE
        }

        // Left: Date
        if (enabledIcons.contains("calendar")) {
            binding.tvOverlayDate.visibility = View.VISIBLE
            try {
                val sdf = SimpleDateFormat(settings.dateFormat, Locale.getDefault())
                binding.tvOverlayDate.text = sdf.format(Date())
            } catch (_: Exception) {
                binding.tvOverlayDate.text = "Wed, Oct 07"
            }
            binding.tvOverlayDate.setTextColor(settings.getEffectiveIconColor("calendar"))
            val dtSize = settings.getEffectiveIconSize("calendar")
            binding.tvOverlayDate.textSize = (dtSize * 0.55f).coerceIn(8f, 16f)
        } else {
            binding.tvOverlayDate.visibility = View.GONE
        }

        // Left: Airplane (Shows ONLY if Airplane mode is active)
        if (enabledIcons.contains("airplane") && hardwareState.isAirplaneActive) {
            binding.ivOverlayAirplane.visibility = View.VISIBLE
            binding.ivOverlayAirplane.imageTintList = ColorStateList.valueOf(settings.getEffectiveIconColor("airplane"))
            val s = (settings.getEffectiveIconSize("airplane") * density).toInt()
            binding.ivOverlayAirplane.layoutParams.width = s
            binding.ivOverlayAirplane.layoutParams.height = s
        } else {
            binding.ivOverlayAirplane.visibility = View.GONE
        }

        // Left: Ringer (Shows ONLY if Silent or Vibrate is active)
        if (enabledIcons.contains("ringer") && hardwareState.isRingerSilentOrVibrate) {
            binding.ivOverlayRinger.visibility = View.VISIBLE
            binding.ivOverlayRinger.imageTintList = ColorStateList.valueOf(settings.getEffectiveIconColor("ringer"))
            val s = (settings.getEffectiveIconSize("ringer") * density).toInt()
            binding.ivOverlayRinger.layoutParams.width = s
            binding.ivOverlayRinger.layoutParams.height = s
        } else {
            binding.ivOverlayRinger.visibility = View.GONE
        }

        // Left: Hotspot (Shows ONLY if Hotspot is active)
        if (enabledIcons.contains("hotspot") && hardwareState.isHotspotActive) {
            binding.ivOverlayHotspot.visibility = View.VISIBLE
            binding.ivOverlayHotspot.imageTintList = ColorStateList.valueOf(settings.getEffectiveIconColor("hotspot"))
            val s = (settings.getEffectiveIconSize("hotspot") * density).toInt()
            binding.ivOverlayHotspot.layoutParams.width = s
            binding.ivOverlayHotspot.layoutParams.height = s
        } else {
            binding.ivOverlayHotspot.visibility = View.GONE
        }

        // Left: Emotion (Decorative)
        if (enabledIcons.contains("emotion")) {
            binding.ivOverlayEmotion.visibility = View.VISIBLE
            val emotionRes = when (settings.emotionStyle) {
                "sad" -> R.drawable.ic_reaction_sad
                "happy" -> R.drawable.ic_reaction_happy
                else -> R.drawable.ic_reaction_love
            }
            val s = (settings.getEffectiveIconSize("emotion") * density).toInt()
            val remoteEmotionPath = settings.remoteEmotionPath
            if (remoteEmotionPath != null) {
                binding.ivOverlayEmotion.setImageBitmap(RemoteImageLoader.decodeFileForDisplay(remoteEmotionPath, s, s))
            } else {
                binding.ivOverlayEmotion.setImageResource(emotionRes)
            }
            binding.ivOverlayEmotion.layoutParams.width = s
            binding.ivOverlayEmotion.layoutParams.height = s
        } else {
            binding.ivOverlayEmotion.visibility = View.GONE
        }

        // Left: Lottie Animation (Sub se last on left side, towards center notch)
        val isAnimEnabled = settings.isEmojiBatteryEnabled &&
                (!settings.iconSizes.containsKey("animation") || settings.enabledCustomIcons.contains("animation"))
        val animMascotSizeDp = settings.iconSizes["animation"] ?: settings.mascotSizeDp
        val mascotSize = (animMascotSizeDp * density).toInt().coerceIn((12 * density).toInt(), (60 * density).toInt())

        val remoteAnimPath = settings.remoteAnimationPath
        if (isAnimEnabled && remoteAnimPath != null) {
            // Downloaded animation from the remote manifest
            binding.lottieOverlayLeftAnimation.visibility = View.VISIBLE
            val params = binding.lottieOverlayLeftAnimation.layoutParams
            params.width = (mascotSize * 1.35f).toInt()
            params.height = mascotSize
            binding.lottieOverlayLeftAnimation.layoutParams = params
            RemoteImageLoader.bindAnimationFile(
                binding.lottieOverlayLeftAnimation, remoteAnimPath, params.width, params.height
            )
        } else if (isAnimEnabled && settings.isAppliedAnimationLottie && settings.appliedAnimationRawRes != 0) {
            binding.lottieOverlayLeftAnimation.visibility = View.VISIBLE
            val params = binding.lottieOverlayLeftAnimation.layoutParams
            params.width = (mascotSize * 1.35f).toInt()
            params.height = mascotSize
            binding.lottieOverlayLeftAnimation.layoutParams = params

            val currentRes = binding.lottieOverlayLeftAnimation.tag as? Int
            if (currentRes != settings.appliedAnimationRawRes) {
                binding.lottieOverlayLeftAnimation.tag = settings.appliedAnimationRawRes
                binding.lottieOverlayLeftAnimation.setAnimation(settings.appliedAnimationRawRes)
                binding.lottieOverlayLeftAnimation.playAnimation()
            } else if (!binding.lottieOverlayLeftAnimation.isAnimating && !suppressAnimation) {
                binding.lottieOverlayLeftAnimation.playAnimation()
            }
        } else {
            binding.lottieOverlayLeftAnimation.visibility = View.GONE
            binding.lottieOverlayLeftAnimation.cancelAnimation()
            binding.lottieOverlayLeftAnimation.tag = null
        }

        if (suppressAnimation) binding.lottieOverlayLeftAnimation.pauseAnimation()

        // Left: Animation Sparkle (Deprecated redundant widget; replaced by left status bar animation)
        binding.ivOverlayAnimation.visibility = View.GONE

        // Right: Data (Shows if enabled in customization OR if mobile data is active)
        val isDataEnabled = enabledIcons.contains("data")
        val showData = hardwareState.isMobileDataConnected

        if (showData) {
            binding.ivOverlayData.visibility = View.VISIBLE

            val dataColor = if (isDataEnabled) {
                settings.getEffectiveIconColor("data")
            } else {
                effectiveContentColor
            }

            binding.ivOverlayData.imageTintList =
                ColorStateList.valueOf(dataColor)

            val s = if (isDataEnabled) {
                (settings.getEffectiveIconSize("data") * density).toInt()
            } else {
                (14 * density).toInt()
            }

            binding.ivOverlayData.layoutParams =
                binding.ivOverlayData.layoutParams.apply {
                    width = s
                    height = s
                }
        } else {
            binding.ivOverlayData.visibility = View.GONE
        }

        // Right: Signal / SIM (Shows if enabled in customization OR if Airplane mode is inactive)
        val isSimEnabled = enabledIcons.contains("signal") || enabledIcons.contains("sim")
        val showSignal = isSimEnabled || !hardwareState.isAirplaneActive
        if (showSignal) {
            binding.ivOverlaySignal.visibility = View.VISIBLE
            // Live bars from the system; full bars until the first reading arrives
            binding.ivOverlaySignal.setImageLevel(
                if (hardwareState.signalLevel in 0..4) hardwareState.signalLevel else 4
            )
            val signalColor = if (isSimEnabled) settings.getEffectiveIconColor("signal") else effectiveContentColor
            binding.ivOverlaySignal.imageTintList = ColorStateList.valueOf(signalColor)
            val s = if (isSimEnabled) {
                (settings.getEffectiveIconSize("signal") * density).toInt()
            } else {
                (14 * density).toInt()
            }
            binding.ivOverlaySignal.layoutParams.width = s
            binding.ivOverlaySignal.layoutParams.height = s
        } else {
            binding.ivOverlaySignal.visibility = View.GONE
        }

        // Right: Wifi (Shows if Wi-Fi is connected)
        if (hardwareState.isWifiConnected) {
            binding.ivOverlayWifi.visibility = View.VISIBLE
            val wifiColor = if (enabledIcons.contains("wifi")) settings.getEffectiveIconColor("wifi") else effectiveContentColor
            binding.ivOverlayWifi.imageTintList = ColorStateList.valueOf(wifiColor)
            if (enabledIcons.contains("wifi")) {
                val s = (settings.getEffectiveIconSize("wifi") * density).toInt()
                binding.ivOverlayWifi.layoutParams.width = s
                binding.ivOverlayWifi.layoutParams.height = s
            }
        } else {
            binding.ivOverlayWifi.visibility = View.GONE
        }

        // Right: Charge Bolt (Shows ONLY when device is actively charging)
        if (hardwareState.isCharging) {
            binding.ivOverlayBolt.visibility = View.VISIBLE
            val chargeColor = if (enabledIcons.contains("charge")) settings.getEffectiveIconColor("charge") else effectiveContentColor
            binding.ivOverlayBolt.imageTintList = ColorStateList.valueOf(chargeColor)
            val s = (settings.getEffectiveIconSize("charge") * density).toInt()
            binding.ivOverlayBolt.layoutParams.width = s
            binding.ivOverlayBolt.layoutParams.height = s
        } else {
            binding.ivOverlayBolt.visibility = View.GONE
        }

        // 6. Battery Icon (Style or Emoji)
        val iconSizeDp = settings.batteryIconSizeDp
        val iconHeightPx = (iconSizeDp * density).toInt()
        val iconWidthPx = (iconSizeDp * 1.5f * density).toInt()
        val iconParams = binding.ivOverlayBatteryIcon.layoutParams
        iconParams.width = iconWidthPx
        iconParams.height = iconHeightPx
        binding.ivOverlayBatteryIcon.layoutParams = iconParams

        val remoteIconPath = settings.remoteBatteryIconPath
        if (remoteIconPath != null) {
            // Downloaded emoji from the remote manifest
            binding.ivOverlayBatteryIcon.setImageBitmap(
                RemoteImageLoader.decodeFileForDisplay(remoteIconPath, iconWidthPx, iconHeightPx)
            )
        } else {
            binding.ivOverlayBatteryIcon.setImageResource(settings.appliedBatteryIconRes)
        }
        if (settings.isBatteryIconTintable && remoteIconPath == null) {
            binding.ivOverlayBatteryIcon.imageTintList = ColorStateList.valueOf(settings.iconColor)
        } else {
            binding.ivOverlayBatteryIcon.imageTintList = null
        }
    }

    /**
     * Extra left/right padding so the time and battery don't sit under a corner punch-hole camera
     * or get clipped by rounded screen corners, like the system status bar. Zero until the window
     * has insets (the services re-render once they arrive).
     */
    private fun safeSideInsets(root: View): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return 0 to 0
        val insets = root.rootWindowInsets ?: return 0 to 0
        val width = root.resources.displayMetrics.widthPixels
        val gap = (4 * root.resources.displayMetrics.density).toInt()
        var left = 0
        var right = 0

        insets.displayCutout?.boundingRects?.forEach { rect ->
            if (rect.top > 0 || rect.isEmpty) return@forEach
            when {
                rect.centerX() < width / 3 -> left = maxOf(left, rect.right + gap)
                rect.centerX() > width * 2 / 3 -> right = maxOf(right, width - rect.left + gap)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // The corner curve only bites into the first part of its radius at status bar height
            insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.let {
                left = maxOf(left, (it.radius * CORNER_INSET_FRACTION).toInt())
            }
            insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.let {
                right = maxOf(right, (it.radius * CORNER_INSET_FRACTION).toInt())
            }
        }
        return left to right
    }

    companion object {
        private const val ICON_TRANSITION_MS = 180L
        private const val CORNER_INSET_FRACTION = 0.35f

        fun getOverlayRectDrawable(themeRes: Int): Int {
            return when (themeRes) {
                R.drawable.bg_theme_grad_pink -> R.drawable.bg_theme_grad_pink_rect
                R.drawable.bg_theme_grad_lavender -> R.drawable.bg_theme_grad_lavender_rect
                R.drawable.bg_theme_grad_cyan -> R.drawable.bg_theme_grad_cyan_rect
                R.drawable.bg_theme_grad_orange -> R.drawable.bg_theme_grad_orange_rect
                R.drawable.bg_theme_grad_peach -> R.drawable.bg_theme_grad_peach_rect
                R.drawable.bg_theme_grad_plum -> R.drawable.bg_theme_grad_plum_rect
                else -> themeRes
            }
        }
    }
}
