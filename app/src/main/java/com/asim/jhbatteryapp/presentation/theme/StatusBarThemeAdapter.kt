package com.asim.jhbatteryapp.presentation.theme

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ItemStatusBarThemeBinding
import com.asim.jhbatteryapp.domain.model.StatusBarThemeItem

class StatusBarThemeAdapter(
    private val onItemClick: (StatusBarThemeItem) -> Unit
) : RecyclerView.Adapter<StatusBarThemeAdapter.ThemeViewHolder>() {

    private val items = mutableListOf<StatusBarThemeItem>()
    private var selectedItemId: String = "grad_pink"

    fun setItems(newItems: List<StatusBarThemeItem>, selectedId: String) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemStatusBarThemeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        holder.bind(items[position], items[position].id == selectedItemId)
    }

    override fun getItemCount(): Int = items.size

    inner class ThemeViewHolder(
        private val binding: ItemStatusBarThemeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StatusBarThemeItem, isSelected: Boolean) {
            // Outer card selection border matching the snippet
            val cardBg = if (isSelected) {
                R.drawable.bg_theme_item_card_selected
            } else {
                R.drawable.bg_theme_item_card_unselected
            }
            binding.clThemeCard.setBackgroundResource(cardBg)

            // Inner capsule with gradient or pattern and rounded radius clipping
            binding.clInnerCapsule.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, view.resources.getDimension(com.intuit.sdp.R.dimen._18sdp))
                }
            }
            binding.clInnerCapsule.clipToOutline = true
            binding.clInnerCapsule.setBackgroundResource(item.drawableRes)
            binding.tvThemeTime.text = item.timeText
            binding.tvThemeBattery.text = item.batteryPercent

            // Text & icon colors: White for gradients, Dark for backgrounds
            val contentColor = if (item.isGradient) {
                ContextCompat.getColor(binding.root.context, R.color.white)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.text_primary)
            }

            binding.tvThemeTime.setTextColor(contentColor)
            binding.tvThemeBattery.setTextColor(contentColor)
            binding.ivThemeSignal.imageTintList = ColorStateList.valueOf(contentColor)
            binding.ivThemeWifi.imageTintList = ColorStateList.valueOf(contentColor)
            binding.ivThemeBattery.imageTintList = ColorStateList.valueOf(contentColor)
            binding.ivThemeBolt.imageTintList = ColorStateList.valueOf(contentColor)

            binding.clThemeCard.setOnClickListener {
                setSelectedId(item.id)
                onItemClick(item)
            }
        }
    }
}
