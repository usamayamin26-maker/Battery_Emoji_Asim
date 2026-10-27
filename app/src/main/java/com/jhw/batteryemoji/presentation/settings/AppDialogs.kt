package com.jhw.batteryemoji.presentation.settings

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.databinding.DialogEmojiFeedbackBinding
import com.jhw.batteryemoji.databinding.DialogRateUsBinding
import com.jhw.batteryemoji.databinding.DialogShareAppBinding

object AppDialogs {

    /**
     * Show Share Emoji App Dialog matching the screenshot
     */
    fun showShareDialog(context: Context) {
        com.jhw.batteryemoji.core.Utils.MyUtils.showShareDialog(context)
    }

    /**
     * Show 5-Star Rating Feedback Dialog matching Image 2
     */
    fun showRateUsDialog(context: Context, onFeedbackSubmitted: (() -> Unit)? = null) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogRateUsBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        var currentRating = 3
        val stars = listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)

        fun updateStars(rating: Int) {
            currentRating = rating
            for (i in stars.indices) {
                if (i < rating) {
                    stars[i].setImageResource(R.drawable.ic_star_filled)
                } else {
                    stars[i].setImageResource(R.drawable.ic_star_outline)
                }
            }
        }

        stars.forEachIndexed { index, starView ->
            starView.setOnClickListener {
                updateStars(index + 1)
            }
        }

        // Done Action
        binding.btnRatingDone.setOnClickListener {
            Toast.makeText(context, context.getString(R.string.feedback_submitted), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onFeedbackSubmitted?.invoke()
        }

        dialog.show()
    }

    /**
     * Show Emoji Reaction Feedback Dialog matching Image 2 (with floating top emoji)
     */
    fun showEmojiFeedbackDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogEmojiFeedbackBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        var selectedEmojiRes = R.drawable.ic_reaction_neutral

        fun selectEmoji(emojiRes: Int, targetView: ImageView) {
            selectedEmojiRes = emojiRes
            binding.ivFloatingTopEmoji.setImageResource(emojiRes)

            // Reset elevation
            binding.cvEmojiLove.cardElevation = 0f

            // Animate top floating badge
            binding.ivFloatingTopEmoji.scaleX = 0.8f
            binding.ivFloatingTopEmoji.scaleY = 0.8f
            binding.ivFloatingTopEmoji.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120).withEndAction {
                binding.ivFloatingTopEmoji.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
            }.start()
        }

        binding.ivEmojiAngry.setOnClickListener { selectEmoji(R.drawable.ic_reaction_angry, binding.ivEmojiAngry) }
        binding.ivEmojiSad.setOnClickListener { selectEmoji(R.drawable.ic_reaction_sad, binding.ivEmojiSad) }
        binding.ivEmojiNeutral.setOnClickListener { selectEmoji(R.drawable.ic_reaction_neutral, binding.ivEmojiNeutral) }
        binding.ivEmojiHappy.setOnClickListener { selectEmoji(R.drawable.ic_reaction_happy, binding.ivEmojiHappy) }
        binding.ivEmojiLove.setOnClickListener {
            binding.cvEmojiLove.cardElevation = 4f * context.resources.displayMetrics.density
            selectEmoji(R.drawable.ic_reaction_love, binding.ivEmojiLove)
        }

        // Done Action
        binding.btnFeedbackDone.setOnClickListener {
            Toast.makeText(context, context.getString(R.string.feedback_submitted), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}
