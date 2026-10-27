package com.jhw.batteryemoji.presentation.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.ItemBatteryEmojiBinding
import com.jhw.batteryemoji.domain.model.BatteryEmojiItem
import com.jhw.batteryemoji.domain.model.RemoteAssetItem

class BatteryEmojiAdapter(
    private val onRemoteItemClick: ((RemoteAssetItem) -> Unit)? = null,
    private val onItemClick: (BatteryEmojiItem) -> Unit
) : RecyclerView.Adapter<BatteryEmojiAdapter.BatteryEmojiViewHolder>() {

    // ── Local items (existing — unchanged) ─────────────────────────────────────
    private val items = ArrayList<BatteryEmojiItem>()

    fun submitList(newItems: List<BatteryEmojiItem>) {
        if (items == newItems && items.isNotEmpty()) {
            return
        }
        items.clear()
        items.addAll(newItems)
        remoteItems.clear()   // Clear remote when local is refreshed
        notifyDataSetChanged()
    }

    fun getItem(position: Int): BatteryEmojiItem = items[position]

    // ── Remote items (new — additive only) ─────────────────────────────────────

    private val remoteItems = ArrayList<RemoteAssetItem>()

    /**
     * Submits remote assets to append after local items.
     * Does NOT affect existing [submitList] or [onItemClick] behavior.
     */
    fun submitRemoteList(newItems: List<RemoteAssetItem>) {
        remoteItems.clear()
        remoteItems.addAll(newItems)
        notifyDataSetChanged()
    }

    fun clearRemoteList() {
        if (remoteItems.isNotEmpty()) {
            remoteItems.clear()
            notifyDataSetChanged()
        }
    }

    // ── Adapter overrides ──────────────────────────────────────────────────────

    override fun getItemCount(): Int = items.size + remoteItems.size

    override fun getItemViewType(position: Int): Int =
        if (position < items.size) VIEW_TYPE_LOCAL else VIEW_TYPE_REMOTE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatteryEmojiViewHolder {
        val binding = ItemBatteryEmojiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BatteryEmojiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BatteryEmojiViewHolder, position: Int) {
        if (position < items.size) {
            holder.bindLocal(items[position])
        } else {
            val remoteIndex = position - items.size
            holder.bindRemote(remoteItems[remoteIndex])
            RemoteImageLoader.prefetchAfter(remoteItems, remoteIndex)
        }
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    inner class BatteryEmojiViewHolder(
        private val binding: ItemBatteryEmojiBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Original local binding — completely unchanged. */
        fun bindLocal(item: BatteryEmojiItem) {
            // Invalidate any in-flight remote load targeting this recycled view
            binding.cardEmojiBattery.getEmojiImageView().tag = null
            binding.cardEmojiBattery.setCardColorRes(item.backgroundColorRes)
            binding.cardEmojiBattery.setEmojiMascot(item.previewDrawableRes)
            binding.cardEmojiBattery.setOnCardClickListener {
                onItemClick(item)
            }
        }

        /** New remote binding — loads image from URL via RemoteImageLoader. */
        fun bindRemote(item: RemoteAssetItem) {
            // Parse hex color safely, fall back to a neutral purple
            val bgColor = try {
                Color.parseColor(item.backgroundColor)
            } catch (_: Throwable) {
                Color.parseColor("#FFECE8F5")
            }
            binding.cardEmojiBattery.setCardColor(bgColor)
            // Load from URL into the same ImageView slot
            RemoteImageLoader.loadImage(
                asset = item,
                view = binding.cardEmojiBattery.getEmojiImageView()
            )
            binding.cardEmojiBattery.setOnCardClickListener {
                onRemoteItemClick?.invoke(item)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_LOCAL = 0
        private const val VIEW_TYPE_REMOTE = 1
    }
}
