package com.asim.jhbatteryapp.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.remote.RemoteImageLoader
import com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl
import com.asim.jhbatteryapp.databinding.LayoutFloatingStickerBinding
import com.asim.jhbatteryapp.databinding.LayoutStatusBarOverlayBinding
import com.asim.jhbatteryapp.domain.repository.PreferenceRepository

/**
 * Accessibility Service that uses TYPE_ACCESSIBILITY_OVERLAY to draw:
 * 1. Custom status bar themes, animations and emoji battery directly on top of the status bar.
 * 2. Floating screen sticker display overlay directly over apps and system screens.
 */
class StatusBarAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var binding: LayoutStatusBarOverlayBinding? = null
    private var isViewAttached = false

    // Floating Sticker Views & Parameters (Driven directly by Accessibility Service)
    private var stickerBinding: LayoutFloatingStickerBinding? = null
    var isStickerAttached = false
        private set
    var stickerLayoutParams: WindowManager.LayoutParams? = null
        private set

    var stickerCurrentX: Int = -1
        private set
    var stickerCurrentY: Int = -1
        private set
    var stickerCurrentSizeDp: Int = 60
        private set
    var stickerCurrentRotationDeg: Float = 0f
        private set
    var stickerCurrentDrawableRes: Int = R.drawable.ic_sticker_player_blue
        private set
    var stickerCurrentImagePath: String = ""
        private set

    val preferenceRepository: PreferenceRepository by lazy {
        PreferenceRepositoryImpl()
    }

    private val hardwareObserver = SystemHardwareStateObserver()
    private val overlayRenderer = StatusBarOverlayRenderer()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true

        try {
            initAccessibilityOverlayView()

            hardwareObserver.startObserving(this) { hardwareState ->
                renderCurrentSettings(hardwareState)
            }

            // Apply saved global appearance (custom or theme)
            applyCustomSettings()

            // Restore floating sticker if enabled
            if (preferenceRepository.isFloatingStickerEnabled()) {
                initFloatingStickerView()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initAccessibilityOverlayView() {
        if (isViewAttached) return

        try {
            if (windowManager == null) {
                windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            }
            val wm = windowManager ?: return

            val inflater = LayoutInflater.from(this)
            binding = LayoutStatusBarOverlayBinding.inflate(inflater)

            val statusBarHeight = getStatusBarHeight()

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                statusBarHeight,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            wm.addView(binding?.root, layoutParams)
            isViewAttached = true

            // Trigger initial hardware state refresh
            hardwareObserver.refreshState(this, null)
            renderCurrentSettings()
        } catch (e: Exception) {
            e.printStackTrace()
            isViewAttached = false
        }
    }

    fun initFloatingStickerView() {
        if (isStickerAttached) {
            updateStickerVisuals()
            return
        }

        try {
            if (windowManager == null) {
                windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            }
            val wm = windowManager ?: return

            val inflater = LayoutInflater.from(this)
            stickerBinding = LayoutFloatingStickerBinding.inflate(inflater)

            val dm = resources.displayMetrics
            stickerCurrentSizeDp = preferenceRepository.getFloatingStickerSize().coerceIn(30, 150)
            stickerCurrentRotationDeg = preferenceRepository.getFloatingStickerRotation()
            stickerCurrentDrawableRes = preferenceRepository.getFloatingStickerDrawableRes()
            stickerCurrentImagePath = preferenceRepository.getFloatingStickerImagePath()

            val savedX = preferenceRepository.getFloatingStickerX()
            val savedY = preferenceRepository.getFloatingStickerY()
            val sizePx = (stickerCurrentSizeDp * dm.density).toInt()
            val defaultX = (dm.widthPixels / 2) - (sizePx / 2)
            val defaultY = (dm.heightPixels / 3)

            stickerCurrentX = if (savedX <= 0 || savedX >= dm.widthPixels) defaultX else savedX
            stickerCurrentY = if (savedY <= 0 || savedY >= dm.heightPixels) defaultY else savedY

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = stickerCurrentX
                this.y = stickerCurrentY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            stickerLayoutParams = lp

            val b = stickerBinding ?: return
            RemoteImageLoader.bindSticker(b.ivFloatingSticker, stickerCurrentDrawableRes, stickerCurrentImagePath)
            val ivParams = b.ivFloatingSticker.layoutParams
            if (ivParams != null) {
                ivParams.width = sizePx
                ivParams.height = sizePx
                b.ivFloatingSticker.layoutParams = ivParams
            }
            b.ivFloatingSticker.rotation = stickerCurrentRotationDeg

            setupStickerTouchDragListener()

            wm.addView(b.root, lp)
            isStickerAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
            isStickerAttached = false
        }
    }

    private fun setupStickerTouchDragListener() {
        val root = stickerBinding?.root ?: return

        root.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val params = stickerLayoutParams ?: return false
                val wm = windowManager ?: return false

                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()

                        if (Math.abs(deltaX) > 4 || Math.abs(deltaY) > 4) {
                            isDragging = true
                        }

                        params.x = initialX + deltaX
                        params.y = initialY + deltaY
                        stickerCurrentX = params.x
                        stickerCurrentY = params.y

                        try {
                            wm.updateViewLayout(stickerBinding?.root, params)
                        } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            preferenceRepository.setFloatingStickerX(params.x)
                            preferenceRepository.setFloatingStickerY(params.y)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    fun nudgeSticker(deltaX: Int, deltaY: Int) {
        val params = stickerLayoutParams ?: return
        val wm = windowManager ?: return

        params.x += deltaX
        params.y += deltaY
        stickerCurrentX = params.x
        stickerCurrentY = params.y

        preferenceRepository.setFloatingStickerX(stickerCurrentX)
        preferenceRepository.setFloatingStickerY(stickerCurrentY)

        try {
            wm.updateViewLayout(stickerBinding?.root, params)
        } catch (_: Exception) {}
    }

    fun updateStickerSize(sizeDp: Int) {
        stickerCurrentSizeDp = sizeDp
        preferenceRepository.setFloatingStickerSize(sizeDp)
        updateStickerVisuals()
    }

    fun updateStickerRotation(rotDeg: Float) {
        stickerCurrentRotationDeg = rotDeg
        preferenceRepository.setFloatingStickerRotation(rotDeg)
        updateStickerVisuals()
    }

    fun updateStickerDrawable(drawableRes: Int, imagePath: String? = null) {
        stickerCurrentDrawableRes = drawableRes
        stickerCurrentImagePath = imagePath.orEmpty()
        preferenceRepository.setFloatingStickerDrawableRes(drawableRes)
        preferenceRepository.setFloatingStickerImagePath(stickerCurrentImagePath)
        updateStickerVisuals()
    }

    fun updateStickerPosition(x: Int, y: Int) {
        stickerCurrentX = x
        stickerCurrentY = y
        preferenceRepository.setFloatingStickerX(x)
        preferenceRepository.setFloatingStickerY(y)
        stickerLayoutParams?.let { lp ->
            lp.x = x
            lp.y = y
            if (isStickerAttached) {
                try {
                    windowManager?.updateViewLayout(stickerBinding?.root, lp)
                } catch (_: Exception) {}
            }
        }
    }

    private fun updateStickerVisuals() {
        val b = stickerBinding ?: return
        val density = resources.displayMetrics.density

        RemoteImageLoader.bindSticker(b.ivFloatingSticker, stickerCurrentDrawableRes, stickerCurrentImagePath)

        val sizePx = (stickerCurrentSizeDp * density).toInt()
        val ivParams = b.ivFloatingSticker.layoutParams
        if (ivParams != null) {
            ivParams.width = sizePx
            ivParams.height = sizePx
            b.ivFloatingSticker.layoutParams = ivParams
        }
        b.root.requestLayout()

        b.ivFloatingSticker.rotation = stickerCurrentRotationDeg

        if (isStickerAttached) {
            stickerLayoutParams?.let { lp ->
                lp.x = stickerCurrentX
                lp.y = stickerCurrentY
                try {
                    windowManager?.updateViewLayout(b.root, lp)
                } catch (_: Exception) {}
            }
        }
    }

    fun removeFloatingSticker() {
        if (isStickerAttached && stickerBinding != null) {
            try {
                windowManager?.removeView(stickerBinding?.root)
            } catch (_: Exception) {}
        }
        stickerBinding = null
        isStickerAttached = false
    }

    fun applyCustomSettings() {
        renderCurrentSettings(hardwareObserver.currentState)
    }

    private fun renderCurrentSettings(hardwareState: HardwareState = hardwareObserver.currentState) {
        val b = binding ?: return
        val settings = preferenceRepository.getStatusBarSettings()
        overlayRenderer.render(
            context = this,
            binding = b,
            settings = settings,
            hardwareState = hardwareState,
            windowManager = windowManager,
            systemStatusBarHeight = getStatusBarHeight()
        )
    }

    fun applyTheme(
        themeDrawableRes: Int,
        isGradient: Boolean,
        mascotRes: Int,
        isEmojiBatteryActive: Boolean
    ) {
        preferenceRepository.setAppliedThemeDrawableRes(themeDrawableRes)
        preferenceRepository.setAppliedThemeGradient(isGradient)
        preferenceRepository.setAppliedMascotDrawableRes(mascotRes)
        preferenceRepository.setEmojiBatteryEnabled(isEmojiBatteryActive)
        preferenceRepository.setLastAppliedThemeMode("theme")
        renderCurrentSettings()
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val height = if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
        val minHeight = (30 * resources.displayMetrics.density).toInt()
        return if (height > minHeight) height else minHeight
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (!isViewAttached || binding == null) {
                initAccessibilityOverlayView()
                applyCustomSettings()
            }
            if (preferenceRepository.isFloatingStickerEnabled() && (!isStickerAttached || stickerBinding == null)) {
                initFloatingStickerView()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            if (!isViewAttached || binding == null) {
                initAccessibilityOverlayView()
                applyCustomSettings()
            }
            if (preferenceRepository.isFloatingStickerEnabled() && (!isStickerAttached || stickerBinding == null)) {
                initFloatingStickerView()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false

        hardwareObserver.stopObserving(this)

        if (isViewAttached && binding != null) {
            try {
                windowManager?.removeView(binding?.root)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding = null
        isViewAttached = false

        removeFloatingSticker()
    }

    companion object {
        var isRunning: Boolean = false
            private set

        var instance: StatusBarAccessibilityService? = null
            private set

        fun getOverlayRectDrawable(themeRes: Int): Int {
            return StatusBarOverlayRenderer.getOverlayRectDrawable(themeRes)
        }

        fun updateTheme(themeRes: Int, isGradient: Boolean) {
            val inst = instance ?: return
            inst.preferenceRepository.setAppliedThemeDrawableRes(themeRes)
            inst.preferenceRepository.setAppliedThemeGradient(isGradient)
            inst.preferenceRepository.setLastAppliedThemeMode("theme")
            inst.applyCustomSettings()
        }

        fun updateCustomSettings() {
            instance?.applyCustomSettings()
        }

        fun updateBatteryIcon(iconRes: Int, isTintable: Boolean) {
            val inst = instance ?: return
            inst.preferenceRepository.setAppliedBatteryIconRes(iconRes)
            inst.preferenceRepository.setBatteryIconTintable(isTintable)
            inst.applyCustomSettings()
        }

        fun updateMascot(mascotRes: Int) {
            val inst = instance ?: return
            inst.preferenceRepository.setAppliedMascotDrawableRes(mascotRes)
            if (mascotRes != 0) {
                inst.preferenceRepository.setAppliedAnimationRawRes(0)
                inst.preferenceRepository.setAppliedAnimationLottie(false)
            }
            inst.applyCustomSettings()
        }

        fun updateAnimation(
            animationRawRes: Int,
            isLottie: Boolean,
            fallbackDrawableRes: Int,
            sizeDp: Int = 50
        ) {
            val inst = instance ?: return
            inst.preferenceRepository.setAppliedAnimationRawRes(animationRawRes)
            inst.preferenceRepository.setAppliedAnimationLottie(isLottie)
            inst.preferenceRepository.setAppliedMascotDrawableRes(fallbackDrawableRes)
            inst.preferenceRepository.setLeftStatusEmojiRes(0)
            inst.preferenceRepository.setLeftStatusEmojiPath("")
            inst.preferenceRepository.setMascotSize(sizeDp)
            inst.preferenceRepository.setCustomIconSize("animation", sizeDp)
            inst.preferenceRepository.setCustomIconEnabled("animation", true)
            inst.preferenceRepository.setEmojiBatteryEnabled(true)
            inst.applyCustomSettings()
        }

        fun updateLeftEmoji(emojiRes: Int) {
            val inst = instance ?: return
            // inst.preferenceRepository.setLeftStatusEmojiRes(emojiRes)
            // inst.preferenceRepository.setAppliedMascotDrawableRes(emojiRes)
            // inst.preferenceRepository.setLastAppliedThemeMode("custom")
            // inst.applyCustomSettings()
            // Logic moved to ViewModel and Fragment "Apply" button to prevent immediate mode flips
        }

        // Floating Sticker Controls (Accessibility Overlay Layer)
        fun showFloatingSticker(
            drawableRes: Int,
            imagePath: String? = null,
            sizeDp: Int = 60,
            rotationDeg: Float = 0f,
            x: Int? = null,
            y: Int? = null
        ) {
            val inst = instance ?: return
            inst.preferenceRepository.setFloatingStickerEnabled(true)
            inst.preferenceRepository.setFloatingStickerDrawableRes(drawableRes)
            inst.preferenceRepository.setFloatingStickerImagePath(imagePath.orEmpty())
            inst.preferenceRepository.setFloatingStickerSize(sizeDp)
            inst.preferenceRepository.setFloatingStickerRotation(rotationDeg)
            if (x != null && y != null) {
                inst.preferenceRepository.setFloatingStickerX(x)
                inst.preferenceRepository.setFloatingStickerY(y)
            }

            if (!inst.isStickerAttached) {
                inst.initFloatingStickerView()
            } else {
                inst.updateStickerDrawable(drawableRes, imagePath)
                inst.updateStickerSize(sizeDp)
                inst.updateStickerRotation(rotationDeg)
                if (x != null && y != null) {
                    inst.updateStickerPosition(x, y)
                }
            }
        }

        fun nudgeFloatingSticker(deltaX: Int, deltaY: Int) {
            instance?.nudgeSticker(deltaX, deltaY)
        }

        fun updateFloatingStickerSize(sizeDp: Int) {
            instance?.updateStickerSize(sizeDp)
        }

        fun updateFloatingStickerRotation(rotDeg: Float) {
            instance?.updateStickerRotation(rotDeg)
        }

        fun updateFloatingStickerDrawable(drawableRes: Int, imagePath: String? = null) {
            instance?.updateStickerDrawable(drawableRes, imagePath)
        }

        fun stopFloatingSticker() {
            instance?.removeFloatingSticker()
        }

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (isRunning || instance != null) {
                return true
            }

            val targetPackage = context.packageName
            val targetClassName = StatusBarAccessibilityService::class.java.name

            try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
                val enabledServices = am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                if (enabledServices != null) {
                    for (service in enabledServices) {
                        val serviceInfo = service.resolveInfo?.serviceInfo
                        if (serviceInfo != null) {
                            if (serviceInfo.packageName == targetPackage &&
                                (serviceInfo.name == targetClassName || serviceInfo.name.endsWith("StatusBarAccessibilityService"))) {
                                return true
                            }
                        }
                        if (service.id.contains(targetPackage, ignoreCase = true) &&
                            service.id.contains("StatusBarAccessibilityService", ignoreCase = true)) {
                            return true
                        }
                    }
                }
            } catch (_: Exception) {}

            try {
                val enabledServicesSetting = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                if (!enabledServicesSetting.isNullOrEmpty()) {
                    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
                    colonSplitter.setString(enabledServicesSetting)
                    val expectedComponent = android.content.ComponentName(context, StatusBarAccessibilityService::class.java)
                    while (colonSplitter.hasNext()) {
                        val componentNameString = colonSplitter.next()
                        val component = android.content.ComponentName.unflattenFromString(componentNameString)
                        if (component != null && component == expectedComponent) {
                            return true
                        }
                        if (componentNameString.contains(targetPackage, ignoreCase = true) &&
                            componentNameString.contains("StatusBarAccessibilityService", ignoreCase = true)) {
                            return true
                        }
                    }
                }
            } catch (_: Exception) {}

            return false
        }

        fun openAccessibilitySettings(context: Context) {
            com.asim.jhbatteryapp.JHBatteryApplication.skipNextWelcome()
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    if (context !is android.app.Activity) {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
