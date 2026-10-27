package com.jhw.batteryemoji.presentation.sticker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.ItemMyStickerBinding
import com.jhw.batteryemoji.domain.model.StickerItem

class MyStickerAdapter(
    private val onItemClick: (StickerItem) -> Unit,
    private val onRemoveClick: (StickerItem) -> Unit
) : RecyclerView.Adapter<MyStickerAdapter.MyStickerViewHolder>() {

    private val items = mutableListOf<StickerItem>()
    private var activeId: String? = null

    fun submitList(newItems: List<StickerItem>, selectedId: String?) {
        items.clear()
        items.addAll(newItems)
        activeId = selectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyStickerViewHolder {
        val binding = ItemMyStickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MyStickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyStickerViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, item.id == activeId)
    }

    override fun getItemCount(): Int = items.size

    inner class MyStickerViewHolder(
        private val binding: ItemMyStickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StickerItem, isActive: Boolean) {
            RemoteImageLoader.bindSticker(binding.ivMyStickerThumb, item.drawableRes, item.imagePath)

            // Show dashed purple border and remove minus badge when active
            binding.viewDashedBorder.visibility = if (isActive) View.VISIBLE else View.GONE
            binding.btnRemoveSticker.visibility = if (isActive) View.VISIBLE else View.GONE

            binding.clMyStickerContainer.setOnClickListener {
                activeId = item.id
                notifyDataSetChanged()
                onItemClick(item)
            }

            binding.btnRemoveSticker.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }
}
