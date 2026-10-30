package com.jhw.batteryemoji.presentation.common

import android.app.AppOpsManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.core.Utils.MyUtils
import com.jhw.batteryemoji.databinding.DialogOverlayPermissionBinding

/**
 * Discretely distinguishes between overlay permission, accessibility setting, and live running service.
 */
data class AppPermissionState(
    val hasOverlayPermission: Boolean,
    val isAccessibilityEnabled: Boolean,
    val isAccessibilityRunning: Boolean
) {
    val isFullyReady: Boolean
        get() = isAccessibilityRunning || (isAccessibilityEnabled && hasOverlayPermission)
}

object StatusBarPermissionHelper {

    fun getPermissionState(context: Context): AppPermissionState {
        val overlay = hasOverlayPermission(context)
        val accessRunning = com.jhw.batteryemoji.service.StatusBarAccessibilityService.isRunning
        val accessEnabled = com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(context)
        return AppPermissionState(
            hasOverlayPermission = overlay,
            isAccessibilityEnabled = accessEnabled,
            isAccessibilityRunning = accessRunning
        )
    }

    /**
     * Multi-tier permission check:
     * 1. Settings.canDrawOverlays(context) with Activity context
     * 2. Settings.canDrawOverlays(appContext) fallback
     * 3. AppOpsManager SYSTEM_ALERT_WINDOW check
     * 4. Ground-truth WindowManager probe test (bypasses OEM caching delay)
     */
    fun hasOverlayPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        // Tier 1: Check standard API with the provided context (Activity context preferred)
        try {
            if (Settings.canDrawOverlays(context)) {
                return true
            }
        } catch (_: Exception) {}

        // Tier 1b: Fallback with applicationContext
        try {
            val appCtx = context.applicationContext
            if (appCtx != null && Settings.canDrawOverlays(appCtx)) {
                return true
            }
        } catch (_: Exception) {}

