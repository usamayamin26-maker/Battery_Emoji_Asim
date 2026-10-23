package com.asim.jhbatteryapp.presentation.custom

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.remote.RemoteImageLoader
import com.asim.jhbatteryapp.databinding.ItemCustomStatusIconBinding
import com.asim.jhbatteryapp.domain.model.RemoteAssetItem

data class CustomStatusEmojiItem(
    val id: String,
    val title: String,
    val drawableRes: Int,
    val isNone: Boolean = false,
    /** Set for an emoji from the remote manifest (type "status_emoji"); [drawableRes] is then 0. */
    val remote: RemoteAssetItem? = null,
    /** Translated title; when set it is shown instead of [title]. */
    val titleRes: Int = 0
)

class CustomStatusEmojiAdapter(
    private val onEmojiClicked: (CustomStatusEmojiItem) -> Unit
) : RecyclerView.Adapter<CustomStatusEmojiAdapter.EmojiViewHolder>() {

    private val items = mutableListOf<CustomStatusEmojiItem>()
    private var selectedRes: Int = 0

    /** Id of the selected remote emoji; non-null means no local item (nor "None") is selected. */
    private var selectedRemoteId: String? = null

    fun setItems(
        newItems: List<CustomStatusEmojiItem>,
        currentlySelectedRes: Int = 0,
        currentlySelectedRemoteId: String? = null
    ) {
        items.clear()
        items.addAll(newItems)
        selectedRes = currentlySelectedRes
        selectedRemoteId = currentlySelectedRemoteId
        notifyDataSetChanged()
    }

    fun setSelection(newRes: Int, remoteId: String?) {
        selectedRes = newRes
        selectedRemoteId = remoteId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val binding = ItemCustomStatusIconBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EmojiViewHolder(
        private val binding: ItemCustomStatusIconBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomStatusEmojiItem) {
            val isSelected = when {
                item.remote != null -> selectedRemoteId == item.remote.id
                selectedRemoteId != null -> false
                item.isNone -> selectedRes == 0
                else -> selectedRes == item.drawableRes
            }

            val bgRes = if (isSelected) {
                R.drawable.bg_custom_icon_item_selected
            } else {
                R.drawable.bg_custom_icon_item
            }
            binding.clIconCard.setBackgroundResource(bgRes)
            binding.ivIconCardCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.tvCustomStatusIconTitle.text = if (item.titleRes != 0) binding.root.context.getString(item.titleRes) else item.title

            if (item.remote != null) {
                RemoteImageLoader.loadImage(item.remote, binding.ivCustomStatusIcon)
                binding.ivCustomStatusIcon.imageTintList = null
            } else if (item.isNone) {
                // Clear the tag so a remote load still in flight doesn't overwrite this icon
                binding.ivCustomStatusIcon.tag = null
                binding.ivCustomStatusIcon.setImageResource(R.drawable.ic_clear)
                val color = ContextCompat.getColor(
                    binding.root.context,
                    if (isSelected) R.color.primary_purple else R.color.text_secondary
                )
                binding.ivCustomStatusIcon.imageTintList = ColorStateList.valueOf(color)
            } else {
                binding.ivCustomStatusIcon.tag = null
                binding.ivCustomStatusIcon.setImageResource(item.drawableRes)
                binding.ivCustomStatusIcon.imageTintList = null
            }

            binding.clIconCard.setOnClickListener {
                selectedRemoteId = item.remote?.id
                selectedRes = if (item.isNone) 0 else item.drawableRes
                notifyDataSetChanged()
                onEmojiClicked(item)
            }
        }
    }
}
