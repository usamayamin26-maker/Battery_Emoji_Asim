package com.universal.remote.ads

import android.app.Activity
import androidx.annotation.VisibleForTesting
import java.lang.ref.WeakReference

/**
 * "A full-screen ad is on screen (or loading behind a dialog)", so the resume app open ad never
 * stacks on top of another one, and the locker does not re-lock while an ad covers the app. Every
 * full-screen format calls [begin] / [end]; if the owning activity is destroyed without [end],
 * [onActivityDestroyed] clears it so it can't get stuck.
 *
 * How often full-screen ads appear is decided only by the click counters in [InterstitialClickGates]
 * (no minimum gap, no first-session cap).
 */
object FullScreenAdState {

    @Volatile
    var isShowing = false
        private set

    private var owner: WeakReference<Activity>? = null

    fun begin(activity: Activity) {
        isShowing = true
        owner = WeakReference(activity)
    }

    fun end() {
        isShowing = false
        owner = null
    }

    fun onActivityDestroyed(activity: Activity) {
        if (owner?.get() === activity) end()
    }

    @VisibleForTesting
    fun reset() {
        end()
    }
}
