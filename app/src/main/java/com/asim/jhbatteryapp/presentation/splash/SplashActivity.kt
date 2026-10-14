package com.asim.jhbatteryapp.presentation.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.asim.jhbatteryapp.data.datasource.PreferenceLocalDataSource
import com.asim.jhbatteryapp.databinding.ActivitySplashBinding
import com.asim.jhbatteryapp.presentation.home.MainActivity
import com.asim.jhbatteryapp.presentation.onboarding.OnboardingActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }

        ViewCompat.requestApplyInsets(binding.root)
        startProgressAndNavigate()
    }

    private fun startProgressAndNavigate() {
        // Animate the Gradient Linear Progress Indicator smoothly from 0 to 100
        val progressAnimator = ObjectAnimator.ofInt(
            binding.gradientProgressIndicator,
            "progress",
            0,
            100
        ).apply {
            duration = 2200 // 2.2 seconds smooth load
            interpolator = DecelerateInterpolator()
        }

        progressAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Check if onboarding was already viewed
                val preferences = PreferenceLocalDataSource(this@SplashActivity)
                val targetActivity = if (preferences.isOnboardingCompleted()) {
                    MainActivity::class.java
                } else {
                    OnboardingActivity::class.java
                }
                startActivity(Intent(this@SplashActivity, targetActivity))
                finish()
            }
        })

        progressAnimator.start()
    }
}
