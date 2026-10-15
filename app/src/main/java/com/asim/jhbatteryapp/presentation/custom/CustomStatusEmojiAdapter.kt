package com.asim.jhbatteryapp.presentation.custom

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ItemCustomStatusIconBinding

data class CustomStatusEmojiItem(
    val id: String,
    val title: String,
    val drawableRes: Int,
    val isNone: Boolean = false
)

class CustomStatusEmojiAdapter(
    private val onEmojiClicked: (CustomStatusEmojiItem) -> Unit
) : RecyclerView.Adapter<CustomStatusEmojiAdapter.EmojiViewHolder>() {

    private val items = mutableListOf<CustomStatusEmojiItem>()
    private var selectedRes: Int = 0

    fun setItems(newItems: List<CustomStatusEmojiItem>, currentlySelectedRes: Int = 0) {
        items.clear()
        items.addAll(newItems)
        selectedRes = currentlySelectedRes
        notifyDataSetChanged()
    }

    fun setSelectedRes(newRes: Int) {
        selectedRes = newRes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val binding = ItemCustomStatusIconBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EmojiViewHolder(
        private val binding: ItemCustomStatusIconBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomStatusEmojiItem) {
            val isSelected = if (item.isNone) {
                selectedRes == 0
            } else {
                selectedRes == item.drawableRes
            }

            val bgRes = if (isSelected) {
                R.drawable.bg_custom_icon_item_selected
            } else {
                R.drawable.bg_custom_icon_item
            }
            binding.clIconCard.setBackgroundResource(bgRes)
            binding.ivIconCardCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.tvCustomStatusIconTitle.text = item.title

            if (item.isNone) {
                binding.ivCustomStatusIcon.setImageResource(R.drawable.ic_clear)
                val color = ContextCompat.getColor(
                    binding.root.context,
                    if (isSelected) R.color.primary_purple else R.color.text_secondary
                )
                binding.ivCustomStatusIcon.imageTintList = ColorStateList.valueOf(color)
            } else {
                binding.ivCustomStatusIcon.setImageResource(item.drawableRes)
                binding.ivCustomStatusIcon.imageTintList = null
            }

            binding.clIconCard.setOnClickListener {
                selectedRes = if (item.isNone) 0 else item.drawableRes
                notifyDataSetChanged()
                onEmojiClicked(item)
            }
        }
    }
}
