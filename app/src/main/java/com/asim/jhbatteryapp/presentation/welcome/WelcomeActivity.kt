package com.asim.jhbatteryapp.presentation.welcome

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.asim.jhbatteryapp.databinding.ActivityWelcomeBinding
import com.asim.jhbatteryapp.presentation.home.MainActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // If this activity is being recreated, just finish it to avoid loops
        if (intent?.getBooleanExtra("IS_REOPEN", false) == false) {
             // We want to avoid showing it on cold start if Splash is already showing something similar
             // But the user specifically asked for background -> foreground.
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
            navigateToMain()
        }

        binding.root.postDelayed({
            navigateToMain()
        }, 3500L)
    }

    private fun navigateToMain() {
        if (hasNavigated) return
        hasNavigated = true
        // If we are already on MainActivity, we don't need to start it again, just finish.
        // However, starting it with CLEAR_TOP or SINGLE_TOP is safer.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
}
