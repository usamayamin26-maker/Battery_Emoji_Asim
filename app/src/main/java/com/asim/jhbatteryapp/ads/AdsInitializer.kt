package com.universal.remote.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.atomic.AtomicBoolean

/** Initializes the Google Mobile Ads SDK exactly once, after consent has been resolved. */
object AdsInitializer {

    private val initialized = AtomicBoolean(false)

    /** True once the SDK finished initializing (i.e. consent was resolved first). */
    @Volatile
    var isInitialized = false
        private set

    fun initializeIfNeeded(context: Context, onInitialized: () -> Unit) {
        if (initialized.getAndSet(true)) {
            onInitialized()
            return
        }
        MobileAds.initialize(context.applicationContext) {
            isInitialized = true
            onInitialized()
        }
    }
}
