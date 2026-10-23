package com.asim.jhbatteryapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.remote.RemoteImageLoader
import com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl
import com.asim.jhbatteryapp.databinding.LayoutFloatingStickerBinding
import com.asim.jhbatteryapp.domain.repository.PreferenceRepository
import com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper
import com.asim.jhbatteryapp.presentation.home.MainActivity

class FloatingStickerService : Service() {

    private var windowManager: WindowManager? = null
    private var binding: LayoutFloatingStickerBinding? = null
    var isViewAttached = false
        private set
    var layoutParams: WindowManager.LayoutParams? = null
        private set

    val preferenceRepository: PreferenceRepository by lazy {
        PreferenceRepositoryImpl()
    }

    var currentX: Int = -1
        private set
    var currentY: Int = -1
        private set
    var currentSizeDp: Int = 60
        private set
    var currentRotationDeg: Float = 0f
        private set
    var currentDrawableRes: Int = R.drawable.ic_sticker_player_blue
        private set
    var currentImagePath: String = ""
        private set

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        startForegroundServiceWithNotification()

        // The accessibility service already shows the sticker; a second copy would show twice
        if (StatusBarAccessibilityService.isRunning) {
            stopSelf()
            return
        }

        loadSavedState()

        if (StatusBarPermissionHelper.hasOverlayPermission(this)) {
            initFloatingView()
        }
    }

    private fun loadSavedState() {
        val dm = resources.displayMetrics
        currentSizeDp = preferenceRepository.getFloatingStickerSize().coerceIn(30, 150)
        currentRotationDeg = preferenceRepository.getFloatingStickerRotation()
        currentDrawableRes = preferenceRepository.getFloatingStickerDrawableRes()
        currentImagePath = preferenceRepository.getFloatingStickerImagePath()

        val savedX = preferenceRepository.getFloatingStickerX()
        val savedY = preferenceRepository.getFloatingStickerY()

        val sizePx = (currentSizeDp * dm.density).toInt()
        val defaultX = (dm.widthPixels / 2) - (sizePx / 2)
        val defaultY = (dm.heightPixels / 3)

        currentX = if (savedX <= 0 || savedX >= dm.widthPixels) defaultX else savedX
        currentY = if (savedY <= 0 || savedY >= dm.heightPixels) defaultY else savedY
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP || StatusBarAccessibilityService.isRunning) {
            stopSelf()
            return START_NOT_STICKY
        }

        val res = intent?.getIntExtra(EXTRA_DRAWABLE_RES, -1) ?: -1
        if (res != -1) {
            currentDrawableRes = res
            currentImagePath = intent?.getStringExtra(EXTRA_IMAGE_PATH).orEmpty()
            preferenceRepository.setFloatingStickerDrawableRes(res)
            preferenceRepository.setFloatingStickerImagePath(currentImagePath)
        }

        val x = intent?.getIntExtra(EXTRA_X, -1) ?: -1
        if (x != -1) {
            currentX = x
            preferenceRepository.setFloatingStickerX(x)
        }

        val y = intent?.getIntExtra(EXTRA_Y, -1) ?: -1
        if (y != -1) {
            currentY = y
            preferenceRepository.setFloatingStickerY(y)
        }

        val size = intent?.getIntExtra(EXTRA_SIZE_DP, -1) ?: -1
        if (size != -1) {
            currentSizeDp = size
            preferenceRepository.setFloatingStickerSize(size)
        }

        val rot = intent?.getFloatExtra(EXTRA_ROT_DEG, Float.NaN) ?: Float.NaN
        if (!rot.isNaN()) {
            currentRotationDeg = rot
            preferenceRepository.setFloatingStickerRotation(rot)
        }

        preferenceRepository.setFloatingStickerEnabled(true)

        if (!isViewAttached) {
            if (StatusBarPermissionHelper.hasOverlayPermission(this)) {
                initFloatingView()
            }
        } else {
            updateViewVisuals()
        }

        return START_STICKY
    }

    fun initFloatingView() {
        if (isViewAttached) return

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager == null) return

            val inflater = LayoutInflater.from(this)
            binding = LayoutFloatingStickerBinding.inflate(inflater)

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = currentX
                this.y = currentY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            layoutParams = lp
            setupTouchDragListener()

            val density = resources.displayMetrics.density
            binding?.ivFloatingSticker?.let {
                RemoteImageLoader.bindSticker(it, currentDrawableRes, currentImagePath)
            }
            val sizePx = (currentSizeDp * density).toInt()
            val ivParams = binding?.ivFloatingSticker?.layoutParams
            if (ivParams != null) {
                ivParams.width = sizePx
                ivParams.height = sizePx
                binding?.ivFloatingSticker?.layoutParams = ivParams
            }
            binding?.ivFloatingSticker?.rotation = currentRotationDeg

            windowManager?.addView(binding?.root, lp)
            isViewAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
            isViewAttached = false
        }
    }

    private fun setupTouchDragListener() {
        val root = binding?.root ?: return

        root.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val params = layoutParams ?: return false
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
                        currentX = params.x
                        currentY = params.y

                        try {
                            wm.updateViewLayout(binding?.root, params)
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

    fun applyOffsetNudge(deltaX: Int, deltaY: Int) {
        val params = layoutParams ?: return
        val wm = windowManager ?: return

        params.x += deltaX
        params.y += deltaY
        currentX = params.x
        currentY = params.y

        preferenceRepository.setFloatingStickerX(currentX)
        preferenceRepository.setFloatingStickerY(currentY)

        try {
            wm.updateViewLayout(binding?.root, params)
        } catch (_: Exception) {}
    }

    fun applySize(sizeDp: Int) {
        currentSizeDp = sizeDp
        preferenceRepository.setFloatingStickerSize(sizeDp)
        updateViewVisuals()
    }

    fun applyRotation(rotDeg: Float) {
        currentRotationDeg = rotDeg
        preferenceRepository.setFloatingStickerRotation(rotDeg)
        updateViewVisuals()
    }

    fun applyDrawable(drawableRes: Int, imagePath: String? = null) {
        currentDrawableRes = drawableRes
        currentImagePath = imagePath.orEmpty()
        preferenceRepository.setFloatingStickerDrawableRes(drawableRes)
        preferenceRepository.setFloatingStickerImagePath(currentImagePath)
        updateViewVisuals()
    }

    private fun updateViewVisuals() {
        val b = binding ?: return
        val density = resources.displayMetrics.density

        RemoteImageLoader.bindSticker(b.ivFloatingSticker, currentDrawableRes, currentImagePath)

        val sizePx = (currentSizeDp * density).toInt()
        val ivParams = b.ivFloatingSticker.layoutParams
        if (ivParams != null) {
            ivParams.width = sizePx
            ivParams.height = sizePx
            b.ivFloatingSticker.layoutParams = ivParams
        }
        b.root.requestLayout()

        b.ivFloatingSticker.rotation = currentRotationDeg

        if (isViewAttached) {
            layoutParams?.let { lp ->
                lp.x = currentX
                lp.y = currentY
                try {
                    windowManager?.updateViewLayout(b.root, lp)
                } catch (_: Exception) {}
            }
        }
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "floating_sticker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notif_channel_sticker_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_sticker_desc)
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
            .setContentTitle(getString(R.string.notif_sticker_title))
            .setContentText(getString(R.string.notif_sticker_text))
            .setSmallIcon(R.drawable.ic_sparkle)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
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
        // If floating sticker is enabled, keep it running when app is closed
        // (unless the accessibility service shows it; it outlives the app on its own)
        if (preferenceRepository.isFloatingStickerEnabled() && !StatusBarAccessibilityService.isRunning) {
            val restartIntent = Intent(applicationContext, FloatingStickerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false

        if (isViewAttached && binding != null) {
            try {
                windowManager?.removeView(binding?.root)
            } catch (_: Exception) {}
        }
        binding = null
        isViewAttached = false
    }

    companion object {
        private const val NOTIFICATION_ID = 2002
        private const val ACTION_STOP = "action_stop_floating_sticker"

        const val EXTRA_DRAWABLE_RES = "extra_drawable_res"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"
        const val EXTRA_SIZE_DP = "extra_size_dp"
        const val EXTRA_ROT_DEG = "extra_rot_deg"

        var isRunning: Boolean = false
            private set

        var instance: FloatingStickerService? = null
            private set

        fun start(
            context: Context,
            drawableRes: Int? = null,
            imagePath: String? = null,
            x: Int? = null,
            y: Int? = null,
            sizeDp: Int? = null,
            rotationDeg: Float? = null
        ) {
            // Always ensure foreground service is running to prevent process death on app close
            StatusBarOverlayService.start(context)

            // Prioritize Accessibility Service overlay (TYPE_ACCESSIBILITY_OVERLAY)
            if (StatusBarAccessibilityService.isAccessibilityServiceEnabled(context) &&
                StatusBarAccessibilityService.instance != null
            ) {
                handOverToAccessibility()
                // No sticker passed (e.g. restore on app start): keep the saved one instead of
                // resetting to the default, so a remote sticker survives relaunch
                val prefs = PreferenceRepositoryImpl()
                StatusBarAccessibilityService.showFloatingSticker(
                    drawableRes = drawableRes ?: prefs.getFloatingStickerDrawableRes(),
                    imagePath = if (drawableRes != null) imagePath else prefs.getFloatingStickerImagePath(),
                    sizeDp = sizeDp ?: 60,
                    rotationDeg = rotationDeg ?: 0f,
                    x = x,
                    y = y
                )
                return
            }

            val inst = instance
            if (inst != null) {
                drawableRes?.let { inst.applyDrawable(it, imagePath) }
                sizeDp?.let { inst.applySize(it) }
                rotationDeg?.let { inst.applyRotation(it) }
                if (x != null && y != null) {
                    inst.currentX = x
                    inst.currentY = y
                    inst.preferenceRepository.setFloatingStickerX(x)
                    inst.preferenceRepository.setFloatingStickerY(y)
                    inst.layoutParams?.let { lp ->
                        lp.x = x
                        lp.y = y
                        if (inst.isViewAttached) {
                            try {
                                inst.windowManager?.updateViewLayout(inst.binding?.root, lp)
                            } catch (_: Exception) {}
                        }
                    }
                }
                if (!inst.isViewAttached && StatusBarPermissionHelper.hasOverlayPermission(context)) {
                    inst.initFloatingView()
                }
                return
            }

            val intent = Intent(context, FloatingStickerService::class.java).apply {
                drawableRes?.let { putExtra(EXTRA_DRAWABLE_RES, it) }
                imagePath?.let { putExtra(EXTRA_IMAGE_PATH, it) }
                x?.let { putExtra(EXTRA_X, it) }
                y?.let { putExtra(EXTRA_Y, it) }
                sizeDp?.let { putExtra(EXTRA_SIZE_DP, it) }
                rotationDeg?.let { putExtra(EXTRA_ROT_DEG, it) }
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

        /**
         * Stops this fallback once the accessibility service shows the sticker. Unlike [stop] it
         * leaves the "sticker enabled" setting and the accessibility sticker alone.
         */
        fun handOverToAccessibility() {
            instance?.stopSelf()
        }

        fun stop(context: Context) {
            StatusBarAccessibilityService.stopFloatingSticker()
            val intent = Intent(context, FloatingStickerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun nudgePosition(deltaX: Int, deltaY: Int) {
            StatusBarAccessibilityService.nudgeFloatingSticker(deltaX, deltaY)
            instance?.applyOffsetNudge(deltaX, deltaY)
        }

        fun updateSize(sizeDp: Int) {
            StatusBarAccessibilityService.updateFloatingStickerSize(sizeDp)
            instance?.applySize(sizeDp)
        }

        fun updateRotation(rotDeg: Float) {
            StatusBarAccessibilityService.updateFloatingStickerRotation(rotDeg)
            instance?.applyRotation(rotDeg)
        }

        fun updateStickerDrawable(drawableRes: Int, imagePath: String? = null) {
            StatusBarAccessibilityService.updateFloatingStickerDrawable(drawableRes, imagePath)
            instance?.applyDrawable(drawableRes, imagePath)
        }
    }
}
