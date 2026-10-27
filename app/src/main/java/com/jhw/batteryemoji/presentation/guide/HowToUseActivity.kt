package com.jhw.batteryemoji.presentation.guide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.databinding.ActivityHowToUseBinding
import com.jhw.batteryemoji.domain.model.AccessibilityPermissionStatus
import com.jhw.batteryemoji.presentation.common.PermissionSettingsNavigator
import org.koin.androidx.viewmodel.ext.android.viewModel

class HowToUseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHowToUseBinding
    private val viewModel: AccessibilityPermissionViewModel by viewModel()

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
        viewModel.status.observe(this, ::render)
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
        binding.tvRestrictedSteps.text = Html.fromHtml(
            getString(R.string.restricted_settings_steps),
            Html.FROM_HTML_MODE_COMPACT
        )
    }

    private fun setupClickListeners() {
        binding.topHeader.backButton.setOnClickListener {
            com.universal.remote.ads.AdsController.back(this, "is_show_how_to_use_back_inter") {
                finish()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.topHeader.backButton.performClick()
            }
        })

        binding.btnGoToSettings.setOnClickListener {
            PermissionSettingsNavigator.openAccessibilitySettings(this)
        }

        binding.btnOpenAppInfo.setOnClickListener {
            if (!PermissionSettingsNavigator.openAppInfo(this)) {
                Toast.makeText(this, R.string.app_info_open_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Back from Settings / App Info: re-read the state, the UI follows through render()
        viewModel.refresh()
    }

    private fun render(status: AccessibilityPermissionStatus) {
        // Granted while in Settings: the guide has done its job
        if (status.isServiceEnabled) {
            finish()
            return
        }
        binding.cardRestrictedSettings.visibility =
            if (status.mayNeedRestrictedSettings) View.VISIBLE else View.GONE
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, HowToUseActivity::class.java)
            context.startActivity(intent)
        }
    }
}
