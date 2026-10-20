package com.universal.remote.ads

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ViewAdSlotBinding
import com.facebook.shimmer.ShimmerFrameLayout

/**
 * One ad slot (top / bottom / center of a screen) that shows either a banner or a native ad —
 * [AdSlots] picks which (`AdsController.slot`). The banner part spans the slot edge to edge; the native part gets
 * `app:nativeMarginHorizontal` (the screen's native side margin) as its start/end margin.
 *
 * The whole slot is the loaders' `slotRoot`: only one of them runs per slot, so its GONE/VISIBLE
 * applies to the slot as a whole.
 */
class AdSlotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = ViewAdSlotBinding.inflate(LayoutInflater.from(context), this)

    val bannerShimmer: ShimmerFrameLayout get() = binding.shimmerBanner.root
    val bannerContainer: FrameLayout get() = binding.adContainerBanner
    val nativeShimmer: ShimmerFrameLayout get() = binding.shimmerNative.root
    val nativeContainer: FrameLayout get() = binding.adContainerNative

    init {
        context.withStyledAttributes(attrs, R.styleable.AdSlotView) {
            val margin = getDimensionPixelSize(R.styleable.AdSlotView_nativeMarginHorizontal, 0)
            (binding.adSlotNativePart.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = margin
                marginEnd = margin
            }
        }
    }

    fun showBanner() {
        binding.adSlotBannerPart.visibility = View.VISIBLE
        binding.adSlotNativePart.visibility = View.GONE
        visibility = View.VISIBLE
    }

    fun showNative() {
        binding.adSlotBannerPart.visibility = View.GONE
        binding.adSlotNativePart.visibility = View.VISIBLE
        visibility = View.VISIBLE
    }

    fun hide() {
        bannerShimmer.stopShimmer()
        nativeShimmer.stopShimmer()
        visibility = View.GONE
    }

    /** The banner taken off in [onDetachedFromWindow]; only read if this same slot comes back. */
    private var detachedBanner: View? = null

    /**
     * The slot left the window (screen destroyed, tab replaced, sheet dismissed). BannerAdCache keeps
     * the AdView for the next cache hit, so take it off here: otherwise its parent chain keeps this
     * whole view tree (and Activity) alive until then. A cache hit re-attaches it as before.
     */
    override fun onDetachedFromWindow() {
        detachedBanner = bannerContainer.getChildAt(0)?.also { bannerContainer.removeView(it) }
        super.onDetachedFromWindow()
    }

    /**
     * No screen re-attaches a slot without loading it again (every path builds new views), but if one
     * ever does, put the banner back — unless a cache hit has moved it to another slot meanwhile.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        detachedBanner?.let { adView ->
            if (adView.parent == null && bannerContainer.childCount == 0) bannerContainer.addView(adView)
        }
        detachedBanner = null
    }
}
