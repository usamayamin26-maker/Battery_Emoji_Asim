package com.jhw.batteryemoji.presentation.custom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.ItemCustomBatteryStyleBinding
import com.jhw.batteryemoji.domain.model.RemoteAssetItem

data class BatteryStyleItem(
    val id: String,
    val drawableRes: Int,
    val isSelected: Boolean = false,
    val isTintable: Boolean = false,
    /** Set for a battery icon from the remote manifest; [drawableRes] is then 0. */
    val remote: RemoteAssetItem? = null
)

class BatteryStyleAdapter(
    private val onStyleSelected: (BatteryStyleItem) -> Unit
) : RecyclerView.Adapter<BatteryStyleAdapter.StyleViewHolder>() {

    private val items = mutableListOf<BatteryStyleItem>()
    private var selectedId: String = "bolt"

    fun setItems(newItems: List<BatteryStyleItem>, currentSelectedId: String) {
        items.clear()
        items.addAll(newItems)
        selectedId = currentSelectedId
        notifyDataSetChanged()
    }

    fun setSelectedId(newId: String) {
        val oldPos = items.indexOfFirst { it.id == selectedId }
        val newPos = items.indexOfFirst { it.id == newId }
        selectedId = newId
        if (oldPos != -1) notifyItemChanged(oldPos)
        if (newPos != -1) notifyItemChanged(newPos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        val binding = ItemCustomBatteryStyleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StyleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        holder.bind(items[position], items[position].id == selectedId)
        if (items[position].remote != null) {
            val next = items.drop(position + 1).take(PREFETCH_AHEAD).mapNotNull { it.remote }
            RemoteImageLoader.prefetch(next)
        }
    }

    override fun getItemCount(): Int = items.size

    private companion object {
        const val PREFETCH_AHEAD = 6
    }

    inner class StyleViewHolder(
        private val binding: ItemCustomBatteryStyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BatteryStyleItem, isSelected: Boolean) {
            val bgRes = if (isSelected) {
                R.drawable.bg_custom_icon_item_selected
            } else {
                R.drawable.bg_custom_icon_item
            }
            binding.clBatteryStyleCard.setBackgroundResource(bgRes)
            if (item.remote != null) {
                binding.ivBatteryStyleIcon.setImageDrawable(null)
                RemoteImageLoader.loadImage(item.remote, binding.ivBatteryStyleIcon)
            } else {
                // Clear the tag so a remote load still in flight doesn't overwrite this icon
                binding.ivBatteryStyleIcon.tag = null
                binding.ivBatteryStyleIcon.setImageResource(item.drawableRes)
            }
            if (item.isTintable) {
                binding.ivBatteryStyleIcon.imageTintList = ContextCompat.getColorStateList(binding.root.context, R.color.primary_blue)
            } else {
                binding.ivBatteryStyleIcon.imageTintList = null
            }
            binding.ivBatteryStyleCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

            binding.clBatteryStyleCard.setOnClickListener {
                setSelectedId(item.id)
                onStyleSelected(item)
            }
        }
    }
}
