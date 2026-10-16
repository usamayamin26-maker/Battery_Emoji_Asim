package com.asim.jhbatteryapp.presentation.sticker

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.SeekBar
import com.asim.jhbatteryapp.databinding.DialogStickerAdjustmentBinding
import com.asim.jhbatteryapp.domain.model.StickerItem
import com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper
import com.asim.jhbatteryapp.service.FloatingStickerService
import com.asim.jhbatteryapp.service.StatusBarAccessibilityService
import com.google.android.material.bottomsheet.BottomSheetDialog

class StickerAdjustmentDialog(
    private val context: Context,
    private val sticker: StickerItem,
    private val onApply: (offsetX: Float, offsetY: Float, sizeDp: Int, rotationDeg: Float) -> Unit
) {

    private val dialog = BottomSheetDialog(context)
    private val binding = DialogStickerAdjustmentBinding.inflate(LayoutInflater.from(context))

    private var currentOffsetX = sticker.offsetX
    private var currentOffsetY = sticker.offsetY
    private var currentSize = sticker.sizeDp.coerceIn(30, 100)
    private var currentRotate = ((sticker.rotationDeg / 3.6f) + 50f).toInt().coerceIn(0, 100)

    init {
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Keep the screen behind the adjustment sheet bright and clear
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0f)
        // Allow user touches outside the sheet to drag the floating sticker directly on screen
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)

        setupViews()
        setupListeners()
        updatePreview()

        // Ensure floating sticker is active on screen for live visual adjustment
        if (StatusBarPermissionHelper.hasOverlayPermission(context) ||
            StatusBarAccessibilityService.isAccessibilityServiceEnabled(context)) {
            FloatingStickerService.start(
                context = context,
                drawableRes = sticker.drawableRes,
                sizeDp = currentSize,
                rotationDeg = ((currentRotate - 50) * 3.6f)
            )
            FloatingStickerService.updateStickerDrawable(sticker.drawableRes)
        }
    }

    private fun setupViews() {
        binding.ivAdjustmentSticker.setImageResource(sticker.drawableRes)

        binding.sbAdjustmentSize.progress = currentSize
        binding.tvSizeValue.text = "${currentSize}dp"

        binding.sbAdjustmentRotate.progress = currentRotate
        binding.tvRotateValue.text = "${currentRotate}dp"
    }

    private fun setupListeners() {
        binding.btnCloseAdjustment.setOnClickListener {
            dialog.dismiss()
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
            dialog.dismiss()
        }
    }

    private fun updatePreview() {
        binding.ivAdjustmentSticker.translationX = currentOffsetX
        binding.ivAdjustmentSticker.translationY = currentOffsetY

        val scale = 0.5f + (currentSize / 100f) * 0.8f
        binding.ivAdjustmentSticker.scaleX = scale
        binding.ivAdjustmentSticker.scaleY = scale

        val degrees = (currentRotate - 50) * 3.6f
        binding.ivAdjustmentSticker.rotation = degrees
    }

    fun show() {
        dialog.show()
    }
}
