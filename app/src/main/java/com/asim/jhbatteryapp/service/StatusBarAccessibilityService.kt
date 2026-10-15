package com.asim.jhbatteryapp.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl
import com.asim.jhbatteryapp.databinding.LayoutStatusBarOverlayBinding
import com.asim.jhbatteryapp.domain.repository.PreferenceRepository

/**
 * Accessibility Service that uses TYPE_ACCESSIBILITY_OVERLAY to draw
 * DIRECTLY ON TOP OF the system status bar, completely overlapping and hiding it.
 * Uses StatusBarOverlayRenderer for rendering and SystemHardwareStateObserver for hardware changes.
 */
class StatusBarAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var binding: LayoutStatusBarOverlayBinding? = null
    private var isViewAttached = false

    private val preferenceRepository: PreferenceRepository by lazy {
        PreferenceRepositoryImpl()
    }

    private val hardwareObserver = SystemHardwareStateObserver()
    private val overlayRenderer = StatusBarOverlayRenderer()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true

        initAccessibilityOverlayView()

        hardwareObserver.startObserving(this) { hardwareState ->
            renderCurrentSettings(hardwareState)
        }

        // Apply saved global appearance (custom or theme)
        applyCustomSettings()

        // Stop the fallback overlay service since accessibility overlay is higher layer
        StatusBarOverlayService.stop(this)
    }

    private fun initAccessibilityOverlayView() {
        if (isViewAttached) return

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager == null) return

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

            windowManager?.addView(binding?.root, layoutParams)
            isViewAttached = true

            // Trigger initial hardware state refresh
            hardwareObserver.refreshState(this, null)
            renderCurrentSettings()
        } catch (e: Exception) {
            e.printStackTrace()
            isViewAttached = false
        }
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
        if (!isViewAttached || binding == null) {
            initAccessibilityOverlayView()
            applyCustomSettings()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!isViewAttached || binding == null) {
            initAccessibilityOverlayView()
            applyCustomSettings()
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
            inst.applyCustomSettings()
        }

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (isRunning || instance != null) {
                return true
            }

            try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
                val enabledServices = am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                if (enabledServices != null) {
                    for (service in enabledServices) {
                        if (service.id.contains(context.packageName) &&
                            service.id.contains("StatusBarAccessibilityService")) {
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
                    if (enabledServicesSetting.contains(context.packageName, ignoreCase = true) &&
                        enabledServicesSetting.contains("StatusBarAccessibilityService", ignoreCase = true)) {
                        return true
                    }
                }
            } catch (_: Exception) {}

            return false
        }

        fun openAccessibilitySettings(context: Context) {
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
