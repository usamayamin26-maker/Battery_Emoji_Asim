package com.jhw.batteryemoji.presentation.theme

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.core.Utils.MyUtils
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.FragmentStatusBarThemeBinding
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import com.jhw.batteryemoji.domain.model.StatusBarThemeItem
import com.jhw.batteryemoji.presentation.remote.RemoteAssetUiState
import com.universal.remote.ads.AdsController
import kotlinx.coroutines.launch

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
        checkAndFetchRemoteThemes()
    }

    private fun checkAndFetchRemoteThemes() {
        val currentRemoteList = if (viewModel.currentTab.value == StatusBarThemeViewModel.ThemeTab.BACKGROUND) {
            viewModel.remoteBackgroundThemes.value
        } else {
            viewModel.remoteGradientThemes.value
        }
        if (currentRemoteList.isNullOrEmpty()) {
            if (MyUtils.isNetworkAvailable(requireContext())) {
                viewModel.loadRemoteThemes()
            } else {
                MyUtils.showNoInternetDialog(
                    context = requireContext(),
                    onRetry = { viewModel.loadRemoteThemes() }
                )
            }
        }
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
        themeAdapter = StatusBarThemeAdapter(
            onRemoteItemClick = { remoteItem ->
                viewModel.selectRemoteTheme(remoteItem)
            }
        ) { selectedItem ->
            viewModel.selectTheme(selectedItem)
        }
        binding.rvThemesGrid.adapter = themeAdapter
    }

    private var isAwaitingPermission = false

    private fun setupListeners() {
        AdsController.routeSystemBack(this, binding.topHeader.backButton)
        binding.topHeader.backButton.setOnClickListener {
            AdsController.back(this, "is_show_theme_back_inter") {
                findNavController().navigateUp()
            }
        }

        binding.btnTabGradient.setOnClickListener {
            viewModel.setTab(StatusBarThemeViewModel.ThemeTab.GRADIENT)
        }

        binding.btnTabBackground.setOnClickListener {
            viewModel.setTab(StatusBarThemeViewModel.ThemeTab.BACKGROUND)
        }

        binding.btnApplyTheme.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_theme_btn_inter") {
                if (com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
                    applyCurrentTheme()
                } else {
                    com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
                        context = requireContext(),
                        onSettingsOpened = {
                            isAwaitingPermission = true
                        }
                    )
                }
            }
        }
    }

    private fun applyCurrentTheme() {
        viewModel.selectedRemoteTheme.value?.let { remote ->
            applyRemoteTheme(remote)
            return
        }
        val selected = viewModel.selectedTheme.value ?: return
        viewModel.applyTheme(selected)

        val isAccessibilityEnabled = com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
        if (isAccessibilityEnabled) {
            com.jhw.batteryemoji.service.StatusBarAccessibilityService.updateTheme(
                themeRes = selected.drawableRes,
                isGradient = selected.isGradient
            )
        } else if (com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(requireContext())) {
            com.jhw.batteryemoji.service.StatusBarOverlayService.start(requireContext())
        }
        Toast.makeText(
            requireContext(),
            getString(R.string.theme_applied_success),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun applyRemoteTheme(item: RemoteAssetItem) {
        val appContext = requireContext().applicationContext
        if (!MyUtils.isNetworkAvailable(requireContext())) {
            MyUtils.showNoInternetDialog(
                context = requireContext(),
                onRetry = { applyRemoteTheme(item) }
            )
            return
        }
        binding.btnApplyTheme.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val path = RemoteImageLoader.saveForApply(item)
            _binding?.btnApplyTheme?.isEnabled = true
            if (path == null) {
                Toast.makeText(appContext, R.string.remote_asset_download_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val isGradient = item.category.equals("gradient", ignoreCase = true) ||
                    item.type.equals("gradient", ignoreCase = true)
            viewModel.applyRemoteTheme(path, isGradient)

            val ctx = context ?: return@launch
            if (com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(ctx)) {
                com.jhw.batteryemoji.service.StatusBarAccessibilityService.updateRemoteTheme(path, isGradient)
                Toast.makeText(appContext, getString(R.string.theme_applied_success), Toast.LENGTH_SHORT).show()
            } else if (com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(ctx)) {
                com.jhw.batteryemoji.service.StatusBarOverlayService.start(ctx)
                Toast.makeText(appContext, getString(R.string.theme_applied_success), Toast.LENGTH_SHORT).show()
            } else {
                com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
                    context = ctx,
                    onSettingsOpened = { isAwaitingPermission = true }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
            if (isAwaitingPermission) {
                isAwaitingPermission = false
                applyCurrentTheme()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.currentTab.observe(viewLifecycleOwner) { tab ->
            updateTabUI(tab)
        }

        viewModel.selectedTheme.observe(viewLifecycleOwner) { item ->
            if (item != null) {
                binding.clStatusBarPreview.background = null
                binding.ivPreviewThemeBg.tag = null
                binding.ivPreviewThemeBg.setImageResource(item.drawableRes)
                themeAdapter.setSelectedId(item.id)

                val contentColor = if (item.isGradient) {
                    ContextCompat.getColor(requireContext(), R.color.white)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.text_primary)
                }
                updatePreviewColors(contentColor)
            }
        }

        viewModel.selectedRemoteTheme.observe(viewLifecycleOwner) { remote ->
            if (remote != null) {
                binding.clStatusBarPreview.background = null
                RemoteImageLoader.loadImage(remote, binding.ivPreviewThemeBg)
                themeAdapter.setSelectedId(remote.id)

                val isGradient = remote.category.equals("gradient", ignoreCase = true) ||
                        remote.type.equals("gradient", ignoreCase = true)
                val contentColor = if (isGradient) {
                    Color.WHITE
                } else {
                    ContextCompat.getColor(requireContext(), R.color.text_primary)
                }
                updatePreviewColors(contentColor)
            }
        }

        viewModel.remoteGradientThemes.observe(viewLifecycleOwner) { remoteList ->
            if (viewModel.currentTab.value == StatusBarThemeViewModel.ThemeTab.GRADIENT) {
                themeAdapter.submitRemoteList(remoteList)
            }
        }

        viewModel.remoteBackgroundThemes.observe(viewLifecycleOwner) { remoteList ->
            if (viewModel.currentTab.value == StatusBarThemeViewModel.ThemeTab.BACKGROUND) {
                themeAdapter.submitRemoteList(remoteList)
            }
        }

        viewModel.remoteThemeState.observe(viewLifecycleOwner) { state ->
            if (state is RemoteAssetUiState.Error && !MyUtils.isNetworkAvailable(requireContext())) {
                MyUtils.showNoInternetDialog(
                    context = requireContext(),
                    onRetry = { viewModel.loadRemoteThemes() }
                )
            }
        }
    }

    private fun updatePreviewColors(contentColor: Int) {
        binding.tvPreviewTime.setTextColor(contentColor)
        binding.tvPreviewBattery.setTextColor(contentColor)
        binding.ivPreviewSignal.imageTintList = ColorStateList.valueOf(contentColor)
        binding.ivPreviewWifi.imageTintList = ColorStateList.valueOf(contentColor)
        binding.ivPreviewBattery.imageTintList = ColorStateList.valueOf(contentColor)
        binding.ivPreviewBolt.imageTintList = ColorStateList.valueOf(contentColor)
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
                val currentSelectedId = viewModel.selectedTheme.value?.id
                    ?: viewModel.selectedRemoteTheme.value?.id
                    ?: (items.firstOrNull()?.id ?: "")
                themeAdapter.setItems(items, currentSelectedId)
                themeAdapter.submitRemoteList(viewModel.remoteGradientThemes.value ?: emptyList())
            }
            StatusBarThemeViewModel.ThemeTab.BACKGROUND -> {
                binding.btnTabGradient.setBackgroundResource(R.drawable.bg_tab_pill_inactive)
                binding.btnTabGradient.setTextColor(colorPurple)

                binding.btnTabBackground.setBackgroundResource(R.drawable.bg_tab_pill_active)
                binding.btnTabBackground.setTextColor(colorWhite)

                binding.tvChooseLabel.setText(R.string.choose_background)

                val items = viewModel.backgroundThemes.value ?: emptyList()
                val currentSelectedId = viewModel.selectedTheme.value?.id
                    ?: viewModel.selectedRemoteTheme.value?.id
                    ?: (items.firstOrNull()?.id ?: "")
                themeAdapter.setItems(items, currentSelectedId)
                themeAdapter.submitRemoteList(viewModel.remoteBackgroundThemes.value ?: emptyList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
