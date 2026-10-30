package com.jhw.batteryemoji.presentation.splash

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
import com.jhw.batteryemoji.ads.AppAds
import com.jhw.batteryemoji.ads.RemoteConfigManager
import com.jhw.batteryemoji.core.Utils.MyUtils
import com.jhw.batteryemoji.data.datasource.PreferenceLocalDataSource
import com.jhw.batteryemoji.data.remote.RemoteAssetPreloader
import com.jhw.batteryemoji.databinding.ActivitySplashBinding
import com.jhw.batteryemoji.presentation.home.MainActivity
import com.jhw.batteryemoji.presentation.onboarding.OnboardingActivity
import com.jhw.batteryemoji.presentation.settings.LanguageActivity
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
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        binding.gradientProgressIndicator.visibility = View.VISIBLE
        binding.btnContinue.visibility = View.GONE

        if (!MyUtils.isNetworkAvailable(this)) {
            MyUtils.showNoInternetDialog(
                context = this,
                onRetry = {
                    RemoteAssetPreloader.start()
                    startSplashFlow()
                },
                onCancel = {
                    startSplashFlow()
                }
            )
        } else {
            RemoteAssetPreloader.start()
            startSplashFlow()
        }
    }

    private fun startSplashFlow() {
        if (!AppAds.ADS_ENABLED) {
            lifecycleScope.launch {
                delay(SPLASH_DURATION_MS)
                routeOnwards()
            }
            return
        }

        lifecycleScope.launch {
            Log.i(TAG, "Starting splash flow")

            // 1. Consent + bootstrap
            awaitConsentSettled()
            Log.i(TAG, "Consent settled")

            // 2. Bind splash banner/native slots
            bindAdSlots()

            // 3. Load Splash full-screen ad (interstitial or app open, whichever is ON in remote config)
            val adLoadJob = launch { loadSplashAdIfAny() }
            val minDelayJob = launch { delay(SPLASH_DURATION_MS) }

            adLoadJob.join()
            minDelayJob.join()

            if (isFinishing) return@launch

            // 4. Show full-screen ad if loaded, then navigate; otherwise navigate directly
            val ad = loadedSplashAd
            if (ad != null) {
                Log.i(TAG, "Showing splash ad")
                loadedSplashAd = null
                SplashAds.show(this@SplashActivity, ad, keepBackdrop = false) {
                    binding.gradientProgressIndicator.visibility = View.GONE
                    routeOnwards()
                }
            } else {
                Log.i(TAG, "No splash ad loaded — navigating onwards")
                binding.gradientProgressIndicator.visibility = View.GONE
                routeOnwards()
            }
        }
    }

    private fun bindAdSlots() {
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = "splash")
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = "splash")
    }

    private fun routeOnwards() {
        Log.d(TAG, "routeOnwards")
        if (isFinishing) return

        val config = RemoteConfigManager.current()
        val removeGetStarted = config.int("remove_get_started", 0)

        // If remove_get_started == 0 (Get Started is ON in remote config), ALWAYS show GetStartedActivity
        if (removeGetStarted == 0) {
            Log.d(TAG, "GetStarted is ON (remove_get_started=0) -> GetStartedActivity")
            startActivity(Intent(this, GetStartedActivity::class.java))
            finish()
            return
        }

        // If remove_get_started == 1, skip GetStarted and follow normal onboarding flow
        val prefs = PreferenceLocalDataSource(this)
        val target = when {
            !prefs.isLanguageSelected() -> LanguageActivity::class.java
            !prefs.isOnboardingCompleted() -> OnboardingActivity::class.java
            else -> MainActivity::class.java
        }
        val intent = Intent(this, target)
        if (target == LanguageActivity::class.java) {
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

    /** Loads whichever full-screen format is configured in Remote Config (Interstitial or App Open). */
    private suspend fun loadSplashAdIfAny() {
        val worthWaiting = SplashAds.isWorthWaitingFor(appOpenOnly = false)
        if (!worthWaiting) {
            Log.i(TAG, "No splash full-screen ad configured — skipping load")
            return
        }

        val ready = withTimeoutOrNull(READY_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                AdsController.whenReady(this@SplashActivity) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
        if (ready == null || isFinishing) return

        loadedSplashAd = suspendCancellableCoroutine { cont ->
            cancelSplashAdLoad = SplashAds.load(this@SplashActivity, appOpenOnly = false) { loaded ->
                cancelSplashAdLoad = null
                Log.i(TAG, "Splash ad load result: ${if (loaded == null) "NULL" else "SUCCESS"}")
                if (cont.isActive) cont.resume(loaded)
            }
        }
    }

    private suspend fun awaitConsentSettled() {
        val settled = withTimeoutOrNull(CONSENT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                AdsController.start(this@SplashActivity) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
        if (settled == null) Log.e(TAG, "Consent timeout")
    }

    private companion object {
        const val TAG = "SplashActivity"
        const val SPLASH_DURATION_MS = 2500L
        const val CONSENT_TIMEOUT_MS = 10000L
        const val READY_TIMEOUT_MS = 5000L
        const val SCREEN_KEY = "splash"
    }
}
