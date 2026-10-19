package com.asim.jhbatteryapp.presentation.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.asim.jhbatteryapp.R
import com.google.android.material.card.MaterialCardView

/**
 * Reusable Cute Emoji Battery Card View Component
 * Supports customizable background colors, blueprint mascot drawable,
 * click animation feedback, and battery status.
 */
class EmojiBatteryCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val cardContainer: MaterialCardView
    private val contentLayout: RelativeLayout
    private val mascotImageView: ImageView
    private val percentBadge: TextView

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_emoji_battery_card, this, true)
        cardContainer = root.findViewById(R.id.cardContainer)
        contentLayout = root.findViewById(R.id.rlCardContent)
        mascotImageView = root.findViewById(R.id.ivEmojiMascot)
        percentBadge = root.findViewById(R.id.tvBatteryPercentBadge)

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.EmojiBatteryCardView, defStyleAttr, 0)
            try {
                val bgColor = typedArray.getColor(
                    R.styleable.EmojiBatteryCardView_cardBackgroundColor,
                    ContextCompat.getColor(context, R.color.card_bg_blue)
                )
                val emojiRes = typedArray.getResourceId(R.styleable.EmojiBatteryCardView_emojiImage, 0)
                val percent = typedArray.getInt(R.styleable.EmojiBatteryCardView_batteryPercentage, -1)
                val cornerRadius = typedArray.getDimension(R.styleable.EmojiBatteryCardView_cardCornerRadius, 24f * resources.displayMetrics.density)

                setCardColor(bgColor)
                cardContainer.radius = cornerRadius
                if (emojiRes != 0) {
                    setEmojiMascot(emojiRes)
                }
                if (percent >= 0) {
                    setBatteryPercentage(percent)
                }
            } finally {
                typedArray.recycle()
            }
        }

        setupTouchFeedback()
    }

    fun setCardColor(@ColorInt color: Int) {
        cardContainer.setCardBackgroundColor(color)
    }

    fun setCardColorRes(@ColorRes colorRes: Int) {
        cardContainer.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    fun setEmojiMascot(@DrawableRes drawableRes: Int) {
        com.asim.jhbatteryapp.core.Utils.ImageLoaderUtils.loadOptimizedEmoji(
            imageView = mascotImageView,
            drawableRes = drawableRes,
            targetWidthDp = 100,
            targetHeightDp = 100
        )
    }

    fun setBatteryPercentage(percent: Int) {
        if (percent in 0..100) {
            percentBadge.visibility = View.VISIBLE
            percentBadge.text = "$percent%"
        } else {
            percentBadge.visibility = View.GONE
        }
    }

    fun setOnCardClickListener(listener: OnClickListener?) {
        cardContainer.setOnClickListener(listener)
    }

    private fun setupTouchFeedback() {
        cardContainer.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> animateScale(view, 0.94f)
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
