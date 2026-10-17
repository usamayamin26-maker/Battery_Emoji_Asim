package com.asim.jhbatteryapp.presentation.custom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ItemCustomStatusIconBinding

data class CustomStatusIconItem(
    val id: String,
    val title: String,
    val iconRes: Int,
    var isSelected: Boolean = false
)

class CustomStatusIconAdapter(
    private val onIconClicked: (CustomStatusIconItem) -> Unit,
    private val onCheckClicked: ((CustomStatusIconItem) -> Unit)? = null
) : RecyclerView.Adapter<CustomStatusIconAdapter.IconViewHolder>() {

    private val items = mutableListOf<CustomStatusIconItem>()
    private val enabledIds = mutableSetOf<String>()

    fun setItems(newItems: List<CustomStatusIconItem>, currentEnabledIds: Set<String> = emptySet()) {
        items.clear()
        items.addAll(newItems)
        enabledIds.clear()
        enabledIds.addAll(currentEnabledIds)
        notifyDataSetChanged()
    }

    fun setEnabledIds(newIds: Set<String>) {
        enabledIds.clear()
        enabledIds.addAll(newIds)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemCustomStatusIconBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, enabledIds.contains(item.id))
    }

    override fun getItemCount(): Int = items.size

    inner class IconViewHolder(
        private val binding: ItemCustomStatusIconBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomStatusIconItem, isSelected: Boolean) {
            val bgRes = if (isSelected) {
                R.drawable.bg_custom_icon_item_selected
            } else {
                R.drawable.bg_custom_icon_item
            }
            binding.clIconCard.setBackgroundResource(bgRes)
            binding.ivCustomStatusIcon.setImageResource(item.iconRes)
            binding.tvCustomStatusIconTitle.text = item.title
            binding.ivIconCardCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

            val tintColor = if (isSelected) {
                ContextCompat.getColor(binding.root.context, R.color.primary_purple)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.text_primary)
            }
            binding.ivCustomStatusIcon.setColorFilter(tintColor)

            binding.ivIconCardCheck.setOnClickListener {
                if (onCheckClicked != null) {
                    onCheckClicked.invoke(item)
                } else {
                    onIconClicked(item)
                }
            }

            binding.clIconCard.setOnClickListener {
                onIconClicked(item)
            }

            binding.root.setOnClickListener {
                onIconClicked(item)
            }
        }
    }
}
