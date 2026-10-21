package com.asim.jhbatteryapp.presentation.guide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ActivityHowToUseBinding
import com.asim.jhbatteryapp.databinding.ActivityMainBinding
import com.asim.jhbatteryapp.service.StatusBarAccessibilityService

class HowToUseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHowToUseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityHowToUseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }

        ViewCompat.requestApplyInsets(binding.root)

        setupStepTexts()
        setupClickListeners()
    }

    private fun setupStepTexts() {
        binding.tvStep1.text = Html.fromHtml(
            getString(R.string.how_to_use_step_1),
            Html.FROM_HTML_MODE_COMPACT
        )
        binding.tvStep2.text = Html.fromHtml(
            getString(R.string.how_to_use_step_2),
            Html.FROM_HTML_MODE_COMPACT
        )
        binding.tvStep3.text = Html.fromHtml(
            getString(R.string.how_to_use_step_3),
            Html.FROM_HTML_MODE_COMPACT
        )
        binding.tvStep4.text = Html.fromHtml(
            getString(R.string.how_to_use_step_4),
            Html.FROM_HTML_MODE_COMPACT
        )
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            com.universal.remote.ads.AdsController.back(this, "is_show_how_to_use_back_inter") {
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.btnBack.performClick()
            }
        })

        binding.btnGoToSettings.setOnClickListener {
            StatusBarAccessibilityService.openAccessibilitySettings(this)
        }
    }

    override fun onResume() {
        super.onResume()
        // If accessibility permission is granted while in settings and user returns, finish this guide
        if (StatusBarAccessibilityService.isAccessibilityServiceEnabled(this)) {
            finish()
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, HowToUseActivity::class.java)
            context.startActivity(intent)
        }
    }
}
