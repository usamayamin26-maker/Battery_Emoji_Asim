package com.asim.jhbatteryapp.presentation.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.asim.jhbatteryapp.R

/**
 * Reusable Floating Navigation Bar Component
 * Displays frosted glass capsule with 3 tabs: Trending, Customize, Setting.
 */
class FloatingBottomBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class Tab {
        TRENDING, CUSTOMIZE, SETTING
    }

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

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_floating_bottom_bar, this, true)
        tabTrending = root.findViewById(R.id.llTabTrending)
        tabCustomize = root.findViewById(R.id.llTabCustomize)
        tabSetting = root.findViewById(R.id.llTabSetting)

        ivTrending = root.findViewById(R.id.ivTabTrending)
        ivCustomize = root.findViewById(R.id.ivTabCustomize)
        ivSetting = root.findViewById(R.id.ivTabSetting)

        tvTrending = root.findViewById(R.id.tvTabTrending)
        tvCustomize = root.findViewById(R.id.tvTabCustomize)
        tvSetting = root.findViewById(R.id.tvTabSetting)

        tabTrending.setOnClickListener { selectTab(Tab.TRENDING) }
        tabCustomize.setOnClickListener { selectTab(Tab.CUSTOMIZE) }
        tabSetting.setOnClickListener { selectTab(Tab.SETTING) }

        updateTabUi()
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

    fun setOnTabSelectedListener(listener: (Tab) -> Unit) {
        onTabSelectedListener = listener
    }

    private fun updateTabUi() {
        val activeColor = ContextCompat.getColor(context, R.color.nav_item_active)
        val inactiveColor = ContextCompat.getColor(context, R.color.nav_item_inactive)

        // Trending
        val isTrending = selectedTab == Tab.TRENDING
        tvTrending.setTextColor(if (isTrending) activeColor else inactiveColor)
        ivTrending.setColorFilter(if (isTrending) activeColor else inactiveColor)
        tvTrending.typeface = if (isTrending) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        // Customize
        val isCustomize = selectedTab == Tab.CUSTOMIZE
        tvCustomize.setTextColor(if (isCustomize) activeColor else inactiveColor)
        ivCustomize.setColorFilter(if (isCustomize) activeColor else inactiveColor)
        tvCustomize.typeface = if (isCustomize) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT

        // Setting
        val isSetting = selectedTab == Tab.SETTING
        tvSetting.setTextColor(if (isSetting) activeColor else inactiveColor)
        ivSetting.setColorFilter(if (isSetting) activeColor else inactiveColor)
        tvSetting.typeface = if (isSetting) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
    }
}
