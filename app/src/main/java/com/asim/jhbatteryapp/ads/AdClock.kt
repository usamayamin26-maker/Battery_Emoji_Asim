package com.universal.remote.ads

import android.os.SystemClock
import androidx.annotation.VisibleForTesting

/**
 * How old a cached ad may be before it is thrown away, and the clock that measures it. One place, so
 * [BannerAdCache] and [NativeAdCache] cannot drift apart.
 *
 * **One hour, and where that comes from.** Google's interstitial guide states it directly, in the
 * paragraph about keeping a cache of preloaded ads: "Because ads expire after an hour, you should
 * clear this cache and reload with new ads every hour."
 * (https://developers.google.com/admob/android/interstitial)
 *
 * That is the closest thing to a general figure Google publishes, and it is stated about exactly this
 * situation — an ad held in a cache rather than shown straight away. The banner and native advanced
 * guides give no expiry at all; the native guide says only "After you show a native ad, destroy the
 * ad". The one other documented number is format-specific and longer: app open ads "will time out
 * after four hours. Ads rendered more than four hours after request time will no longer be valid and
 * may not earn revenue." (https://developers.google.com/admob/android/app-open) — which is why
 * `ResumeAppOpenManager.MAX_AD_AGE_MS` is four hours and stays that way. The hour here is the
 * tighter, more general figure, and it applies to the two formats these caches hold.
 */
internal const val MAX_CACHED_AD_AGE_MS = 60 * 60 * 1000L

/**
 * The clock an ad's age is measured against.
 *
 * [SystemClock.elapsedRealtime] rather than `System.currentTimeMillis`, because an age is an elapsed
 * duration: wall-clock time jumps when the network or the user corrects it, and a jump backwards
 * would make a stale ad look fresh again.
 *
 * It is a property so a test can replace it — `SystemClock` returns 0 off a device, which would make
 * every age zero and every expiry test vacuous.
 */
internal object AdClock {

    @VisibleForTesting
    var elapsedRealtimeMs: () -> Long = { SystemClock.elapsedRealtime() }

    fun now(): Long = elapsedRealtimeMs()

    @VisibleForTesting
    fun reset() {
        elapsedRealtimeMs = { SystemClock.elapsedRealtime() }
    }
}
