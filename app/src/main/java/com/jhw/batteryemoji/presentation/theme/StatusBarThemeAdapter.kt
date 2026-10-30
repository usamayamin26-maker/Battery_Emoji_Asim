package com.jhw.batteryemoji.presentation.theme

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.ItemStatusBarThemeBinding
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import com.jhw.batteryemoji.domain.model.StatusBarThemeItem

class StatusBarThemeAdapter(
    private val onRemoteItemClick: ((RemoteAssetItem) -> Unit)? = null,
    private val onItemClick: (StatusBarThemeItem) -> Unit
) : RecyclerView.Adapter<StatusBarThemeAdapter.ThemeViewHolder>() {

    companion object {
        private const val VIEW_TYPE_LOCAL = 0
        private const val VIEW_TYPE_REMOTE = 1
    }

    private val items = mutableListOf<StatusBarThemeItem>()
    private val remoteItems = mutableListOf<RemoteAssetItem>()
    private var selectedItemId: String = "grad_pink"

    fun setItems(newItems: List<StatusBarThemeItem>, selectedId: String) {
        items.clear()
        items.addAll(newItems)
        selectedItemId = selectedId
        notifyDataSetChanged()
    }

    fun submitRemoteList(newRemote: List<RemoteAssetItem>) {
        remoteItems.clear()
        remoteItems.addAll(newRemote)
        notifyDataSetChanged()
    }

    fun clearRemoteList() {
        if (remoteItems.isNotEmpty()) {
            remoteItems.clear()
            notifyDataSetChanged()
        }
    }

    fun setSelectedId(newSelectedId: String) {
        val oldIndex = positionOf(selectedItemId)
        val newIndex = positionOf(newSelectedId)
        selectedItemId = newSelectedId
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    private fun positionOf(id: String): Int {
        val local = items.indexOfFirst { it.id == id }
        if (local != -1) return local
        val remote = remoteItems.indexOfFirst { it.id == id }
        return if (remote == -1) -1 else items.size + remote
    }

    override fun getItemViewType(position: Int): Int =
        if (position < items.size) VIEW_TYPE_LOCAL else VIEW_TYPE_REMOTE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemStatusBarThemeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        if (position < items.size) {
            holder.bind(items[position], items[position].id == selectedItemId)
        } else {
            val remoteIndex = position - items.size
            holder.bindRemote(remoteItems[remoteIndex], remoteItems[remoteIndex].id == selectedItemId)
            RemoteImageLoader.prefetchAfter(remoteItems, remoteIndex)
        }
    }

    override fun getItemCount(): Int = items.size + remoteItems.size

    inner class ThemeViewHolder(
        private val binding: ItemStatusBarThemeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.clInnerCapsule.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, view.resources.getDimension(com.intuit.sdp.R.dimen._18sdp))
                }
            }
            binding.clInnerCapsule.clipToOutline = true
        }

        fun bind(item: StatusBarThemeItem, isSelected: Boolean) {
            val cardBg = if (isSelected) {
                R.drawable.bg_theme_item_card_selected
            } else {
                R.drawable.bg_theme_item_card_unselected
            }
            binding.clThemeCard.setBackgroundResource(cardBg)

            binding.clInnerCapsule.background = null
            binding.ivCapsuleBg.tag = null
            binding.ivCapsuleBg.setImageResource(item.drawableRes)

            binding.tvThemeTime.text = item.timeText
            binding.tvThemeBattery.text = item.batteryPercent

            val contentColor = if (item.isGradient) {
                ContextCompat.getColor(binding.root.context, R.color.white)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.text_primary)
            }

            applyColors(contentColor)

            binding.clThemeCard.setOnClickListener {
                setSelectedId(item.id)
                onItemClick(item)
            }
        }

        fun bindRemote(item: RemoteAssetItem, isSelected: Boolean) {
            val cardBg = if (isSelected) {
                R.drawable.bg_theme_item_card_selected
            } else {
                R.drawable.bg_theme_item_card_unselected
            }
            binding.clThemeCard.setBackgroundResource(cardBg)

            binding.clInnerCapsule.background = null
            RemoteImageLoader.loadImage(item, binding.ivCapsuleBg)

            binding.tvThemeTime.text = "20:05"
            binding.tvThemeBattery.text = "58%"

            val isGradient = item.category.equals("gradient", ignoreCase = true) ||
                    item.type.equals("gradient", ignoreCase = true)
            val contentColor = if (isGradient) {
                Color.WHITE
            } else {
                ContextCompat.getColor(binding.root.context, R.color.text_primary)
            }

            applyColors(contentColor)

            binding.clThemeCard.setOnClickListener {
                setSelectedId(item.id)
                onRemoteItemClick?.invoke(item)
            }
        }

        private fun applyColors(contentColor: Int) {
            binding.tvThemeTime.setTextColor(contentColor)
            binding.tvThemeBattery.setTextColor(contentColor)
            binding.ivThemeSignal.imageTintList = ColorStateList.valueOf(contentColor)
            binding.ivThemeWifi.imageTintList = ColorStateList.valueOf(contentColor)
            binding.ivThemeBattery.imageTintList = ColorStateList.valueOf(contentColor)
            binding.ivThemeBolt.imageTintList = ColorStateList.valueOf(contentColor)
        }
    }
}
