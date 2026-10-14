package com.asim.jhbatteryapp.presentation.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Keep

/**
 * Reusable Linear Gradient Progress Indicator Component
 * Renders a pill-shaped progress bar with a smooth multi-color linear gradient
 * from purple -> magenta -> pink, matching the design.
 */
class GradientProgressIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4DFFFFFF") // Translucent soft white track
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val trackRect = RectF()
    private val progressRect = RectF()

    var max: Int = 100
        set(value) {
            field = if (value > 0) value else 100
            postInvalidate()
        }

    @get:Keep
    @set:Keep
    var progress: Int = 0
        set(value) {
            val clamped = value.coerceIn(0, max)
            if (field != clamped) {
                field = clamped
                postInvalidate()
            }
        }

    // Gradient colors matching the user's uploaded image
    @ColorInt private var startColor: Int = Color.parseColor("#9241FD")   // Purple
    @ColorInt private var centerColor: Int = Color.parseColor("#D424AA")  // Magenta
    @ColorInt private var endColor: Int = Color.parseColor("#F1337B")     // Pink

    private var gradientShader: LinearGradient? = null

    fun setGradientColors(@ColorInt start: Int, @ColorInt center: Int, @ColorInt end: Int) {
        startColor = start
        centerColor = center
        endColor = end
        gradientShader = null
        postInvalidate()
    }

    fun setTrackColor(@ColorInt color: Int) {
        trackPaint.color = color
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradientShader = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val radius = h / 2f

        // 1. Draw Background Track
        trackRect.set(0f, 0f, w, h)
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        // 2. Draw Progress Bar with Gradient
        val progressRatio = progress.toFloat() / max.toFloat()
        val progressWidth = w * progressRatio

        if (progressWidth > 0) {
            if (gradientShader == null) {
                gradientShader = LinearGradient(
                    0f, 0f, w, 0f,
                    intArrayOf(startColor, centerColor, endColor),
                    floatArrayOf(0.0f, 0.5f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                progressPaint.shader = gradientShader
            }

            // Keep pill shape for the progress bar
            val progressCornerRadius = radius.coerceAtMost(progressWidth / 2f)
            progressRect.set(0f, 0f, progressWidth, h)
            canvas.drawRoundRect(progressRect, progressCornerRadius, progressCornerRadius, progressPaint)
        }
    }
}
