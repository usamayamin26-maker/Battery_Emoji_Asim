package com.jhw.batteryemoji.presentation.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.jhw.batteryemoji.ads.AppAds
import com.jhw.batteryemoji.data.datasource.PreferenceLocalDataSource
import com.jhw.batteryemoji.databinding.ActivityGetStartedBinding
import com.jhw.batteryemoji.databinding.ActivitySplashBinding
import com.jhw.batteryemoji.presentation.home.MainActivity
import com.jhw.batteryemoji.presentation.onboarding.OnboardingActivity
import com.jhw.batteryemoji.presentation.settings.LanguageActivity
import com.universal.remote.ads.AdsController

class GetStartedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetStartedBinding

    companion object {
        private const val SCREEN_KEY = "get_started"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        ViewCompat.requestApplyInsets(binding.root)
        if (AppAds.ADS_ENABLED) {
            bindAdSlots()
        }

        binding.btnApplyDetail.setOnClickListener {
            onGetStartedClicked()
        }
    }

    private fun bindAdSlots() {
        AdsController.slot(this, binding.adSlotTop, "${SCREEN_KEY}_top", units = SCREEN_KEY)
        AdsController.slot(this, binding.adSlotBottom, "${SCREEN_KEY}_bottom", units = SCREEN_KEY)
    }

    private fun onGetStartedClicked() {
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
        AdsController.forgetSlots(SCREEN_KEY)
        super.onDestroy()
    }
}
