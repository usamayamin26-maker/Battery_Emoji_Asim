package com.asim.jhbatteryapp.presentation.animation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.databinding.ItemAnimationCardBinding
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem

class StatusBarAnimationAdapter(
    private val onItemClick: (BatteryEmojiItem) -> Unit
) : RecyclerView.Adapter<StatusBarAnimationAdapter.AnimationViewHolder>() {

    private val items = mutableListOf<BatteryEmojiItem>()
    private var selectedItemId: String = "anim_girl"

    fun setItems(newItems: List<BatteryEmojiItem>, selectedId: String) {
        items.clear()
        items.addAll(newItems)
        selectedItemId = selectedId
        notifyDataSetChanged()
    }

    fun setSelectedId(newSelectedId: String) {
        val oldIndex = items.indexOfFirst { it.id == selectedItemId }
        val newIndex = items.indexOfFirst { it.id == newSelectedId }
        selectedItemId = newSelectedId
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimationViewHolder {
        val binding = ItemAnimationCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnimationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimationViewHolder, position: Int) {
        holder.bind(items[position], items[position].id == selectedItemId)
    }

    override fun getItemCount(): Int = items.size

    inner class AnimationViewHolder(
        private val binding: ItemAnimationCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BatteryEmojiItem, isSelected: Boolean) {
            val color = ContextCompat.getColor(binding.root.context, item.backgroundColorRes)
            binding.clAnimCard.backgroundTintList = ColorStateList.valueOf(color)

            if (item.isLottie && item.lottieRawRes != 0) {
                binding.ivAnimMascot.visibility = View.GONE
                binding.lottieAnimCard.visibility = View.VISIBLE
                val currentRes = binding.lottieAnimCard.tag as? Int
                if (currentRes != item.lottieRawRes) {
                    binding.lottieAnimCard.tag = item.lottieRawRes
                    binding.lottieAnimCard.setAnimation(item.lottieRawRes)
                    binding.lottieAnimCard.playAnimation()
                } else if (!binding.lottieAnimCard.isAnimating) {
                    binding.lottieAnimCard.playAnimation()
                }
            } else {
                binding.lottieAnimCard.visibility = View.GONE
                binding.lottieAnimCard.cancelAnimation()
                binding.lottieAnimCard.tag = null
                binding.ivAnimMascot.visibility = View.VISIBLE
                com.asim.jhbatteryapp.core.Utils.ImageLoaderUtils.loadOptimizedEmoji(
                    imageView = binding.ivAnimMascot,
                    drawableRes = item.previewDrawableRes,
                    targetWidthDp = 80,
                    targetHeightDp = 80
                )
            }

            binding.viewAnimSelectionBorder.visibility = if (isSelected) View.VISIBLE else View.GONE

            binding.clAnimCard.setOnClickListener {
                setSelectedId(item.id)
                onItemClick(item)
            }
        }
    }
}
