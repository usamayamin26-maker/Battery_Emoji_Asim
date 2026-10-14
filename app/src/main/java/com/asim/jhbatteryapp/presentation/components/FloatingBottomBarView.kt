package com.asim.jhbatteryapp.presentation.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.asim.jhbatteryapp.R
import eightbitlab.com.blurview.BlurView

/**
 * Reusable Floating Navigation Bar Component with Frosted Liquid Glass Effect.
 * Real-time background blur using BlurView with translucent glass rim highlight.
 */
class FloatingBottomBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class Tab {
        TRENDING, CUSTOMIZE, SETTING
    }

    val blurView: BlurView
    private val tabTrending: LinearLayout
    private val tabCustomize: LinearLayout
    private val tabSetting: LinearLayout

    private val ivTrending: ImageView
    private val ivCustomize: ImageView
    private val ivSetting: ImageView

    private val tvTrending: TextView
    private val tvCustomize: TextView
    private val tvSetting: TextView

    private var selectedTab: Tab = Tab.TRENDING
    private var onTabSelectedListener: ((Tab) -> Unit)? = null
    private var isBlurInitialized = false

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_floating_bottom_bar, this, true)
        blurView = root.findViewById(R.id.blurView)

        tabTrending = root.findViewById(R.id.llTabTrending)
        tabCustomize = root.findViewById(R.id.llTabCustomize)
        tabSetting = root.findViewById(R.id.llTabSetting)

        ivTrending = root.findViewById(R.id.ivTabTrending)
        ivCustomize = root.findViewById(R.id.ivTabCustomize)
        ivSetting = root.findViewById(R.id.ivTabSetting)

        tvTrending = root.findViewById(R.id.tvTabTrending)
        tvCustomize = root.findViewById(R.id.tvTabCustomize)
        tvSetting = root.findViewById(R.id.tvTabSetting)

        tabTrending.setOnClickListener {
            animateTabClick(tabTrending)
            selectTab(Tab.TRENDING)
        }
        tabCustomize.setOnClickListener {
            animateTabClick(tabCustomize)
            selectTab(Tab.CUSTOMIZE)
        }
        tabSetting.setOnClickListener {
            animateTabClick(tabSetting)
            selectTab(Tab.SETTING)
        }

        updateTabUi()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode && !isBlurInitialized) {
            post {
                findRootViewGroup()?.let { setupBlur(it) }
            }
        }
    }

    /**
     * Initializes the real-time background blur for the liquid glass bar.
     * @param rootView The root ViewGroup whose background is blurred underneath.
     * @param blurRadius The blur radius intensity (default 20f).
     */
    fun setupBlur(rootView: ViewGroup, blurRadius: Float = 20f) {
        val windowBackground = getActivity()?.window?.decorView?.background
        blurView.setupWith(rootView)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(blurRadius)
            .setBlurAutoUpdate(true)
        isBlurInitialized = true
    }

    private fun findRootViewGroup(): ViewGroup? {
        val activity = getActivity()
        return activity?.window?.decorView?.findViewById(android.R.id.content)
            ?: (parent as? ViewGroup)
    }

    private fun getActivity(): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun selectTab(tab: Tab) {
        if (selectedTab != tab) {
            selectedTab = tab
            updateTabUi()
            onTabSelectedListener?.invoke(tab)
        }
    }

    fun selectTabSilently(tab: Tab) {
        if (selectedTab != tab) {
            selectedTab = tab
            updateTabUi()
        }
    }

    fun getSelectedTab(): Tab = selectedTab

    fun setOnTabSelectedListener(listener: (Tab) -> Unit) {
        onTabSelectedListener = listener
    }

    private fun animateTabClick(view: View) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(90)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun updateTabUi() {
        val activeColor = ContextCompat.getColor(context, R.color.primary_purple)
        val boldFont = ResourcesCompat.getFont(context, R.font.outfit_bold)
        val mediumFont = ResourcesCompat.getFont(context, R.font.outfit_medium)

        // 1. Trending Tab
        val isTrending = selectedTab == Tab.TRENDING
        if (isTrending) {
            ivTrending.clearColorFilter()
            tvTrending.setTextColor(activeColor)
            tvTrending.typeface = boldFont
        } else {
            ivTrending.setColorFilter(Color.WHITE)
            tvTrending.setTextColor(Color.WHITE)
            tvTrending.typeface = mediumFont
        }

        // 2. Customize Tab
        val isCustomize = selectedTab == Tab.CUSTOMIZE
        if (isCustomize) {
            ivCustomize.setColorFilter(activeColor)
            tvCustomize.setTextColor(activeColor)
            tvCustomize.typeface = boldFont
        } else {
            ivCustomize.clearColorFilter()
            tvCustomize.setTextColor(Color.WHITE)
            tvCustomize.typeface = mediumFont
        }

        // 3. Setting Tab
        val isSetting = selectedTab == Tab.SETTING
        if (isSetting) {
            ivSetting.setColorFilter(activeColor)
            tvSetting.setTextColor(activeColor)
            tvSetting.typeface = boldFont
        } else {
            ivSetting.clearColorFilter()
            tvSetting.setTextColor(Color.WHITE)
            tvSetting.typeface = mediumFont
        }
    }
}
