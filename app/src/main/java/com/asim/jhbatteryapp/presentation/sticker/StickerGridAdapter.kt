package com.asim.jhbatteryapp.presentation.sticker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.databinding.ItemStickerGridBinding
import com.asim.jhbatteryapp.domain.model.StickerItem

class StickerGridAdapter(
    private val onItemClick: (StickerItem) -> Unit
) : RecyclerView.Adapter<StickerGridAdapter.StickerGridViewHolder>() {

    private val items = mutableListOf<StickerItem>()

    fun submitList(newItems: List<StickerItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerGridViewHolder {
        val binding = ItemStickerGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StickerGridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StickerGridViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class StickerGridViewHolder(
        private val binding: ItemStickerGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StickerItem) {
            binding.ivStickerThumb.setImageResource(item.drawableRes)
            binding.clStickerCard.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
