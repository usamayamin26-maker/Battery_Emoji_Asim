package com.asim.jhbatteryapp.presentation.settings

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
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.FragmentSettingsBinding
import com.asim.jhbatteryapp.presentation.common.ViewModelFactory
import com.asim.jhbatteryapp.presentation.home.HomeViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels {
        ViewModelFactory(requireContext())
    }

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
    }

    private fun setupListeners() {
        // Banner Toggle
        binding.btnBannerStart.setOnClickListener {
            viewModel.toggleEmojiBattery()
        }

        // 1. Language Item -> Navigate to LanguageFragment via NavGraph!
        binding.itemSettingLanguage.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_languageFragment)
        }

        // 2. Privacy Policy Item
        binding.itemSettingPrivacy.setOnClickListener {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
                startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Opening Privacy Policy…", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Share App Item -> Show Share Emoji App Dialog matching the screenshot!
        binding.itemSettingShare.setOnClickListener {
            AppDialogs.showShareDialog(requireContext())
        }

        // 4. Rate Us Item -> Show Rating / Feedback Dialog matching Image 2!
        binding.itemSettingRate.setOnClickListener {
            AppDialogs.showRateUsDialog(requireContext()) {
                // Also optionally demonstrate the emoji reaction dialog
                AppDialogs.showEmojiFeedbackDialog(requireContext())
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isEmojiBatteryEnabled.observe(viewLifecycleOwner) { enabled ->
            if (enabled) {
                binding.tvBannerText.text = "Emoji battery is active"
                binding.btnBannerStart.text = "Active"
            } else {
                binding.tvBannerText.text = getString(R.string.enable_emoji_battery_banner)
                binding.btnBannerStart.text = getString(R.string.start)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
