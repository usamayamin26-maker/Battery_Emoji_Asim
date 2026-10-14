package com.asim.jhbatteryapp.presentation.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.asim.jhbatteryapp.R

/**
 * Reusable Custom Gradient Button Component
 * Supporting custom gradient colors, text, icons, corner radius, touch animations,
 * and loading progress state.
 */
class GradientButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val container: LinearLayout
    private val textView: TextView
    private val iconView: ImageView
    private val progressBar: ProgressBar

    private var buttonText: String? = null
    private var startColor: Int = ContextCompat.getColor(context, R.color.primary_gradient_start)
    private var endColor: Int = ContextCompat.getColor(context, R.color.primary_gradient_end)
    private var cornerRadiusPx: Float = 28f * resources.displayMetrics.density
    private var isLoading: Boolean = false

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_gradient_button, this, true)
        container = root.findViewById(R.id.llButtonContainer)
        textView = root.findViewById(R.id.tvButtonText)
        iconView = root.findViewById(R.id.ivButtonIcon)
        progressBar = root.findViewById(R.id.pbButtonLoading)

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GradientButton, defStyleAttr, 0)
            try {
                buttonText = typedArray.getString(R.styleable.GradientButton_btnText)
                val textColor = typedArray.getColor(
                    R.styleable.GradientButton_btnTextColor,
                    ContextCompat.getColor(context, R.color.white)
                )
                val textSize = typedArray.getDimension(R.styleable.GradientButton_btnTextSize, -1f)
                startColor = typedArray.getColor(R.styleable.GradientButton_btnGradientStartColor, startColor)
                endColor = typedArray.getColor(R.styleable.GradientButton_btnGradientEndColor, endColor)
                cornerRadiusPx = typedArray.getDimension(R.styleable.GradientButton_btnCornerRadius, cornerRadiusPx)
                isLoading = typedArray.getBoolean(R.styleable.GradientButton_btnIsLoading, false)
                val iconRes = typedArray.getResourceId(R.styleable.GradientButton_btnIcon, 0)

                textView.text = buttonText ?: ""
                textView.setTextColor(textColor)
                if (textSize > 0) {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                }

                if (iconRes != 0) {
                    iconView.visibility = View.VISIBLE
                    iconView.setImageResource(iconRes)
                } else {
                    iconView.visibility = View.GONE
                }

                setLoading(isLoading)
            } finally {
                typedArray.recycle()
            }
        }

        applyGradientBackground()
        setupTouchFeedback()
    }

    fun setText(text: CharSequence?) {
        buttonText = text?.toString()
        textView.text = text
    }

    fun getText(): String = textView.text.toString()

    fun setIcon(@DrawableRes iconRes: Int) {
        if (iconRes != 0) {
            iconView.visibility = View.VISIBLE
            iconView.setImageResource(iconRes)
        } else {
            iconView.visibility = View.GONE
        }
    }

    fun setGradientColors(@ColorInt start: Int, @ColorInt end: Int) {
        startColor = start
        endColor = end
        applyGradientBackground()
    }

    fun setCornerRadius(radiusPx: Float) {
        cornerRadiusPx = radiusPx
        applyGradientBackground()
    }

    fun setLoading(loading: Boolean) {
        isLoading = loading
        if (loading) {
            textView.visibility = View.INVISIBLE
            iconView.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            container.isClickable = false
        } else {
            textView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            container.isClickable = true
        }
    }

    fun setOnButtonClickListener(listener: OnClickListener?) {
        container.setOnClickListener(listener)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        container.isEnabled = enabled
        alpha = if (enabled) 1.0f else 0.5f
    }

    private fun applyGradientBackground() {
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = cornerRadiusPx
        }
        container.background = gradient
    }

    private fun setupTouchFeedback() {
        container.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> animateScale(view, 0.96f)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animateScale(view, 1.0f)
            }
            false
        }
    }

    private fun animateScale(view: View, targetScale: Float) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, targetScale)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, targetScale)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 100
            start()
        }
    }
}
