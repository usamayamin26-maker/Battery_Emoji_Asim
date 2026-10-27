package com.jhw.batteryemoji.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.core.Utils.Constants
import com.jhw.batteryemoji.databinding.FragmentSettingsBinding
import com.jhw.batteryemoji.presentation.home.HomeViewModel
import com.universal.remote.ads.AdsController

class SettingsFragment : Fragment() {

    companion object {
        private const val SCREEN_KEY = "settings"
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
        bindAdSlots()
    }

    private fun bindAdSlots() {
        val activity = activity ?: return
        AdsController.slot(
            activity = activity,
            container = binding.adSlotTop,
            slotKey = "${SCREEN_KEY}_top",
            units = SCREEN_KEY,
            isCurrent = { isAdded && _binding != null }
        )
        AdsController.slot(
            activity = activity,
            container = binding.adSlotBottom,
            slotKey = "${SCREEN_KEY}_bottom",
            units = SCREEN_KEY,
            isCurrent = { isAdded && _binding != null }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshEmojiBatteryState()
        updateCurrentLanguageLabel()
    }

    private fun updateCurrentLanguageLabel() {
        val currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val currentLangTag = if (!currentLocales.isEmpty) currentLocales.toLanguageTags() else java.util.Locale.getDefault().language
        val currentCode = currentLangTag.split("-").first().lowercase()
        val currentLangName = when (currentCode) {
            "ar" -> getString(R.string.lang_arabic)
            "de" -> getString(R.string.lang_german)
            "as" -> getString(R.string.lang_assamese)
            "fa" -> getString(R.string.lang_persian)
            "fr" -> getString(R.string.lang_french)
            "hi" -> getString(R.string.lang_hindi)
            "in", "id" -> getString(R.string.lang_indonesian)
            "iw", "he" -> getString(R.string.lang_hebrew)
            "ja" -> getString(R.string.lang_japanese)
            "ko" -> getString(R.string.lang_korean)
            "ms" -> getString(R.string.lang_malay)
            "pt" -> getString(R.string.lang_portuguese)
            "ru" -> getString(R.string.lang_russian)
            "tr" -> getString(R.string.lang_turkish)
            "ur" -> getString(R.string.lang_urdu)
            "vi" -> getString(R.string.lang_vietnamese)
            "zh" -> getString(R.string.lang_chinese)
            "nl" -> getString(R.string.lang_dutch)
            "es" -> getString(R.string.lang_spanish)
            "th" -> getString(R.string.lang_thai)
            "uk" -> getString(R.string.lang_ukrainian)
            else -> getString(R.string.lang_english)
        }
        binding.tvCurrentLanguage.text = currentLangName
    }

    private fun setupListeners() {
        // Banner Toggle (Both card and button)
        val bannerClickListener = View.OnClickListener {
            val isCurrentlyActive = viewModel.isEmojiBatteryEnabled.value ?: false
            if (isCurrentlyActive) {
                viewModel.toggleEmojiBattery()
                if (com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
                    com.jhw.batteryemoji.service.StatusBarAccessibilityService.updateCustomSettings()
                } else {
                    com.jhw.batteryemoji.service.StatusBarOverlayService.stop(requireContext())
                }
                Toast.makeText(requireContext(), getString(R.string.emoji_battery_deactivated), Toast.LENGTH_SHORT).show()
            } else {
                if (com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
                    viewModel.toggleEmojiBattery()
                    com.jhw.batteryemoji.service.StatusBarAccessibilityService.updateCustomSettings()
                    Toast.makeText(requireContext(), getString(R.string.emoji_battery_activated), Toast.LENGTH_SHORT).show()
                } else {
                    com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
                        context = requireContext()
                    )
                }
            }
        }
        binding.llEnableBanner.setOnClickListener(bannerClickListener)
        binding.btnBannerStart.setOnClickListener(bannerClickListener)

        // 1. Language Item -> Launch LanguageActivity
        binding.itemSettingLanguage.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_settings_btn_inter") {
                val intent = Intent(requireContext(), LanguageActivity::class.java).apply {
                    putExtra(LanguageActivity.EXTRA_FROM_SETTINGS, true)
                }
                startActivity(intent)
            }
        }

        // 2. Privacy Policy Item
        binding.itemSettingPrivacy.setOnClickListener {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PrivacyPolicyUrl))
                startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.opening_privacy_policy), Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Share App Item -> Show Share Emoji App Dialog matching the screenshot!
        binding.itemSettingShare.setOnClickListener {
            AppDialogs.showShareDialog(requireContext())
        }

        // 4. Rate Us Item -> 4-5 stars opens the Play Store, 1-3 stars asks for feedback
        binding.itemSettingRate.setOnClickListener {
            AppDialogs.showRateUsDialog(requireContext()) {
                AppDialogs.showEmojiFeedbackDialog(requireContext())
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isEmojiBatteryEnabled.observe(viewLifecycleOwner) { enabled ->
            if (enabled) {
                binding.tvBannerText.text = getString(R.string.emoji_battery_is_active)
                binding.btnBannerStart.text = getString(R.string.status_active)
            } else {
                binding.tvBannerText.text = getString(R.string.enable_emoji_battery_banner)
                binding.btnBannerStart.text = getString(R.string.start)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
