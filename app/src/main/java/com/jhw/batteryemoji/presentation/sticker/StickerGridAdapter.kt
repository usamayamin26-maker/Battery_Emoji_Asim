package com.jhw.batteryemoji.presentation.sticker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.ItemStickerGridBinding
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import com.jhw.batteryemoji.domain.model.StickerItem

class StickerGridAdapter(
    private val onRemoteItemClick: ((RemoteAssetItem) -> Unit)? = null,
    private val onItemClick: (StickerItem) -> Unit
) : RecyclerView.Adapter<StickerGridAdapter.StickerGridViewHolder>() {

    // ── Local items (existing — unchanged) ─────────────────────────────────────
    private val items = mutableListOf<StickerItem>()

    fun submitList(newItems: List<StickerItem>) {
        items.clear()
        items.addAll(newItems)
        remoteItems.clear()
        notifyDataSetChanged()
    }

    // ── Remote items (new — additive only) ─────────────────────────────────────
    private val remoteItems = mutableListOf<RemoteAssetItem>()

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerGridViewHolder {
        val binding = ItemStickerGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StickerGridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StickerGridViewHolder, position: Int) {
        if (position < items.size) {
            holder.bindLocal(items[position])
        } else {
            val remoteIndex = position - items.size
            holder.bindRemote(remoteItems[remoteIndex])
            RemoteImageLoader.prefetchAfter(remoteItems, remoteIndex)
        }
    }

    override fun getItemCount(): Int = items.size + remoteItems.size

    override fun getItemViewType(position: Int): Int =
        if (position < items.size) VIEW_TYPE_LOCAL else VIEW_TYPE_REMOTE

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    inner class StickerGridViewHolder(
        private val binding: ItemStickerGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Original local binding — completely unchanged. */
        fun bindLocal(item: StickerItem) {
            // Invalidate any in-flight remote load targeting this recycled view
            binding.ivStickerThumb.tag = null
            binding.ivStickerThumb.setImageResource(item.drawableRes)
            binding.clStickerCard.setOnClickListener {
                onItemClick(item)
            }
        }

        /** New remote binding — loads image from URL via RemoteImageLoader. */
        fun bindRemote(item: RemoteAssetItem) {
            binding.ivStickerThumb.setImageDrawable(null) // Reset while loading
            RemoteImageLoader.loadImage(
                asset = item,
                view = binding.ivStickerThumb
            )
            binding.clStickerCard.setOnClickListener {
                onRemoteItemClick?.invoke(item)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_LOCAL = 0
        private const val VIEW_TYPE_REMOTE = 1
    }
}
