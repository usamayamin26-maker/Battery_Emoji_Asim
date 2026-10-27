package com.jhw.batteryemoji.presentation.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.core.Utils.MyUtils
import com.jhw.batteryemoji.databinding.DialogNotifyPermissionBinding

/**
 * Generic reusable dialog and permission helper matching the design specification:
 * - Illustration at top with document & checkmark badge
 * - "Notify Permission" heading
 * - Explanatory subtitle
 * - "Not Now" (outlined purple) and "Allow" (gradient pink/purple) action buttons
 * Built using MyUtils.createDialogWithBinding in core.
 */
object NotificationPermissionDialog {

    /**
     * Checks whether the app has notification posting permission.
     * On Android 13+ (API 33+), checks POST_NOTIFICATIONS runtime permission.
     * On earlier Android versions, checks NotificationManagerCompat notification enablement.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Shows the generic notification permission modal dialog using MyUtils.createDialogWithBinding.
     */
    fun show(
        context: Context,
        title: String? = null,
        description: String? = null,
        onAllow: () -> Unit,
        onNotNow: (() -> Unit)? = null
    ) {
        MyUtils.createDialogWithBinding(
            bindingInflater = DialogNotifyPermissionBinding::inflate,
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

            if (title != null) {
                binding.tvNotifyTitle.text = title
            }
            if (description != null) {
                binding.tvNotifyDescription.text = description
            }

            var actionInvoked = false
            dialog.setOnCancelListener {
                if (!actionInvoked) {
                    actionInvoked = true
                    onNotNow?.invoke()
                }
            }

            binding.btnNotifyNotNow.setOnClickListener {
                if (!actionInvoked) {
                    actionInvoked = true
                    dialog.dismiss()
                    onNotNow?.invoke()
                }
            }

            binding.btnNotifyAllow.setOnClickListener {
                if (!actionInvoked) {
                    actionInvoked = true
                    dialog.dismiss()
                    onAllow.invoke()
                }
            }
        }
    }
}
