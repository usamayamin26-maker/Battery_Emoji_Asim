package com.asim.jhbatteryapp.core.Utils

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.viewbinding.ViewBinding
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.BottomSheetExitBinding
import com.asim.jhbatteryapp.databinding.DialogDontMissOutBinding
import com.asim.jhbatteryapp.databinding.DialogExitBinding
import com.asim.jhbatteryapp.databinding.DialogNoInternetBinding
import com.asim.jhbatteryapp.databinding.DialogShareAppBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object MyUtils {

    /**
     * Creates and displays a generic dialog using ViewBinding and Theme.JHBatteryApp.Dialog.
     */
    fun <T : ViewBinding> createDialogWithBinding(
        bindingInflater: (LayoutInflater) -> T,
        @StyleRes style: Int = R.style.Theme_JHBatteryApp_Dialog,
        isCancelable: Boolean = false,
        context: Context,
        action: (Dialog, T) -> Unit
    ) {
        val binding = bindingInflater(LayoutInflater.from(context))
        val dialog = Dialog(context, style).apply {
            setContentView(binding.root)
            setCancelable(isCancelable)
        }

        action(dialog, binding)
        dialog.show()
    }

    /**
     * Creates and displays a generic bottom sheet dialog using ViewBinding.
     */
    inline fun <T : ViewBinding> genBottomSheetDialog(
        context: Context,
        @StyleRes style: Int = R.style.Style_Rounded_Bottom_Sheet,
        isCancelable: Boolean = false,
        crossinline bindingInflater: (LayoutInflater) -> T,
        crossinline action: BottomSheetDialog.(T) -> Unit
    ) {
        val dialog = BottomSheetDialog(context, style)
        val binding = bindingInflater.invoke(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        val bottomSheet = dialog.delegate.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            it.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.setBackgroundResource(R.drawable.bottom_sheet_shap)
        }

        dialog.setCancelable(isCancelable)
        dialog.action(binding)
        dialog.show()
    }

    /**
     * Checks if the device has an active, working internet connection using ConnectivityManager.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val caps = cm.getNetworkCapabilities(activeNetwork)
                    if (caps != null && (
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )) {
                        return true
                    }
                }
                cm.allNetworks.any { network ->
                    val caps = cm.getNetworkCapabilities(network)
                    caps != null && (
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                val netInfo = cm.activeNetworkInfo
                netInfo != null && netInfo.isConnected
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Displays the "No Internet Connection" dialog.
     * Retry button verifies connectivity via ConnectivityManager before dismissing.
     */
    fun showNoInternetDialog(
        context: Context,
        onRetry: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        createDialogWithBinding(
            bindingInflater = DialogNoInternetBinding::inflate,
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

            binding.btnNoInternetCancel.setOnClickListener {
                dialog.dismiss()
                onCancel?.invoke()
            }

            binding.btnNoInternetRetry.setOnClickListener {
                if (isNetworkAvailable(context)) {
                    dialog.dismiss()
                    onRetry?.invoke()
                } else {
                    Toast.makeText(context, context.getString(R.string.no_connection_msg), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Displays the "Are you sure you want to exit?" dialog matching the design.
     */
    fun showExitDialog(
        context: Context,
        onExit: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        createDialogWithBinding(
            bindingInflater = DialogExitBinding::inflate,
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

            (context as? android.app.Activity)?.let { activity ->
                com.universal.remote.ads.AdsController.slot(
                    activity = activity,
                    container = binding.adSlotBottom,
                    slotKey = "exit_bottom",
                    units = "exit"
                )
            }

            dialog.setOnDismissListener {
                com.universal.remote.ads.AdsController.forgetSlots("exit")
            }

            binding.btnExitYes.setOnClickListener {
                dialog.dismiss()
                onExit.invoke()
            }

            binding.btnExitNo.setOnClickListener {
                dialog.dismiss()
                onCancel?.invoke()
            }
        }
    }

    /**
     * Displays the "Are you sure you want to exit?" as a BottomSheet using genBottomSheetDialog.
     */
    fun showExitBottomSheet(
        context: Context,
        onExit: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        genBottomSheetDialog(
            context = context,
            style = R.style.Style_Rounded_Bottom_Sheet,
            isCancelable = true,
            bindingInflater = BottomSheetExitBinding::inflate
        ) { binding ->
            (context as? android.app.Activity)?.let { activity ->
                com.universal.remote.ads.AdsController.slot(
                    activity = activity,
                    container = binding.adSlotBottom,
                    slotKey = "exit_bottom",
                    units = "exit"
                )
            }

            setOnDismissListener {
                com.universal.remote.ads.AdsController.forgetSlots("exit")
            }

            binding.btnExitYes.setOnClickListener {
                dismiss()
                onExit.invoke()
            }

            binding.btnExitNo.setOnClickListener {
                dismiss()
                onCancel?.invoke()
            }
        }
    }

    /**
     * Displays the "Don't Miss Out!!" notification promotional dialog.
     */
    fun showDontMissOutDialog(
        context: Context,
        onTurnOn: () -> Unit,
        onMaybeLater: (() -> Unit)? = null
    ) {
        createDialogWithBinding(
            bindingInflater = DialogDontMissOutBinding::inflate,
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

            var actionInvoked = false
            dialog.setOnCancelListener {
                if (!actionInvoked) {
                    actionInvoked = true
                    onMaybeLater?.invoke()
                }
            }

            binding.btnDontMissLater.setOnClickListener {
                if (!actionInvoked) {
                    actionInvoked = true
                    dialog.dismiss()
                    onMaybeLater?.invoke()
                }
            }

            binding.btnDontMissTurnOn.setOnClickListener {
                if (!actionInvoked) {
                    actionInvoked = true
                    dialog.dismiss()
                    onTurnOn.invoke()
                }
            }
        }
    }

    /**
     * Displays the "Share Emoji app" dialog using createDialogWithBinding.
     */
    fun showShareDialog(context: Context) {
        createDialogWithBinding(
            bindingInflater = DialogShareAppBinding::inflate,
            isCancelable = true,
            context = context
        ) { dialog, binding ->
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (context.resources.displayMetrics.widthPixels * 0.90).toInt().coerceAtMost(
                        (420 * context.resources.displayMetrics.density).toInt()
                    ),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            // Copy Link Action
            binding.btnCopyLink.setOnClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Share Link", binding.tvShareUrl.text.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
            }

            binding.btnShareDone.setOnClickListener {
                dialog.dismiss()
            }
        }
    }
}