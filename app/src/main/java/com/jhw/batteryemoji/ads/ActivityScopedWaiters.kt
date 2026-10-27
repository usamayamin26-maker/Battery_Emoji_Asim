package com.universal.remote.ads

import android.app.Activity
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * Callbacks waiting on something asynchronous, each owned by the Activity that registered it and
 * **dropped the moment that Activity is destroyed**.
 *
 * A plain `mutableListOf<() -> Unit>` cannot do this. The callback is a lambda that closes over the
 * Activity and its views, so holding the lambda holds the Activity — and holding it until the
 * asynchronous work finishes means holding it for as long as that work takes, or for ever if the
 * work never finishes. That is the same defect the ad caches had, in a different place: see
 * [BannerAdCache] for the caches' version of it.
 *
 * So the owner is recorded and a one-shot destroy observer drops its callbacks. The [WeakReference]
 * is a second line of defence, not the mechanism: if the observer is somehow missed, an entry
 * degrades to one that [drain] refuses to invoke rather than to a retained Activity.
 *
 * Main thread only. Every caller is an ad or lifecycle callback, which all arrive there.
 */
internal class ActivityScopedWaiters<T : Any> {

    private class Entry<T>(owner: Activity, val block: T) {
        val owner = WeakReference(owner)
    }

    private val entries = mutableListOf<Entry<T>>()

    /** Activities already being watched. Weak keys, so this map never holds one itself. */
    private val watched = WeakHashMap<Activity, Unit>()

    /** How many callbacks are held right now. Zero means nothing here is keeping an Activity alive. */
    @get:VisibleForTesting
    val size: Int get() = entries.size

    /**
     * Holds [block] until [drain]. Returns false — holding nothing at all — when [activity] is
     * already destroyed, so a caller that gets false should simply do nothing.
     */
    @MainThread
    fun add(activity: Activity, block: T): Boolean {
        if (isGone(activity)) return false
        entries += Entry(activity, block)
        watch(activity)
        return true
    }

    /**
     * Empties the list and returns, in registration order, only the callbacks whose Activity is
     * still alive. Everything else is dropped without being invoked.
     */
    @MainThread
    fun drain(): List<T> {
        val alive = entries
            .filter { entry -> entry.owner.get()?.let { !isGone(it) } == true }
            .map { it.block }
        entries.clear()
        return alive
    }

    /** Drops everything [activity] registered, and anything whose owner has been collected. */
    @MainThread
    fun remove(activity: Activity) {
        entries.removeAll { entry ->
            val owner = entry.owner.get()
            owner === activity || owner == null
        }
    }

    private fun isGone(activity: Activity): Boolean {
        if (activity.isDestroyed) return true
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle ?: return false
        return lifecycle.currentState == Lifecycle.State.DESTROYED
    }

    /**
     * One destroy observer per Activity. The observer holds the Activity whose lifecycle it is
     * registered on, so the two become unreachable together.
     *
     * An Activity that is not a [LifecycleOwner] gets no observer; its callbacks are then held until
     * the next [drain], which still refuses to invoke them. Every Activity in this app extends
     * AppCompatActivity, so that path is a fallback rather than a case that happens.
     */
    private fun watch(activity: Activity) {
        if (activity !is LifecycleOwner) return
        if (watched.put(activity, Unit) != null) return
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                remove(activity)
                watched.remove(activity)
            }
        })
    }
}
