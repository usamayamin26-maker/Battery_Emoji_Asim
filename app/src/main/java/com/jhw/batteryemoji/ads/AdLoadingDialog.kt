package com.universal.remote.ads

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.jhw.batteryemoji.databinding.DialogAdLoadingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Non-cancelable "Ad Loading.." dialog shown while a button-triggered interstitial loads. */
object AdLoadingDialog {

    fun show(activity: Activity): AlertDialog {
        val dialogBinding = DialogAdLoadingBinding.inflate(activity.layoutInflater)
        return MaterialAlertDialogBuilder(activity)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .show()
    }
}

/**
 * Dismisses an ad loading dialog without crashing when its activity's window is already gone
 * (activity finished / recreated while the ad was loading or showing). A plain dismiss() then
 * throws "View ... not attached to window manager" (Crashlytics, LanguageActivity).
 */
fun android.app.Dialog.dismissSafely() {
    try {
        val owner = ownerActivity ?: (context as? android.content.ContextWrapper)?.let { unwrapActivity(it) }
        if (owner != null && owner.isDestroyed) return
        if (!isShowing) return
        if (window?.decorView?.isAttachedToWindow != true) return
        dismiss()
    } catch (_: IllegalArgumentException) {
        // Window already detached - nothing left to dismiss.
    } catch (_: IllegalStateException) {
    }
}

private fun unwrapActivity(context: android.content.Context): Activity? {
    var c: android.content.Context? = context
    while (c is android.content.ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
