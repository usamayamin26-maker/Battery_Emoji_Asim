package com.asim.jhbatteryapp.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.asim.jhbatteryapp.data.datasource.PreferenceLocalDataSource
import com.asim.jhbatteryapp.data.remote.RemoteAssetPreloader
import com.asim.jhbatteryapp.databinding.ActivitySplashBinding
import com.asim.jhbatteryapp.presentation.home.MainActivity
import com.asim.jhbatteryapp.presentation.onboarding.OnboardingActivity
import com.asim.jhbatteryapp.presentation.settings.LanguageActivity
import com.universal.remote.ads.AdsController
import com.universal.remote.ads.SplashAds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var loadedSplashAd: SplashAds.Loaded? = null

    /** Drops an in-flight splash ad request so a destroyed activity is not held by it. */
    private var cancelSplashAdLoad: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fetch the remote manifest (and Home's first thumbnails) while the splash is on screen
        RemoteAssetPreloader.start()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }

        ViewCompat.requestApplyInsets(binding.root)
        // Initial UI: Show loader, hide continue button
        binding.gradientProgressIndicator.visibility = View.VISIBLE
        binding.btnContinue.visibility = View.GONE

        binding.btnContinue.setOnClickListener {
            onContinueClicked()
        }

        lifecycleScope.launch {
            Log.i(TAG, "lifecycleScope.launch: Starting splash flow")
            // 1. Wait for consent and ads bootstrap
            awaitConsentSettled()
            Log.i(TAG, "lifecycleScope.launch: Consent settled")

            // 2. Bind layout native/banner slots
            bindAdSlots()
            Log.i(TAG, "lifecycleScope.launch: Ad slots bound")

            // 3. Load the splash app open ad and wait for minimum branding delay
            Log.i(TAG, "lifecycleScope.launch: Launching ad load and min delay")
            val adLoadJob = launch { loadSplashAdIfAny() }
            val minDelayJob = launch { delay(SPLASH_DURATION_MS) }

            // Wait for both to complete
            adLoadJob.join()
            minDelayJob.join()
            Log.i(TAG, "lifecycleScope.launch: Ad load and min delay finished")

            // 4. If the app open ad loaded, show it automatically; Continue appears once it closes
            val ad = loadedSplashAd
            if (ad != null && !isFinishing) {
                Log.i(TAG, "lifecycleScope.launch: Showing splash app open ad automatically")
                loadedSplashAd = null
                SplashAds.show(this@SplashActivity, ad, keepBackdrop = false) {
                    showContinueButton()
                }
            } else if (!isFinishing) {
                Log.i(TAG, "lifecycleScope.launch: No app open ad, showing continue button")
                showContinueButton()
            }
        }
    }

    private fun showContinueButton() {
        if (isFinishing) return
        binding.gradientProgressIndicator.visibility = View.GONE
        binding.btnContinue.visibility = View.VISIBLE
    }

    private fun bindAdSlots() {
        Log.i(TAG, "bindAdSlots: Binding top slot")
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = "splash")
        Log.i(TAG, "bindAdSlots: Binding bottom slot")
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = "splash")
    }

    private fun onContinueClicked() {
        Log.i(TAG, "onContinueClicked: Button clicked")
        binding.btnContinue.isEnabled = false

        // The full-screen native is shown only from this button, never automatically
        Log.i(TAG, "onContinueClicked: Showing full-screen native via AdsController")
        AdsController.fullScreenNative(
            activity = this,
            flag = "is_show_admob_splash_native",
            unitKey = "admob_splash_native_id"
        ) {
            Log.i(TAG, "onContinueClicked: Full-screen native finished, routing onwards")
            routeOnwards()
        }
    }

    private fun routeOnwards() {
        Log.d(TAG, "routeOnwards: Navigating from splash")
        if (isFinishing) return
        
        val preferences = PreferenceLocalDataSource(this@SplashActivity)
        val targetActivity = when {
            !preferences.isLanguageSelected() -> LanguageActivity::class.java
            !preferences.isOnboardingCompleted() -> OnboardingActivity::class.java
            else -> MainActivity::class.java
        }

        val intent = Intent(this@SplashActivity, targetActivity)
        if (targetActivity == LanguageActivity::class.java) {
            intent.putExtra(LanguageActivity.EXTRA_FROM_SPLASH, true)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        cancelSplashAdLoad?.invoke()
        cancelSplashAdLoad = null
        loadedSplashAd = null
        AdsController.forgetSlots(SCREEN_KEY)
        super.onDestroy()
    }

    private suspend fun loadSplashAdIfAny() {
        Log.i(TAG, "loadSplashAdIfAny: Checking if splash ad is worth waiting for")
        val worthWaiting = SplashAds.isWorthWaitingFor(appOpenOnly = true)

        if (!worthWaiting) {
            Log.i(TAG, "loadSplashAdIfAny: Early exit - no splash ad configured in Remote Config")
            return
        }
        
        Log.i(TAG, "loadSplashAdIfAny: Waiting for AdsController to be ready")
        val ready = withTimeoutOrNull(READY_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                AdsController.whenReady(this@SplashActivity) {
                    Log.i(TAG, "loadSplashAdIfAny: AdsController is ready")
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
        
        if (ready == null) {
            Log.e(TAG, "loadSplashAdIfAny: Ads not ready within $READY_TIMEOUT_MS ms")
            return
        }

        if (isFinishing) return
        
        Log.i(TAG, "loadSplashAdIfAny: Requesting splash ad load")
        loadedSplashAd = suspendCancellableCoroutine { continuation ->
            // App open only: the splash never shows an interstitial
            cancelSplashAdLoad = SplashAds.load(this@SplashActivity, appOpenOnly = true) { loaded ->
                cancelSplashAdLoad = null
                Log.i(TAG, "loadSplashAdIfAny: Splash ad load finished. Result: ${if (loaded == null) "NULL" else "SUCCESS"}")
                if (continuation.isActive) continuation.resume(loaded)
            }
        }
    }

    private suspend fun awaitConsentSettled() {
        Log.i(TAG, "awaitConsentSettled: Starting AdsController")
        val settled = withTimeoutOrNull(CONSENT_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                AdsController.start(this@SplashActivity) {
                    Log.i(TAG, "awaitConsentSettled: AdsController started")
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
        if (settled == null) {
            Log.e(TAG, "awaitConsentSettled: Consent did not settle within $CONSENT_TIMEOUT_MS ms")
        } else {
            Log.i(TAG, "awaitConsentSettled: Consent settled")
        }
    }

    private companion object {
        const val TAG = "SplashActivity"
        const val SPLASH_DURATION_MS = 2500L
        const val CONSENT_TIMEOUT_MS = 10000L
        const val READY_TIMEOUT_MS = 5000L
        const val SCREEN_KEY = "splash"
    }
}
