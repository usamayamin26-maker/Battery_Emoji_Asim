package com.jhw.batteryemoji.presentation.custom

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.databinding.ItemColorSwatchBinding

data class ColorSwatchItem(
    val id: String,
    val colorInt: Int,
    val isEyedropper: Boolean = false,
    val isSelected: Boolean = false
)

class ColorSwatchAdapter(
    private val onColorSelected: (ColorSwatchItem) -> Unit
) : RecyclerView.Adapter<ColorSwatchAdapter.SwatchViewHolder>() {

    private val items = mutableListOf<ColorSwatchItem>()
    private var selectedId: String = "purple"

    fun setItems(newItems: List<ColorSwatchItem>, currentSelectedId: String) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwatchViewHolder {
        val binding = ItemColorSwatchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SwatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SwatchViewHolder, position: Int) {
        holder.bind(items[position], items[position].id == selectedId)
    }

    override fun getItemCount(): Int = items.size

    inner class SwatchViewHolder(
        private val binding: ItemColorSwatchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ColorSwatchItem, isSelected: Boolean) {
            val context = binding.root.context

            if (item.isEyedropper) {
                // Eyedropper icon
                binding.vColorCircle.background = ContextCompat.getDrawable(context, R.drawable.bg_custom_icon_item)
                binding.ivSwatchIcon.visibility = View.VISIBLE
                binding.ivSwatchIcon.setImageResource(R.drawable.ic_color_eyedropper)
                binding.ivSwatchIcon.imageTintList = null
                binding.vOuterRing.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            } else {
                // Solid color circle
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(item.colorInt)
                    if (item.colorInt == Color.WHITE) {
                        setStroke(2, Color.parseColor("#E4D5EC"))
                    }
                }
                binding.vColorCircle.background = shape

                if (isSelected) {
                    binding.vOuterRing.visibility = View.VISIBLE
                    binding.ivSwatchIcon.visibility = View.VISIBLE
                    binding.ivSwatchIcon.setImageResource(R.drawable.ic_check)
                    val checkTint = if (item.colorInt == Color.WHITE) Color.BLACK else Color.WHITE
                    binding.ivSwatchIcon.setColorFilter(checkTint)
                } else {
                    binding.vOuterRing.visibility = View.INVISIBLE
                    binding.ivSwatchIcon.visibility = View.GONE
                }
            }

            binding.root.setOnClickListener {
                setSelectedId(item.id)
                onColorSelected(item)
            }
        }
    }
}
