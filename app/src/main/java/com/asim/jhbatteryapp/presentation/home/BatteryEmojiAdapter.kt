package com.asim.jhbatteryapp.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.databinding.ItemBatteryEmojiBinding
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem

class BatteryEmojiAdapter(
    private val onItemClick: (BatteryEmojiItem) -> Unit
) : ListAdapter<BatteryEmojiItem, BatteryEmojiAdapter.BatteryEmojiViewHolder>(EmojiDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatteryEmojiViewHolder {
        val binding = ItemBatteryEmojiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BatteryEmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BatteryEmojiViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BatteryEmojiViewHolder(
        private val binding: ItemBatteryEmojiBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BatteryEmojiItem) {
            binding.cardEmojiBattery.setCardColorRes(item.backgroundColorRes)
            binding.cardEmojiBattery.setEmojiMascot(item.previewDrawableRes)
            binding.cardEmojiBattery.setOnCardClickListener {
                onItemClick(item)
            }
        }
    }

    object EmojiDiffCallback : DiffUtil.ItemCallback<BatteryEmojiItem>() {
        override fun areItemsTheSame(oldItem: BatteryEmojiItem, newItem: BatteryEmojiItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BatteryEmojiItem, newItem: BatteryEmojiItem): Boolean {
            return oldItem == newItem
        }
    }
}
