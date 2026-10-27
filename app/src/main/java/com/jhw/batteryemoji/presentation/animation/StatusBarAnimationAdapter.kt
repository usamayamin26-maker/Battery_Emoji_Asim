package com.jhw.batteryemoji.presentation.animation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.ItemAnimationCardBinding
import com.jhw.batteryemoji.domain.model.BatteryEmojiItem
import com.jhw.batteryemoji.domain.model.RemoteAssetItem

class StatusBarAnimationAdapter(
    private val onRemoteItemClick: ((RemoteAssetItem) -> Unit)? = null,
    private val onItemClick: (BatteryEmojiItem) -> Unit
) : RecyclerView.Adapter<StatusBarAnimationAdapter.AnimationViewHolder>() {

    // ── Local items (existing — unchanged) ─────────────────────────────────────
    private val items = mutableListOf<BatteryEmojiItem>()
    private var selectedItemId: String = "anim_girl"

    fun setItems(newItems: List<BatteryEmojiItem>, selectedId: String) {
        items.clear()
        items.addAll(newItems)
        selectedItemId = selectedId
        notifyDataSetChanged()
    }

    fun setSelectedId(newSelectedId: String) {
        val oldIndex = positionOf(selectedItemId)
        val newIndex = positionOf(newSelectedId)
        selectedItemId = newSelectedId
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    /** Adapter position of [id] across local items followed by remote items, or -1. */
    private fun positionOf(id: String): Int {
        val local = items.indexOfFirst { it.id == id }
        if (local != -1) return local
        val remote = remoteItems.indexOfFirst { it.id == id }
        return if (remote == -1) -1 else items.size + remote
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimationViewHolder {
        val binding = ItemAnimationCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnimationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimationViewHolder, position: Int) {
        if (position < items.size) {
            holder.bind(items[position], items[position].id == selectedItemId)
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

    inner class AnimationViewHolder(
        private val binding: ItemAnimationCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /** Original local binding — completely unchanged. */
        fun bind(item: BatteryEmojiItem, isSelected: Boolean) {
            // Invalidate any in-flight remote load targeting this recycled view
            binding.ivAnimMascot.tag = null
            val color = ContextCompat.getColor(binding.root.context, item.backgroundColorRes)
            binding.clAnimCard.backgroundTintList = ColorStateList.valueOf(color)

            if (item.isLottie && item.lottieRawRes != 0) {
                binding.ivAnimMascot.visibility = View.GONE
                binding.lottieAnimCard.visibility = View.VISIBLE
                val currentRes = binding.lottieAnimCard.tag as? Int
                if (currentRes != item.lottieRawRes) {
                    binding.lottieAnimCard.tag = item.lottieRawRes
                    binding.lottieAnimCard.setAnimation(item.lottieRawRes)
                    binding.lottieAnimCard.playAnimation()
                } else if (!binding.lottieAnimCard.isAnimating) {
                    binding.lottieAnimCard.playAnimation()
                }
            } else {
                binding.lottieAnimCard.visibility = View.GONE
                binding.lottieAnimCard.cancelAnimation()
                binding.lottieAnimCard.tag = null
                binding.ivAnimMascot.visibility = View.VISIBLE
                com.jhw.batteryemoji.core.Utils.ImageLoaderUtils.loadOptimizedEmoji(
                    imageView = binding.ivAnimMascot,
                    drawableRes = item.previewDrawableRes,
                    targetWidthDp = 80,
                    targetHeightDp = 80
                )
            }

            binding.viewAnimSelectionBorder.visibility = if (isSelected) View.VISIBLE else View.GONE

            binding.clAnimCard.setOnClickListener {
                setSelectedId(item.id)
                onItemClick(item)
            }
        }

        /** New remote binding — supports both Lottie JSON URL and image URL. */
        fun bindRemote(item: RemoteAssetItem) {
            // Use a neutral background for remote items
            val bgColor = try {
                android.graphics.Color.parseColor(item.backgroundColor)
            } catch (_: Throwable) {
                android.graphics.Color.parseColor("#FFECE8F5")
            }
            binding.clAnimCard.backgroundTintList = ColorStateList.valueOf(bgColor)

            if (item.isLottie) {
                binding.ivAnimMascot.visibility = View.GONE
                binding.lottieAnimCard.visibility = View.VISIBLE
                RemoteImageLoader.loadLottie(
                    asset = item,
                    view = binding.lottieAnimCard
                )
            } else {
                binding.lottieAnimCard.visibility = View.GONE
                binding.lottieAnimCard.cancelAnimation()
                binding.ivAnimMascot.visibility = View.VISIBLE
                RemoteImageLoader.loadImage(
                    asset = item,
                    view = binding.ivAnimMascot
                )
            }

            binding.viewAnimSelectionBorder.visibility =
                if (item.id == selectedItemId) View.VISIBLE else View.GONE

            binding.clAnimCard.setOnClickListener {
                setSelectedId(item.id)
                onRemoteItemClick?.invoke(item)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_LOCAL = 0
        private const val VIEW_TYPE_REMOTE = 1
    }
}
