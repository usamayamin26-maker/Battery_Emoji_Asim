package com.asim.jhbatteryapp.presentation.components
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.asim.jhbatteryapp.R

class GradientProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var offset = 0f
    private var animator: ValueAnimator? = null

    private val colors = intArrayOf(
        ContextCompat.getColor(context, R.color.primary_gradient_start),
        ContextCompat.getColor(context, R.color.primary_purple),
        ContextCompat.getColor(context, R.color.primary_gradient_end)
    )

    init {
        trackPaint.color = android.graphics.Color.parseColor("#E6EFFF")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w <= 0f || h <= 0f) return

        val radius = h / 2f

        // Rounded background track
        val trackRect = RectF(0f, 0f, w, h)

        canvas.drawRoundRect(
            trackRect,
            radius,
            radius,
            trackPaint
        )

        // Gradient colors
        gradientPaint.shader = LinearGradient(
            0f, 0f, w, 0f,
            colors,
            null,
            Shader.TileMode.CLAMP
        )

        // Moving gradient segment
        val segmentWidth = w * 0.38f
        val left = offset * (w + segmentWidth) - segmentWidth
        val right = left + segmentWidth

        val segmentRect = RectF(left, 0f, right, h)

        // Clip gradient inside rounded track
        val trackPath = android.graphics.Path().apply {
            addRoundRect(
                trackRect,
                radius,
                radius,
                android.graphics.Path.Direction.CW
            )
        }

        canvas.save()
        canvas.clipPath(trackPath)

        // Rounded moving segment
        canvas.drawRoundRect(
            segmentRect,
            radius,
            radius,
            gradientPaint
        )

        canvas.restore()

        gradientPaint.shader = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                offset = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
}
