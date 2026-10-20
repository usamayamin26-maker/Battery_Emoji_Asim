package com.asim.jhbatteryapp.presentation.theme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.FragmentStatusBarThemeBinding
import androidx.fragment.app.viewModels
import com.universal.remote.ads.AdsController

class StatusBarThemeFragment : Fragment() {

    companion object {
        private const val SCREEN_KEY = "theme"
    }

    private var _binding: FragmentStatusBarThemeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatusBarThemeViewModel by viewModels()

    private lateinit var themeAdapter: StatusBarThemeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusBarThemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Clip preview capsule to rounded corners
        binding.clStatusBarPreview.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, resources.getDimension(com.intuit.sdp.R.dimen._22sdp))
            }
        }
        binding.clStatusBarPreview.clipToOutline = true

        setupRecyclerView()
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

    private fun setupRecyclerView() {
        themeAdapter = StatusBarThemeAdapter { selectedItem ->
            viewModel.selectTheme(selectedItem)
        }
        binding.rvThemesGrid.adapter = themeAdapter
    }

    private var isAwaitingPermission = false

    private fun setupListeners() {
        binding.ivThemeBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnTabGradient.setOnClickListener {
            viewModel.setTab(StatusBarThemeViewModel.ThemeTab.GRADIENT)
        }

        binding.btnTabBackground.setOnClickListener {
            viewModel.setTab(StatusBarThemeViewModel.ThemeTab.BACKGROUND)
        }

        binding.btnApplyTheme.setOnClickListener {
            if (com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
                applyCurrentTheme()
            } else {
                com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
                    context = requireContext(),
                    onSettingsOpened = {
                        isAwaitingPermission = true
                    }
                )
            }
        }
    }

    private fun applyCurrentTheme() {
        val selected = viewModel.selectedTheme.value ?: return
        viewModel.applyTheme(selected)

        val isAccessibilityEnabled = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
        if (isAccessibilityEnabled) {
            com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateTheme(
                themeRes = selected.drawableRes,
                isGradient = selected.isGradient
            )
        } else if (com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(requireContext())) {
            com.asim.jhbatteryapp.service.StatusBarOverlayService.start(requireContext())
        }
        Toast.makeText(
            requireContext(),
            getString(R.string.theme_applied_success),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()
        if (com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
            val selected = viewModel.selectedTheme.value
            if (selected != null) {
                com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateTheme(
                    themeRes = selected.drawableRes,
                    isGradient = selected.isGradient
                )
                if (isAwaitingPermission) {
                    isAwaitingPermission = false
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.theme_applied_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.currentTab.observe(viewLifecycleOwner) { tab ->
            updateTabUI(tab)
        }

        viewModel.selectedTheme.observe(viewLifecycleOwner) { item ->
            if (item != null) {
                binding.clStatusBarPreview.setBackgroundResource(item.drawableRes)
                themeAdapter.setSelectedId(item.id)

                val contentColor = if (item.isGradient) {
                    ContextCompat.getColor(requireContext(), R.color.white)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.text_primary)
                }
                binding.tvPreviewTime.setTextColor(contentColor)
                binding.tvPreviewBattery.setTextColor(contentColor)
                binding.ivPreviewSignal.imageTintList = android.content.res.ColorStateList.valueOf(contentColor)
                binding.ivPreviewWifi.imageTintList = android.content.res.ColorStateList.valueOf(contentColor)
                binding.ivPreviewBattery.imageTintList = android.content.res.ColorStateList.valueOf(contentColor)
                binding.ivPreviewBolt.imageTintList = android.content.res.ColorStateList.valueOf(contentColor)
            }
        }
    }

    private fun updateTabUI(tab: StatusBarThemeViewModel.ThemeTab) {
        val colorWhite = ContextCompat.getColor(requireContext(), R.color.white)
        val colorPurple = ContextCompat.getColor(requireContext(), R.color.primary_purple)

        when (tab) {
            StatusBarThemeViewModel.ThemeTab.GRADIENT -> {
                binding.btnTabGradient.setBackgroundResource(R.drawable.bg_tab_pill_active)
                binding.btnTabGradient.setTextColor(colorWhite)

                binding.btnTabBackground.setBackgroundResource(R.drawable.bg_tab_pill_inactive)
                binding.btnTabBackground.setTextColor(colorPurple)

                binding.tvChooseLabel.setText(R.string.choose_gradient)

                val items = viewModel.gradientThemes.value ?: emptyList()
                val currentSelectedId = viewModel.selectedTheme.value?.id ?: (items.firstOrNull()?.id ?: "")
                themeAdapter.setItems(items, currentSelectedId)
            }
            StatusBarThemeViewModel.ThemeTab.BACKGROUND -> {
                binding.btnTabGradient.setBackgroundResource(R.drawable.bg_tab_pill_inactive)
                binding.btnTabGradient.setTextColor(colorPurple)

                binding.btnTabBackground.setBackgroundResource(R.drawable.bg_tab_pill_active)
                binding.btnTabBackground.setTextColor(colorWhite)

                binding.tvChooseLabel.setText(R.string.choose_background)

                val items = viewModel.backgroundThemes.value ?: emptyList()
                val currentSelectedId = viewModel.selectedTheme.value?.id ?: (items.firstOrNull()?.id ?: "")
                themeAdapter.setItems(items, currentSelectedId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
