package com.jhw.batteryemoji.core.Utils

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

/**
 * Shimmer placeholder shown while a remote image is downloading.
 * A soft rounded block with a light band sweeping across it. The animation runs only while the
 * drawable is attached to a view: once an ImageView swaps it for the real image (or the view goes
 * away) the callback is cleared and the animator stops itself, so nothing keeps running.
 */
class LoadingShimmerDrawable(
    private val cornerRadiusPx: Float,
    baseColor: Int = 0x14000000,
    private val highlightColor: Int = 0x55FFFFFF
) : Drawable() {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = baseColor }
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val shaderMatrix = Matrix()
    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1100L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            if (callback == null) {
                cancel()
                return@addUpdateListener
            }
            progress = it.animatedValue as Float
            invalidateSelf()
        }
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        rect.set(bounds)
        val bandWidth = bounds.width().coerceAtLeast(1).toFloat()
        shimmerPaint.shader = LinearGradient(
            0f, 0f, bandWidth, 0f,
            intArrayOf(0x00FFFFFF, highlightColor, 0x00FFFFFF),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        if (rect.isEmpty) return
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, basePaint)

        // Band travels from fully left of the block to fully right of it
        val width = rect.width()
        shaderMatrix.setTranslate(rect.left - width + progress * width * 2f, 0f)
        shimmerPaint.shader?.setLocalMatrix(shaderMatrix)
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shimmerPaint)

        if (!animator.isStarted && callback != null) animator.start()
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        if (!visible) animator.cancel()
        return super.setVisible(visible, restart)
    }

    override fun setAlpha(alpha: Int) {
        basePaint.alpha = alpha
        shimmerPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        basePaint.colorFilter = colorFilter
        shimmerPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
