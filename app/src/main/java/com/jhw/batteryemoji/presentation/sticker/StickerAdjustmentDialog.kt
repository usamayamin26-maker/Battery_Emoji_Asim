package com.jhw.batteryemoji.presentation.sticker

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import android.widget.SeekBar
import com.jhw.batteryemoji.core.Utils.MyUtils
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.DialogStickerAdjustmentBinding
import com.jhw.batteryemoji.domain.model.StickerItem
import com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper
import com.jhw.batteryemoji.service.FloatingStickerService
import com.jhw.batteryemoji.service.StatusBarAccessibilityService

class StickerAdjustmentDialog(
    private val context: Context,
    private val sticker: StickerItem,
    private val onApply: (offsetX: Float, offsetY: Float, sizeDp: Int, rotationDeg: Float) -> Unit
) {

    private var currentOffsetX = sticker.offsetX
    private var currentOffsetY = sticker.offsetY
    private var currentSize = sticker.sizeDp.coerceIn(30, 100)
    private var currentRotate = ((sticker.rotationDeg / 3.6f) + 50f).toInt().coerceIn(0, 100)

    fun show() {
        MyUtils.genBottomSheetDialog(
            context = context,
            isCancelable = true,
            bindingInflater = DialogStickerAdjustmentBinding::inflate
        ) { binding ->
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Keep the screen behind the adjustment sheet bright and clear
            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setDimAmount(0f)
            // Allow user touches outside the sheet to drag the floating sticker directly on screen
            window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)

            fun updatePreview() {
                binding.ivAdjustmentSticker.translationX = currentOffsetX
                binding.ivAdjustmentSticker.translationY = currentOffsetY

                val scale = 0.5f + (currentSize / 100f) * 0.8f
                binding.ivAdjustmentSticker.scaleX = scale
                binding.ivAdjustmentSticker.scaleY = scale

                val degrees = (currentRotate - 50) * 3.6f
                binding.ivAdjustmentSticker.rotation = degrees
            }

            // Setup views
            RemoteImageLoader.bindSticker(binding.ivAdjustmentSticker, sticker.drawableRes, sticker.imagePath)
            binding.sbAdjustmentSize.progress = currentSize
            binding.tvSizeValue.text = "${currentSize}dp"
            binding.sbAdjustmentRotate.progress = currentRotate
            binding.tvRotateValue.text = "${currentRotate}dp"

            // Setup listeners
            binding.btnCloseAdjustment.setOnClickListener {
                dismiss()
            }

            // D-Pad Directional Controls (Moves floating sticker on actual screen live!)
            binding.btnDpadUp.setOnClickListener {
                currentOffsetY -= 16f
                updatePreview()
                FloatingStickerService.nudgePosition(0, -35)
            }

            binding.btnDpadDown.setOnClickListener {
                currentOffsetY += 16f
                updatePreview()
                FloatingStickerService.nudgePosition(0, 35)
            }

            binding.btnDpadLeft.setOnClickListener {
                currentOffsetX -= 16f
                updatePreview()
                FloatingStickerService.nudgePosition(-35, 0)
            }

            binding.btnDpadRight.setOnClickListener {
                currentOffsetX += 16f
                updatePreview()
                FloatingStickerService.nudgePosition(35, 0)
            }

            // Size Slider (Scales floating sticker on screen live)
            binding.sbAdjustmentSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    currentSize = progress.coerceIn(20, 120)
                    binding.tvSizeValue.text = "${currentSize}dp"
                    updatePreview()
                    FloatingStickerService.updateSize(currentSize)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Rotate Slider (Rotates floating sticker on screen live)
            binding.sbAdjustmentRotate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    currentRotate = progress.coerceIn(0, 100)
                    binding.tvRotateValue.text = "${currentRotate}dp"
                    updatePreview()
                    val deg = (currentRotate - 50) * 3.6f
                    FloatingStickerService.updateRotation(deg)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Apply Button
            binding.btnAdjustmentApply.setOnClickListener {
                val rotationDeg = (currentRotate - 50) * 3.6f
                onApply(currentOffsetX, currentOffsetY, currentSize, rotationDeg)
                dismiss()
            }

            updatePreview()

            // Ensure floating sticker is active on screen for live visual adjustment
            if (StatusBarPermissionHelper.hasOverlayPermission(context) ||
                StatusBarAccessibilityService.isAccessibilityServiceEnabled(context)) {
                FloatingStickerService.start(
                    context = context,
                    drawableRes = sticker.drawableRes,
                    imagePath = sticker.imagePath,
                    sizeDp = currentSize,
                    rotationDeg = ((currentRotate - 50) * 3.6f)
                )
                FloatingStickerService.updateStickerDrawable(sticker.drawableRes, sticker.imagePath)
            }
        }
    }
}
