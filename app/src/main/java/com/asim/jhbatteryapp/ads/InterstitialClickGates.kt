package com.universal.remote.ads

import androidx.annotation.VisibleForTesting
import com.asim.jhbatteryapp.ads.AdsConfig

/**
 * Click-frequency gates for the groups of buttons that share the app interstitial, one counter per
 * `counter_*` key ([AdsController.Counter] names the usual three: bottom navigation, buttons that
 * open a screen, back). A project that needs another group just uses another key — the gate has no
 * list of them.
 *
 * counter 0 -> never show an ad. counter 1 -> show on every qualifying click. counter N -> show on
 * every Nth click (e.g. N=2: 2nd, 4th, 6th...; N=3: 3rd, 6th, 9th...). A missing key reads as 1. Only
 * clicks that reach the gate (i.e. the button's own `is_show_*` flag is on) advance the counter.
 * Counters live for the process's lifetime only — they reset on the next cold start, never persisted.
 */
object InterstitialClickGates {

    private val clicks = mutableMapOf<String, Int>()

    /** Counts this click against [counterKey] and says whether it is the one that shows an ad. */
    fun shouldShow(config: AdsConfig, counterKey: String): Boolean {
        val every = config.int(counterKey, default = 1)
        if (every <= 0) return false
        val count = (clicks[counterKey] ?: 0) + 1
        clicks[counterKey] = count
        return count % every == 0
    }

    @VisibleForTesting
    fun reset() {
        clicks.clear()
    }
}
