package com.asim.jhbatteryapp.presentation.custom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ItemCustomBatteryStyleBinding

data class BatteryStyleItem(
    val id: String,
    val drawableRes: Int,
    val isSelected: Boolean = false,
    val isTintable: Boolean = true
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
    }

    override fun getItemCount(): Int = items.size

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
            binding.ivBatteryStyleIcon.setImageResource(item.drawableRes)
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
