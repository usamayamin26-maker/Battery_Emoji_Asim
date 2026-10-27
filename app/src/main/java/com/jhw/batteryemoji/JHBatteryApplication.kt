package com.jhw.batteryemoji

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.jhw.batteryemoji.BuildConfig
import com.jhw.batteryemoji.ads.AppAds
import com.jhw.batteryemoji.ads.RemoteConfigManager
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.di.appModule
import com.jhw.batteryemoji.presentation.splash.SplashActivity
import com.jhw.batteryemoji.presentation.welcome.WelcomeActivity
import com.universal.remote.ads.AdsController
import com.universal.remote.ads.AdsSetup
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class JHBatteryApplication : Application() {

    private var startedActivityCount = 0
    private var isChangingConfig = false
    private var hasActiveScreenInBackground = false

    /**
     * True when this return from background launched WelcomeActivity, which shows its own app open
     * ad. Our activity callbacks run before AdsController's (registered first in onCreate), so this
     * is set before ResumeAppOpenManager asks [AdsSetup.allowResumeAd] — one ad per return, not two.
     */
    private var welcomeShowsResumeAd = false

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidContext(this@JHBatteryApplication)
            modules(appModule)
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                startedActivityCount++
                val wasRecreation = isChangingConfig
                isChangingConfig = false

                // If app transitions from background to foreground (0 -> 1 active activities)
                if (startedActivityCount == 1 && !wasRecreation) {
                    onAppReturnedToForeground(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                if (startedActivityCount == 0) {
                    isChangingConfig = activity.isChangingConfigurations
                    // If an activity stopped without finishing, user backgrounded the active app
                    hasActiveScreenInBackground = !activity.isFinishing &&
                            activity !is SplashActivity &&
                            activity !is WelcomeActivity
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                // When all activities are destroyed, no active screen is left backgrounded
                if (startedActivityCount == 0) {
                    hasActiveScreenInBackground = false
                }
            }
        })

        AdsController.init(
            this,
            AdsSetup(
                remoteConfigKey = AppAds.REMOTE_CONFIG_KEY,
                defaultJson = AppAds.DEFAULT_JSON,
                isDebug = BuildConfig.DEBUG,
                allowResumeAd = { !welcomeShowsResumeAd },
            ),
        )

        // Remove the old-format caches of earlier builds (current cache: remote_asset_cache)
        RemoteImageLoader.purgeLegacyDiskCache(this)
    }

    private fun onAppReturnedToForeground(startedActivity: Activity) {
        welcomeShowsResumeAd = false

        // 1. If user navigated to settings for permission, skip welcome screen completely
        if (isPermissionNavigation) {
            isPermissionNavigation = false
            return
        }

        // 2. If SplashActivity or WelcomeActivity is starting, NEVER show WelcomeActivity over it
        if (startedActivity is SplashActivity || startedActivity is WelcomeActivity) {
            return
        }

        // 3. ONLY show WelcomeActivity if an active screen (e.g. MainActivity) was backgrounded,
        // NEVER on a fresh launch when the app was closed/exited!
        if (!hasActiveScreenInBackground) {
            return
        }

        // 4. Check unified Remote Config toggle: is_show_welcome_screen & is_show_admob_resume_app_open
        if (!shouldShowWelcomeScreen()) {
            return
        }

        // Show Welcome Screen on resume
        val intent = Intent(this, WelcomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("IS_REOPEN", true)
        }
        welcomeShowsResumeAd = true
        startActivity(intent)
    }

    companion object {
        lateinit var instance: JHBatteryApplication
            private set

        @Volatile
        var isPermissionNavigation: Boolean = false
            private set

        /**
         * Call before launching system settings / permission screens
         * so returning back to the app does NOT trigger WelcomeActivity or App Open ad.
         */
        fun skipNextWelcome() {
            isPermissionNavigation = true
            AdsController.skipNextResume()
        }

        /**
         * Unified toggle to control whether Welcome Screen & Resume App Open Ad are shown.
         * Controlled by Remote Config keys:
         * - `is_show_welcome_screen`: 1 (show) / 0 (hide)
         * - `is_show_admob_resume_app_open`: 1 (show) / 0 (hide)
         */
        fun shouldShowWelcomeScreen(): Boolean {
            val config = RemoteConfigManager.current()
            val welcomeFlag = config.int("is_show_welcome_screen", default = 1)
            val resumeAdFlag = config.isShowResumeAppOpen
            return welcomeFlag == 1 && resumeAdFlag == 1
        }
    }
}
