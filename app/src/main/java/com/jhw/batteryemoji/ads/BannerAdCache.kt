package com.universal.remote.ads

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdView
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * Caches a loaded banner [AdView] per screen key, so a screen bound again re-attaches the same ad
 * instead of firing a new ad request.
 *
 * **An entry belongs to one Activity instance and dies with it.** An [AdView] holds the Activity it
 * was constructed with — that is how it routes a click and how it expands — so an entry is handed
 * back only to that same Activity, and [AdView.destroy] runs as soon as that Activity is destroyed.
 *
 * Without that, a process-wide map of AdViews that is never destroyed strands one Activity per
 * cached banner, and this app recreates every Activity whenever the theme changes
 * (`AppCompatDelegate.setDefaultNightMode`, from the Settings tab). Handing a *new* Activity an
 * AdView built by a dead one is not a saved request either: the ad would still try to route its
 * click and its expansion through the Activity it is holding, which no longer exists.
 *
 * **An entry also expires.** Past [MAX_CACHED_AD_AGE_MS] it is destroyed and the caller requests a
 * fresh ad, rather than being handed a creative that loaded an hour or more ago.
 *
 * A banner's own auto-refresh does not cover this. Google's banner guide is explicit that "the
 * automatic refresh occurs only if the banner is visible on screen" — and a cached AdView is, by
 * definition, one whose screen the user has left. So its creative ages exactly as its cache entry
 * does, and the age check is the only thing keeping a stale one off screen.
 *
 * What is kept, then, is the case that actually happens: the five bottom-navigation tabs live in one
 * MainActivity that is never replaced while the user switches between them, so a tab bound again
 * inside the expiry window is a cache hit. What is given up is re-attaching across a *recreated*
 * Activity — which was the leak.
 *
 * Main thread only, and no synchronisation, because ad callbacks and lifecycle callbacks all arrive
 * there.
 */
object BannerAdCache {

    private const val TAG = "BannerAdCache"

    private class Entry(val adView: AdView, owner: Activity, val mode: Int, val loadedAtMs: Long) {
        /** Weak, so a missed [release] degrades to a stale entry rather than to a leak. */
        val owner = WeakReference(owner)
    }

    private val cache = mutableMapOf<String, Entry>()

    /** Activities already being watched. Weak keys, so this map never holds one itself. */
    private val watched = WeakHashMap<Activity, Unit>()

    /**
     * The cached banner for [screenKey], but only if [activity] is the one that created it and the ad
     * has not expired, and it was loaded for the same banner [mode] (a 320x50 is no use to a slot the
     * console has since switched to 300x250). Otherwise the entry is destroyed and this reports a
     * miss, so the caller requests a fresh ad.
     */
    @MainThread
    fun get(screenKey: String, activity: Activity, mode: Int): AdView? {
        val entry = cache[screenKey] ?: return null
        if (entry.owner.get() !== activity) {
            evict(screenKey, "it belongs to an activity that is gone or is not this one")
            return null
        }
        if (entry.mode != mode) {
            evict(screenKey, "it was loaded for banner mode ${entry.mode}, the slot now asks for $mode")
            return null
        }
        val ageMs = AdClock.now() - entry.loadedAtMs
        if (ageMs >= MAX_CACHED_AD_AGE_MS) {
            evict(screenKey, "it is ${ageMs}ms old, past the ${MAX_CACHED_AD_AGE_MS}ms limit")
            return null
        }
        return entry.adView
    }

    @MainThread
    fun put(screenKey: String, adView: AdView, activity: Activity, mode: Int) {
        if (cache[screenKey]?.adView !== adView) evict(screenKey, "replaced by a newer ad")
        cache[screenKey] = Entry(adView, activity, mode, AdClock.now())
        watch(activity)
    }

    /**
     * Destroys every banner [activity] owns, and every entry whose owner has already been collected.
     * Called from the observer [watch] installs; public so a host can call it directly.
     */
    @MainThread
    fun release(activity: Activity) {
        cache.filterValues { entry ->
            val owner = entry.owner.get()
            owner === activity || owner == null
        }.keys.toList().forEach { evict(it, "its activity was destroyed") }
    }

    /** Destroys everything cached. */
    @MainThread
    fun evictAll() {
        cache.keys.toList().forEach { evict(it, "the cache was cleared") }
    }

    private fun evict(screenKey: String, reason: String) {
        val entry = cache.remove(screenKey) ?: return
        // Detach before destroying: a destroyed AdView must not stay in a layout.
        (entry.adView.parent as? ViewGroup)?.removeView(entry.adView)
        entry.adView.destroy()
        Log.d(TAG, "Destroyed the cached banner for $screenKey - $reason")
    }

    /**
     * Installs a one-shot destroy observer on [activity], once per Activity. The observer holds the
     * Activity it is registered on, so the two become unreachable together.
     */
    private fun watch(activity: Activity) {
        if (activity !is LifecycleOwner) return
        if (watched.put(activity, Unit) != null) return
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                release(activity)
                watched.remove(activity)
            }
        })
    }
}
