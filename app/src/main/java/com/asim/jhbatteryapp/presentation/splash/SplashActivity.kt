package com.asim.jhbatteryapp.presentation.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.asim.jhbatteryapp.R
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
        binding.gradientProgressIndicator.visibility = View.VISIBLE

        binding.root.postDelayed({
            val preferences = PreferenceLocalDataSource(this@SplashActivity)

            val targetActivity =
                if (preferences.isOnboardingCompleted()) {
                    MainActivity::class.java
                } else {
                    OnboardingActivity::class.java
                }

            startActivity(Intent(this@SplashActivity, targetActivity))
            finish()
        }, 6000L)
    }
}
