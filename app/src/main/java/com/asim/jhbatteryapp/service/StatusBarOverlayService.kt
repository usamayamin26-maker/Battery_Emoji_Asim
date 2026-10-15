package com.asim.jhbatteryapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundServiceWithNotification()

        hardwareObserver.startObserving(this) { hardwareState ->
            renderOverlay(hardwareState)
        }

        if (StatusBarPermissionHelper.hasOverlayPermission(this)) {
            initOverlayView()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isViewAttached) {
            initOverlayView()
        }

        if (!isViewAttached) {
            stopSelf()
            return START_NOT_STICKY
        }

        val themeRes = intent?.getIntExtra(EXTRA_THEME_RES, -1)?.takeIf { it != -1 }
            ?: preferenceRepository.getAppliedThemeDrawableRes()

        val isGradient = intent?.getBooleanExtra(EXTRA_IS_GRADIENT, true)
            ?: preferenceRepository.isAppliedThemeGradient()

        val mascotRes = intent?.getIntExtra(EXTRA_MASCOT_RES, -1)?.takeIf { it != -1 }
            ?: preferenceRepository.getAppliedMascotDrawableRes()

        val isEmojiBatteryActive = preferenceRepository.isEmojiBatteryEnabled()

        applyOverlayTheme(themeRes, isGradient, mascotRes, isEmojiBatteryActive)

        return START_STICKY
    }

    private fun initOverlayView() {
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

            hardwareObserver.refreshState(this, null)
            renderOverlay(hardwareObserver.currentState)
        } catch (e: SecurityException) {
            e.printStackTrace()
            isViewAttached = false
            stopSelf()
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
            systemStatusBarHeight = getStatusBarHeight()
        )
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
            systemStatusBarHeight = getStatusBarHeight()
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
                "Status Bar Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live theme and emoji battery customization over status bar"
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
            .setContentTitle("JH Status Bar Theme")
            .setContentText("Status bar theme & emoji battery active")
            .setSmallIcon(R.drawable.ic_status_bolt)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!isViewAttached || binding == null) {
            initOverlayView()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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
        private const val NOTIFICATION_ID = 2001
        var isRunning: Boolean = false
            private set

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
