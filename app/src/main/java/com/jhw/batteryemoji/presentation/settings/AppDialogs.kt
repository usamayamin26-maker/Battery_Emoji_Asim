package com.jhw.batteryemoji.presentation.settings

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    /** Floating dialog in the app's dialog theme, 90% of the screen width (max 420dp), like the share dialog. */
    private fun createDialog(context: Context, root: android.view.View): Dialog =
        Dialog(context, R.style.Theme_JHBatteryApp_Dialog).apply {
            setContentView(root)
            setCancelable(true)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val metrics = context.resources.displayMetrics
                setLayout(
                    (metrics.widthPixels * 0.90).toInt().coerceAtMost((420 * metrics.density).toInt()),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

    /** Opens the app's Play Store page, falling back to the browser when the Play Store app is missing. */
    private fun openPlayStore(context: Context) {
        val packageName = context.packageName
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * 5-star rating dialog. 3-5 stars plays the rate animation over the dialog, then opens the
     * Play Store page; 1-2 stars calls [onLowRating]
     * (e.g. to ask for feedback instead).
     */
    fun showRateUsDialog(context: Context, onLowRating: (() -> Unit)? = null) {
        val binding = DialogRateUsBinding.inflate(LayoutInflater.from(context))
        val dialog = createDialog(context, binding.root)

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

        updateStars(currentRating)

        // Done Action
        binding.btnRatingDone.setOnClickListener {
            if (currentRating >= 3) {
                // Fade the dialog, play the rate animation over it once, then open the Play Store
                binding.btnRatingDone.isEnabled = false
                stars.forEach { it.isEnabled = false }
                dialog.setCancelable(false)
                binding.cardRateContent.animate().alpha(0.45f).setDuration(200).start()
                binding.lottieRateAnim.visibility = View.VISIBLE
                binding.lottieRateAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!dialog.isShowing) return
                        dialog.dismiss()
                        openPlayStore(context)
                    }
                })
                binding.lottieRateAnim.playAnimation()
                return@setOnClickListener
            }
            dialog.dismiss()
            if (onLowRating != null) {
                onLowRating()
            } else {
                Toast.makeText(context, context.getString(R.string.feedback_submitted), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    /**
     * Show Emoji Reaction Feedback Dialog matching Image 2 (with floating top emoji)
     */
    fun showEmojiFeedbackDialog(context: Context) {
        val binding = DialogEmojiFeedbackBinding.inflate(LayoutInflater.from(context))
        val dialog = createDialog(context, binding.root)

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
