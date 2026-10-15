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
) : RecyclerView.Adapter<BatteryEmojiAdapter.BatteryEmojiViewHolder>() {

    private val items = ArrayList<BatteryEmojiItem>()

    fun submitList(newItems: List<BatteryEmojiItem>) {
        if (items == newItems && items.isNotEmpty()) {
            return
        }
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): BatteryEmojiItem = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatteryEmojiViewHolder {
        val binding = ItemBatteryEmojiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BatteryEmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BatteryEmojiViewHolder, position: Int) {
        holder.bind(items[position])
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
}
