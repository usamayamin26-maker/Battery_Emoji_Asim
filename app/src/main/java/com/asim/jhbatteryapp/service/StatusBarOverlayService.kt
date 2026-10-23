package com.asim.jhbatteryapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl
import com.asim.jhbatteryapp.databinding.LayoutStatusBarOverlayBinding
import com.asim.jhbatteryapp.domain.repository.PreferenceRepository
import com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper
import com.asim.jhbatteryapp.presentation.home.MainActivity

/**
 * Fallback Foreground Service that draws over the status bar using TYPE_APPLICATION_OVERLAY
 * when Accessibility Service is not enabled.
 * Uses StatusBarOverlayRenderer and SystemHardwareStateObserver.
 */
class StatusBarOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var binding: LayoutStatusBarOverlayBinding? = null
    private var isViewAttached = false

    private val preferenceRepository: PreferenceRepository by lazy {
        PreferenceRepositoryImpl()
    }

    private val overlayRenderer = StatusBarOverlayRenderer()
    private val hardwareObserver = SystemHardwareStateObserver()
    private var autoHider: OverlayAutoHider? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        startForegroundServiceWithNotification()

        hardwareObserver.startObserving(this) { hardwareState ->
            if (!StatusBarAccessibilityService.isRunning) {
                renderOverlay(hardwareState)
            }
        }

        if (!StatusBarAccessibilityService.isRunning && StatusBarPermissionHelper.hasOverlayPermission(this)) {
            initOverlayView()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The accessibility service draws the bar: drop this service's copy so it doesn't show
        // twice, but KEEP running. Many phones (Xiaomi, Oppo, Vivo...) kill the whole process when
        // the app is swiped from recents unless a foreground service is running, and that takes
        // the accessibility overlay down with it.
        if (StatusBarAccessibilityService.isRunning) {
            removeOverlayView()
            return START_STICKY
        }

        if (!isViewAttached && StatusBarPermissionHelper.hasOverlayPermission(this)) {
            initOverlayView()
        }

        val themeRes = intent?.getIntExtra(EXTRA_THEME_RES, -1)?.takeIf { it != -1 }
            ?: preferenceRepository.getAppliedThemeDrawableRes()

        val isGradient = intent?.getBooleanExtra(EXTRA_IS_GRADIENT, true)
            ?: preferenceRepository.isAppliedThemeGradient()

        val mascotRes = intent?.getIntExtra(EXTRA_MASCOT_RES, -1)?.takeIf { it != -1 }
            ?: preferenceRepository.getAppliedMascotDrawableRes()

        val isEmojiBatteryActive = preferenceRepository.isEmojiBatteryEnabled()

        if (isViewAttached) {
            applyOverlayTheme(themeRes, isGradient, mascotRes, isEmojiBatteryActive)
        }

        return START_STICKY
    }

    private fun initOverlayView() {
        if (isViewAttached || StatusBarAccessibilityService.isRunning) return

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager == null) return

            val inflater = LayoutInflater.from(this)
            binding = LayoutStatusBarOverlayBinding.inflate(inflater)

            val statusBarHeight = getStatusBarHeight()

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                statusBarHeight,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
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

            binding?.let { b ->
                autoHider = OverlayAutoHider(b.root, b.lottieOverlayLeftAnimation) {
                    renderOverlay()
                }.also { it.attach() }
            }

            hardwareObserver.refreshState(this, null)
            renderOverlay(hardwareObserver.currentState)
        } catch (e: SecurityException) {
            e.printStackTrace()
            isViewAttached = false
            // Keep Foreground Service running to prevent process kill
        } catch (e: Exception) {
            e.printStackTrace()
            isViewAttached = false
        }
    }

    private fun applyOverlayTheme(
        themeDrawableRes: Int,
        isGradient: Boolean,
        mascotRes: Int,
        isEmojiBatteryActive: Boolean
    ) {
        val b = binding ?: return
        val currentSettings = preferenceRepository.getStatusBarSettings().copy(
            appliedThemeDrawableRes = themeDrawableRes,
            isAppliedThemeGradient = isGradient,
            appliedMascotDrawableRes = mascotRes,
            isEmojiBatteryEnabled = isEmojiBatteryActive
        )
        overlayRenderer.render(
            context = this,
            binding = b,
            settings = currentSettings,
            hardwareState = hardwareObserver.currentState,
            windowManager = windowManager,
            systemStatusBarHeight = getStatusBarHeight(),
            suppressAnimation = autoHider?.hidden == true
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        autoHider?.onConfigurationChanged(newConfig)
        renderOverlay()
    }

    private fun renderOverlay(hardwareState: HardwareState = hardwareObserver.currentState) {
        val b = binding ?: return
        val settings = preferenceRepository.getStatusBarSettings()
        overlayRenderer.render(
            context = this,
            binding = b,
            settings = settings,
            hardwareState = hardwareState,
            windowManager = windowManager,
            systemStatusBarHeight = getStatusBarHeight(),
            suppressAnimation = autoHider?.hidden == true
        )
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

    private fun startForegroundServiceWithNotification() {
        val channelId = "status_bar_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notif_channel_overlay_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_overlay_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notif_overlay_title))
            .setContentText(getString(R.string.notif_overlay_text))
            .setSmallIcon(R.drawable.ic_status_bolt)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!StatusBarAccessibilityService.isRunning && StatusBarPermissionHelper.hasOverlayPermission(this)) {
            if (!isViewAttached || binding == null) {
                initOverlayView()
            }
        }
    }

    /** Removes this service's bar (the accessibility service shows its own); the service keeps running. */
    private fun removeOverlayView() {
        if (isViewAttached && binding != null) {
            try {
                windowManager?.removeView(binding?.root)
            } catch (_: Exception) {}
        }
        binding = null
        isViewAttached = false
        autoHider = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
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
        autoHider = null
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
        var isRunning: Boolean = false
            private set

        private var instance: StatusBarOverlayService? = null

        /** The accessibility service took over drawing the bar: hide this copy, keep the service. */
        fun releaseOverlayToAccessibility() {
            instance?.removeOverlayView()
        }

        const val EXTRA_THEME_RES = "extra_theme_res"
        const val EXTRA_IS_GRADIENT = "extra_is_gradient"
        const val EXTRA_MASCOT_RES = "extra_mascot_res"

        fun start(
            context: Context,
            themeDrawableRes: Int? = null,
            isGradient: Boolean = true,
            mascotRes: Int? = null
        ) {
            val intent = Intent(context, StatusBarOverlayService::class.java).apply {
                themeDrawableRes?.let { putExtra(EXTRA_THEME_RES, it) }
                putExtra(EXTRA_IS_GRADIENT, isGradient)
                mascotRes?.let { putExtra(EXTRA_MASCOT_RES, it) }
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, StatusBarOverlayService::class.java)
            context.stopService(intent)
        }
    }
}
