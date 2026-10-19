package com.asim.jhbatteryapp.presentation.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val items: List<LanguageItem>,
    private val onLanguageSelected: (LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedIndex = items.indexOfFirst { it.isSelected }.let { if (it >= 0) it else 0 }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(items[position], position == selectedIndex)
    }

    override fun getItemCount(): Int = items.size

    fun getSelectedLanguage(): LanguageItem = items[selectedIndex]

    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LanguageItem, isSelected: Boolean) {
            binding.tvLanguageName.text = item.name
            binding.tvLanguageNative.text = item.nativeName

            binding.ivLanguageFlag.setImageResource(item.flagRes)

            if (isSelected) {
                binding.llLanguageItem.setBackgroundResource(R.drawable.bg_language_card_selected)
            } else {
                binding.llLanguageItem.setBackgroundResource(R.drawable.bg_language_card)
            }

            binding.root.setOnClickListener {
                val previousIndex = selectedIndex
                val currentPos = bindingAdapterPosition
                if (currentPos != androidx.recyclerview.widget.RecyclerView.NO_POSITION && currentPos != selectedIndex) {
                    selectedIndex = currentPos
                    if (previousIndex in items.indices) {
                        items[previousIndex].isSelected = false
                        notifyItemChanged(previousIndex)
                    }
                    if (selectedIndex in items.indices) {
                        items[selectedIndex].isSelected = true
                        notifyItemChanged(selectedIndex)
                        onLanguageSelected(items[selectedIndex])
                    }
                }
            }
        }
    }
}
