package com.asim.jhbatteryapp

import android.app.Application
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.asim.jhbatteryapp.BuildConfig
import com.asim.jhbatteryapp.ads.AppAds
import com.asim.jhbatteryapp.presentation.welcome.WelcomeActivity
import com.universal.remote.ads.AdsController
import com.universal.remote.ads.AdsSetup

class JHBatteryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var isFirstStart = true

            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                if (isFirstStart) {
                    isFirstStart = false
                    return
                }
                // App came to foreground from background
                val intent = Intent(this@JHBatteryApplication, WelcomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("IS_REOPEN", true)
                }
                startActivity(intent)
            }
        })

        AdsController.init(
            this,
            AdsSetup(
                remoteConfigKey = AppAds.REMOTE_CONFIG_KEY,
                defaultJson = AppAds.DEFAULT_JSON,
                isDebug = BuildConfig.DEBUG,
            ),
        )
    }

    companion object {
        lateinit var instance: JHBatteryApplication
            private set
    }
}
