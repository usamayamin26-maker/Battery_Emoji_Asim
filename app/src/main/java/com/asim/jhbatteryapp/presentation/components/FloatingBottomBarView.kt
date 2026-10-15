package com.asim.jhbatteryapp.presentation.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
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
    fun setupBlur(rootView: ViewGroup, blurRadius: Float = 14f) {
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

    private var gradientCustomizeDrawable: Drawable? = null
    private var gradientSettingDrawable: Drawable? = null

    private fun getGradientCustomize(): Drawable {
        if (gradientCustomizeDrawable == null) {
            gradientCustomizeDrawable = createGradientIcon(R.drawable.ic_nav_customize)
        }
        return gradientCustomizeDrawable!!
    }

    private fun getGradientSetting(): Drawable {
        if (gradientSettingDrawable == null) {
            gradientSettingDrawable = createGradientIcon(R.drawable.ic_nav_setting)
        }
        return gradientSettingDrawable!!
    }

    private fun createGradientIcon(drawableRes: Int): Drawable {
        val sizePx = try {
            resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
        } catch (_: Exception) {
            (24 * resources.displayMetrics.density).toInt()
        }.coerceAtLeast(48)

        val drawable = AppCompatResources.getDrawable(context, drawableRes)?.mutate()
            ?: return ColorDrawable(Color.TRANSPARENT)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            shader = LinearGradient(
                0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                intArrayOf(
                    Color.parseColor("#9241FD"),
                    Color.parseColor("#D424AA"),
                    Color.parseColor("#F1337B")
                ),
                floatArrayOf(0.0f, 0.5f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun updateTabUi() {
        // Consistent grey for inactive tabs
        val inactiveColor = ContextCompat.getColor(context, R.color.text_muted)

        val boldFont = ResourcesCompat.getFont(
            context,
            R.font.outfit_bold
        )

        val mediumFont = ResourcesCompat.getFont(
            context,
            R.font.outfit_medium
        )

        // Trending Tab
        val isTrending = selectedTab == Tab.TRENDING
        if (isTrending) {
            ImageViewCompat.setImageTintList(ivTrending, null)
            ivTrending.clearColorFilter()
            ivTrending.setImageResource(R.drawable.ic_nav_heart)
            applySelectedTabUi(tvTrending, boldFont)
        } else {
            ImageViewCompat.setImageTintList(ivTrending, null)
            ivTrending.setImageResource(R.drawable.ic_nav_heart)
            ivTrending.setColorFilter(inactiveColor)
            applyInactiveTabUi(tvTrending, inactiveColor, mediumFont)
        }

        // Customize Tab
        val isCustomize = selectedTab == Tab.CUSTOMIZE
        if (isCustomize) {
            ImageViewCompat.setImageTintList(ivCustomize, null)
            ivCustomize.clearColorFilter()
            ivCustomize.setImageDrawable(getGradientCustomize())
            applySelectedTabUi(tvCustomize, boldFont)
        } else {
            ImageViewCompat.setImageTintList(ivCustomize, null)
            ivCustomize.setImageResource(R.drawable.ic_nav_customize)
            ivCustomize.setColorFilter(inactiveColor)
            applyInactiveTabUi(tvCustomize, inactiveColor, mediumFont)
        }

        // Setting Tab
        val isSetting = selectedTab == Tab.SETTING
        if (isSetting) {
            ImageViewCompat.setImageTintList(ivSetting, null)
            ivSetting.clearColorFilter()
            ivSetting.setImageDrawable(getGradientSetting())
            applySelectedTabUi(tvSetting, boldFont)
        } else {
            ImageViewCompat.setImageTintList(ivSetting, null)
            ivSetting.setImageResource(R.drawable.ic_nav_setting)
            ivSetting.setColorFilter(inactiveColor)
            applyInactiveTabUi(tvSetting, inactiveColor, mediumFont)
        }
    }

    private fun applySelectedTabUi(textView: TextView, typeface: Typeface?) {
        textView.typeface = typeface
        textView.setTextColor(Color.parseColor("#7200FF"))
        val paint = textView.paint
        val text = textView.text.toString()
        val textWidth = paint.measureText(text)
        if (textWidth > 0f) {
            paint.shader = LinearGradient(
                0f, 0f, textWidth, 0f,
                intArrayOf(Color.parseColor("#7200FF"), Color.parseColor("#D424AA")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        textView.invalidate()
    }

    private fun applyInactiveTabUi(textView: TextView, inactiveColor: Int, typeface: Typeface?) {
        textView.typeface = typeface
        textView.paint.shader = null
        textView.setTextColor(inactiveColor)
        textView.invalidate()
    }
}
