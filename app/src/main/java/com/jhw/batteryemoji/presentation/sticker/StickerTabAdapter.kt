package com.jhw.batteryemoji.presentation.sticker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.databinding.ItemStickerTabBinding

class StickerTabAdapter(
    private val onTabClick: (String) -> Unit
) : RecyclerView.Adapter<StickerTabAdapter.StickerTabViewHolder>() {

    private val tabs = mutableListOf<Pair<String, String>>()
    private var selectedKey: String = "hot"

    fun setTabs(newTabs: List<Pair<String, String>>, currentKey: String) {
        tabs.clear()
        tabs.addAll(newTabs)
        selectedKey = currentKey
        notifyDataSetChanged()
    }

    fun setSelectedTab(key: String) {
        selectedKey = key
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerTabViewHolder {
        val binding = ItemStickerTabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StickerTabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StickerTabViewHolder, position: Int) {
        val (key, title) = tabs[position]
        holder.bind(key, title, key == selectedKey)
    }

    override fun getItemCount(): Int = tabs.size

    inner class StickerTabViewHolder(
        private val binding: ItemStickerTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(key: String, title: String, isSelected: Boolean) {
            binding.tvStickerTabTitle.text = title
            val context = binding.root.context

            if (isSelected) {
                binding.tvStickerTabTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.primary_purple)
                )
                binding.tvStickerTabTitle.alpha = 1.0f
            } else {
                binding.tvStickerTabTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.tab_inactive_text)
                )
                binding.tvStickerTabTitle.alpha = 0.7f
            }

            binding.tvStickerTabTitle.setOnClickListener {
                if (selectedKey != key) {
                    selectedKey = key
                    notifyDataSetChanged()
                    onTabClick(key)
                }
            }
        }
    }
}
