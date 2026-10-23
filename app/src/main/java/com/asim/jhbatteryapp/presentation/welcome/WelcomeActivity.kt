package com.asim.jhbatteryapp.presentation.welcome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.asim.jhbatteryapp.JHBatteryApplication
import com.asim.jhbatteryapp.ads.RemoteConfigManager
import com.asim.jhbatteryapp.databinding.ActivityWelcomeBinding
import com.asim.jhbatteryapp.presentation.home.MainActivity
import com.google.android.gms.ads.appopen.AppOpenAd
import com.universal.remote.ads.AdUnitIds
import com.universal.remote.ads.AdsInitializer
import com.universal.remote.ads.AppOpenAdManager

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private var hasNavigated = false
    private var cancelAdLoad: (() -> Unit)? = null
    private var loadedAppOpenAd: AppOpenAd? = null
    private var minDisplayPassed = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val minDisplayRunnable = Runnable {
        minDisplayPassed = true
        val ad = loadedAppOpenAd
        if (ad != null && !hasNavigated && !isFinishing) {
            Log.d(TAG, "Min display delay reached, showing loaded App Open Ad")
            showAppOpenAndFinish(ad)
        }
    }

    private val timeoutRunnable = Runnable {
        if (!hasNavigated && !isFinishing) {
            val ad = loadedAppOpenAd
            if (ad != null) {
                Log.d(TAG, "Timeout reached with ad ready, showing ad")
                showAppOpenAndFinish(ad)
            } else {
                Log.d(TAG, "Timeout reached without ad, navigating to main")
                completeNavigation()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If Welcome screen is disabled via Remote Config, bypass immediately
        if (!JHBatteryApplication.shouldShowWelcomeScreen()) {
            completeNavigation()
            return
        }

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, topInset, view.paddingRight, bottomInset)
            insets
        }

        ViewCompat.requestApplyInsets(binding.root)

        binding.root.setOnClickListener {
            onUserProceed()
        }

        // Back must not close the welcome screen: that skipped the resume App Open ad and dropped
        // the user straight on Home. The screen leaves on its own (ad shown, or the 3.5s timeout).
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "Back ignored while welcome screen waits for the App Open ad")
            }
        })

        // Initialize MobileAds SDK if not already done, then load App Open Ad
        AdsInitializer.initializeIfNeeded(this) {
            preloadAppOpenAd()
        }

        // Allow at least 1.2s for welcome branding animation
        mainHandler.postDelayed(minDisplayRunnable, MIN_DISPLAY_MS)
        // Hard timeout at 3.5s so user is never kept waiting
        mainHandler.postDelayed(timeoutRunnable, MAX_WAIT_MS)
    }

    private fun preloadAppOpenAd() {
        val config = RemoteConfigManager.current()
        val rawUnitId = config.resumeAppOpenId.ifBlank { config.splashAppOpenId }
        val unitId = AdUnitIds.appOpen(rawUnitId)

        if (unitId.isNotBlank()) {
            Log.d(TAG, "Requesting App Open Ad: $unitId")
            cancelAdLoad = AppOpenAdManager.load(
                activity = this,
                adUnitId = unitId,
                isEnabled = true,
                placement = "WelcomeActivity"
            ) { ad ->
                loadedAppOpenAd = ad
                Log.d(TAG, "App Open Ad loaded result: ${ad != null}, minDisplayPassed=$minDisplayPassed")
                if (ad != null && minDisplayPassed && !hasNavigated && !isFinishing) {
                    showAppOpenAndFinish(ad)
                }
            }
        } else {
            Log.w(TAG, "App Open Ad unit ID is blank")
        }
    }

    private fun onUserProceed() {
        if (hasNavigated) return
        val ad = loadedAppOpenAd
        if (ad != null) {
            showAppOpenAndFinish(ad)
        } else {
            completeNavigation()
        }
    }

    private fun showAppOpenAndFinish(ad: AppOpenAd) {
        if (hasNavigated || isFinishing) return
        hasNavigated = true
        mainHandler.removeCallbacks(minDisplayRunnable)
        mainHandler.removeCallbacks(timeoutRunnable)
        cancelAdLoad?.invoke()
        cancelAdLoad = null

        AppOpenAdManager.show(
            activity = this,
            ad = ad,
            onComplete = {
                completeNavigation()
            },
            placement = "WelcomeActivity"
        )
    }

    private fun completeNavigation() {
        if (!hasNavigated) hasNavigated = true
        mainHandler.removeCallbacks(minDisplayRunnable)
        mainHandler.removeCallbacks(timeoutRunnable)
        cancelAdLoad?.invoke()
        cancelAdLoad = null

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(minDisplayRunnable)
        mainHandler.removeCallbacks(timeoutRunnable)
        cancelAdLoad?.invoke()
        cancelAdLoad = null
        loadedAppOpenAd = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "WelcomeActivity"
        const val MIN_DISPLAY_MS = 1200L
        const val MAX_WAIT_MS = 3500L
    }
}
