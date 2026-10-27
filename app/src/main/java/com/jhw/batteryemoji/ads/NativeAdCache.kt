package com.universal.remote.ads

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.google.android.gms.ads.nativead.NativeAd

/**
 * Caches a loaded [NativeAd] per screen key so a screen bound again re-renders the same ad —
 * rebinding its assets into a freshly inflated [com.google.android.gms.ads.nativead.NativeAdView] —
 * instead of firing a new ad request.
 *
 * Unlike [BannerAdCache] this cache is process-wide, and deliberately so. A [NativeAd] is data plus
 * assets, not a view: the view is inflated fresh on every bind, and [NativeAdLoader] loads the
 * ad through the application context, so a cached entry holds no Activity and cannot strand one. The
 * re-attach benefit therefore survives a recreated Activity here, which is exactly where the banner
 * cache has to give it up.
 *
 * Three things bound it, because "process-wide" must not mean "for ever":
 *
 * 1. **Age.** Past [MAX_CACHED_AD_AGE_MS] an entry is destroyed on the next [get] and the caller
 *    requests a fresh ad, rather than showing a creative that loaded hours ago.
 * 2. **Count.** At most [MAX_ENTRIES] ads stay resident; the least recently used is destroyed to make
 *    room. There are twenty screen keys, and a native ad with the large layout holds a 16:9 image,
 *    so an unbounded map means twenty ads and their bitmaps resident for the life of the process.
 * 3. **Memory pressure.** [watchMemory] releases everything when the system asks for memory back and
 *    the app is in the background.
 *
 * **Why none of the three can destroy a visible ad.** The map is in access order, so the key just read
 * or written is the most recently used — and the slot on screen is by definition the one most recently
 * bound. It is therefore never the least-recently-used victim of rule 2. Rule 1 fires inside [get], at
 * the moment the caller is about to replace that slot's view anyway. Rule 3 is restricted to
 * `TRIM_MEMORY_BACKGROUND` and worse, when nothing is on screen at all. Destroying a [NativeAd] that a
 * live `NativeAdView` is still showing would blank its assets and end its impression, so none of them
 * is allowed to.
 *
 * Main thread only, and no synchronisation, because ad callbacks all arrive there.
 */
object NativeAdCache {

    private const val TAG = "NativeAdCache"

    /**
     * One per native slot the bottom-navigation tabs carry. Those five screens live together in one
     * MainActivity and are the only ones a user can cycle between without an Activity being
     * destroyed, so they are the set worth keeping hot; an inner screen's ad displaces the tab the
     * user is furthest from.
     *
     * Eight, not five. Home and Cast each carry a mid slot as well as their bottom one, and a mid
     * slot has a screen key of its own: `home`, `home_mid`, `remote`, `devices`, `cast`,
     * `cast_mid`, `settings`. At five the two mid ads evicted two tabs on every lap of the bar and
     * both were re-requested on the way back, which is the opposite of what a cache is for. The
     * eighth is `exit`, the native in the exit sheet: it belongs to the same MainActivity and can
     * be live at the same time as all seven, so at seven it would evict whichever tab the user was
     * furthest from every time they pressed back on Home - including the Home ad itself, which is
     * the one still on screen behind the sheet. Raise this with the tabs, not with the screen
     * count - the inner screens are meant to displace each other.
     */
    @VisibleForTesting
    internal const val MAX_ENTRIES = 8

    private class Entry(val ad: NativeAd, val loadedAtMs: Long)

    /**
     * Access-ordered, so iteration runs least-recently-used first and `removeEldestEntry` evicts the
     * screen the user is furthest from rather than the oldest-loaded one.
     */
    private val cache = object : LinkedHashMap<String, Entry>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean {
            if (size <= MAX_ENTRIES) return false
            eldest.value.ad.destroy()
            Log.d(TAG, "Destroyed the cached native ad for ${eldest.key} - the cache is full")
            return true
        }
    }

    private var watchingMemory = false

    @MainThread
    fun get(screenKey: String): NativeAd? {
        val entry = cache[screenKey] ?: return null
        val ageMs = AdClock.now() - entry.loadedAtMs
        if (ageMs >= MAX_CACHED_AD_AGE_MS) {
            evict(screenKey, "it is ${ageMs}ms old, past the ${MAX_CACHED_AD_AGE_MS}ms limit")
            return null
        }
        return entry.ad
    }

    @MainThread
    fun put(screenKey: String, nativeAd: NativeAd) {
        val previous = cache.put(screenKey, Entry(nativeAd, AdClock.now()))
        if (previous != null && previous.ad !== nativeAd) {
            previous.ad.destroy()
            Log.d(TAG, "Destroyed the previous native ad for $screenKey - replaced by a newer ad")
        }
    }

    /** Destroys and forgets the ad cached for [screenKey]. */
    @MainThread
    fun evict(screenKey: String) = evict(screenKey, "evicted")

    /** Destroys everything cached. */
    @MainThread
    fun evictAll() {
        cache.keys.toList().forEach { evict(it, "the cache was cleared") }
    }

    /**
     * Releases every cached ad when the system asks for memory back and the app is in the background.
     *
     * Registered once, on the application context — a framework class, so this costs the package no
     * dependency on the app it sits in. [AdsBootstrap] calls it once the SDK is up.
     *
     * Only `TRIM_MEMORY_BACKGROUND` and worse are acted on. The foreground levels are ignored on
     * purpose: while the app is visible a cached ad may be the one on screen, and destroying it would
     * blank it. The entry cap is what bounds the foreground footprint. Android 14 narrowed which
     * levels are delivered at all, so the test is `>=` rather than a list of constants —
     * `TRIM_MEMORY_COMPLETE` satisfies it either way.
     */
    @MainThread
    fun watchMemory(context: Context) {
        if (watchingMemory) return
        watchingMemory = true
        context.applicationContext.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level < ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) return
                if (cache.isEmpty()) return
                Log.d(TAG, "onTrimMemory($level) in the background - releasing ${cache.size} cached native ad(s)")
                evictAll()
            }

            // Deliberately empty, and deprecated by the platform besides — which is the same
            // conclusion from the other direction. It can arrive while the app is visible, where
            // destroying a cached ad could blank the one on screen; onTrimMemory above covers the
            // case that is safe.
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onLowMemory() = Unit

            override fun onConfigurationChanged(newConfig: Configuration) = Unit
        })
    }

    private fun evict(screenKey: String, reason: String) {
        val removed = cache.remove(screenKey) ?: return
        removed.ad.destroy()
        Log.d(TAG, "Destroyed the cached native ad for $screenKey - $reason")
    }

    @VisibleForTesting
    internal fun size(): Int = cache.size

    /** Least-recently-used first. Test seam for the eviction order. */
    @VisibleForTesting
    internal fun keysLruFirst(): List<String> = cache.keys.toList()
}
