package com.asim.jhbatteryapp.service

import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsets
import com.airbnb.lottie.LottieAnimationView

/**
 * Fades the custom status bar out where it would get in the way, and back in afterwards:
 * - landscape (videos, games; the bar's layout is built for portrait)
 * - any app that hides the system status bar (full-screen video, reels, games)
 *
 * Only the view's alpha is animated, so the renderer stays in charge of visibility. The Lottie
 * animation is paused while hidden so it doesn't keep drawing frames nobody sees.
 */
class OverlayAutoHider(
    private val root: View,
    private val lottie: LottieAnimationView?,
    /** Called when the window insets change (e.g. display cutout known), so padding can be redone. */
    private val onInsetsChanged: () -> Unit = {}
) {

    private var isLandscape = false
    private var isFullscreen = false

    /**
     * Insets only count once the status bar has been reported visible at least once, so a device
     * that never reports it for overlay windows can't leave the bar hidden for good.
     */
    private var hasSeenStatusBar = false

    private var isHidden = false

    fun attach() {
        isLandscape = root.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener { _, insets ->
                val statusBarVisible = insets.isVisible(WindowInsets.Type.statusBars())
                if (statusBarVisible) hasSeenStatusBar = true
                isFullscreen = hasSeenStatusBar && !statusBarVisible
                update(animate = true)
                onInsetsChanged()
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            root.setOnSystemUiVisibilityChangeListener { visibility ->
                isFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
                update(animate = true)
            }
        }
        update(animate = false)
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        update(animate = true)
    }

    private fun update(animate: Boolean) {
        val shouldHide = isLandscape || isFullscreen
        if (shouldHide == isHidden && animate) return
        isHidden = shouldHide

        val target = if (shouldHide) 0f else 1f
        root.animate().cancel()
        if (animate) {
            root.animate()
                .alpha(target)
                .setDuration(if (shouldHide) HIDE_MS else SHOW_MS)
                .start()
        } else {
            root.alpha = target
        }

        val anim = lottie ?: return
        if (shouldHide) {
            anim.pauseAnimation()
        } else if (anim.visibility == View.VISIBLE && anim.composition != null && !anim.isAnimating) {
            anim.resumeAnimation()
        }
    }

    /** True while the bar is faded out; the renderer must not restart animations then. */
    val hidden: Boolean get() = isHidden

    private companion object {
        const val HIDE_MS = 120L
        const val SHOW_MS = 220L
    }
}