        // Tier 2: AppOpsManager check
        try {
            val appOps = (context.getSystemService(Context.APP_OPS_SERVICE)
                ?: context.applicationContext?.getSystemService(Context.APP_OPS_SERVICE)) as? AppOpsManager
            val pkg = context.packageName ?: context.applicationContext?.packageName ?: ""
            if (appOps != null && pkg.isNotEmpty()) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                        Process.myUid(),
                        pkg
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                        Process.myUid(),
                        pkg
                    )
                }
                if (mode == AppOpsManager.MODE_ALLOWED) {
                    return true
                }
            }
        } catch (_: Exception) {}

        // Tier 3: Direct WindowManager probe test (bypasses OEM caching delay)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val wm = (context.getSystemService(Context.WINDOW_SERVICE)
                    ?: context.applicationContext?.getSystemService(Context.WINDOW_SERVICE)) as? WindowManager
                if (wm != null) {
                    val testView = View(context)
                    val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    }
                    val params = WindowManager.LayoutParams(
                        0, 0,
                        layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSPARENT
                    )
                    wm.addView(testView, params)
                    // If addView did not throw SecurityException, permission is granted!
                    try {
                        wm.removeViewImmediate(testView)
                    } catch (_: Exception) {
                        try {
                            wm.removeView(testView)
                        } catch (_: Exception) {}
                    }
                    return true
                }
            } catch (_: SecurityException) {
                return false
            } catch (_: Exception) {}
        }

        return false
    }

    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        return manufacturer.contains("Xiaomi", ignoreCase = true) ||
                brand.contains("Xiaomi", ignoreCase = true) ||
                brand.contains("Redmi", ignoreCase = true) ||
                brand.contains("POCO", ignoreCase = true)
    }

    fun openXiaomiPermissionScreen(context: Context): Boolean {
        com.jhw.batteryemoji.JHBatteryApplication.skipNextWelcome()
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
                if (context !is android.app.Activity) {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
            return true
        } catch (_: Exception) {}

        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                putExtra("extra_pkgname", context.packageName)
                if (context !is android.app.Activity) {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
            return true
        } catch (_: Exception) {}

        return false
    }

    fun openDisplayOverOtherApps(context: Context) {
        com.jhw.batteryemoji.JHBatteryApplication.skipNextWelcome()
        var opened = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    if (context !is android.app.Activity) {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
                context.startActivity(intent)
                opened = true
            } catch (_: Exception) {}
        }

        if (!opened && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    if (context !is android.app.Activity) {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
                context.startActivity(intent)
                opened = true
            } catch (_: Exception) {}
        }

        if (!opened) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    if (context !is android.app.Activity) {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openAutostartSettings(context: Context): Boolean {
        com.jhw.batteryemoji.JHBatteryApplication.skipNextWelcome()
        val intents = arrayOf(
            Intent().apply {
                component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            },
            Intent().apply {
                component = android.content.ComponentName(
                    "com.letv.android.letvsafe",
                    "com.letv.android.letvsafe.AutobootManageActivity"
                )
            },
            Intent().apply {
                component = android.content.ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            },
            Intent().apply {
                component = android.content.ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            },
            Intent().apply {
                component = android.content.ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
        )

        for (intent in intents) {
            try {
                if (context !is android.app.Activity) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            } catch (_: Exception) {}
        }
        return false
    }

    fun openPopUpWindowsSettings(context: Context) {
        com.jhw.batteryemoji.JHBatteryApplication.skipNextWelcome()
        if (!openXiaomiPermissionScreen(context)) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    if (context !is android.app.Activity) {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun showPermissionDialog(
        context: Context,
        onSettingsOpened: (() -> Unit)? = null,
        onProceed: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val binding = DialogOverlayPermissionBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        // Indicate if Display over other apps is already allowed
        if (hasOverlayPermission(context)) {
            binding.btnPermOverlay.text = context.getString(R.string.overlay_perm_step1_allowed)
            binding.btnPermOverlay.setTextColor(ContextCompat.getColor(context, R.color.battery_green))
        }

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        binding.btnPermOverlay.setOnClickListener {
            onSettingsOpened?.invoke()
            openDisplayOverOtherApps(context)
        }

        binding.btnPermPopup.setOnClickListener {
            onSettingsOpened?.invoke()
            openPopUpWindowsSettings(context)
        }

        binding.btnApplyNow.setOnClickListener {
            dialog.dismiss()
            onProceed()
        }

        binding.btnCancelPerm.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showAccessibilityDialog(
        context: Context,
        onSettingsOpened: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        MyUtils.createDialogWithBinding(
            bindingInflater = com.jhw.batteryemoji.databinding.DialogAccessibilityPermissionBinding::inflate,
            style = R.style.Theme_JHBatteryApp_Dialog,
            isCancelable = true,
            context = context
        ) { dialog, binding ->
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (context.resources.displayMetrics.widthPixels * 0.88).toInt().coerceAtMost(
                        (420 * context.resources.displayMetrics.density).toInt()
                    ),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            binding.tvBullet1.text = android.text.Html.fromHtml(
                context.getString(R.string.accessibility_usage_bullet_1),
                android.text.Html.FROM_HTML_MODE_COMPACT
            )
            binding.tvBullet2.text = android.text.Html.fromHtml(
                context.getString(R.string.accessibility_usage_bullet_2),
                android.text.Html.FROM_HTML_MODE_COMPACT
            )
            // The service performs no global actions (Home / Back / Recents), so a disclosure saying
            // it does would be misleading: keep only the bullet that describes what it really does
            binding.llBullet2.visibility = View.GONE
            binding.tvTerms.text = android.text.Html.fromHtml(
                context.getString(R.string.accessibility_usage_terms),
                android.text.Html.FROM_HTML_MODE_COMPACT
            )

//            binding.tvTerms.setOnClickListener {
//                binding.cbTerms.isChecked = !binding.cbTerms.isChecked
//            }

            // Set initial state for Continue button based on checkbox
            binding.btnAccessContinue.isEnabled = binding.cbTerms.isChecked
            binding.btnAccessContinue.alpha = if (binding.cbTerms.isChecked) 1.0f else 0.5f

            binding.cbTerms.setOnCheckedChangeListener { _, isChecked ->
                binding.btnAccessContinue.isEnabled = isChecked
                binding.btnAccessContinue.alpha = if (isChecked) 1.0f else 0.5f
            }

            var actionInvoked = false
            dialog.setOnCancelListener {
                if (!actionInvoked) {
                    actionInvoked = true
                    onDismissed?.invoke()
                }
            }

            binding.btnAccessClose.setOnClickListener {
                if (!actionInvoked) {
                    actionInvoked = true
                    dialog.dismiss()
                    onDismissed?.invoke()
                }
            }

            binding.btnAccessContinue.setOnClickListener {
                if (!binding.cbTerms.isChecked) {
                    Toast.makeText(context, context.getString(R.string.please_agree_terms), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (!actionInvoked) {
                    actionInvoked = true
                    dialog.dismiss()
                    onSettingsOpened?.invoke()
                    com.jhw.batteryemoji.presentation.guide.HowToUseActivity.start(context)
                }
            }
        }
    }
}
