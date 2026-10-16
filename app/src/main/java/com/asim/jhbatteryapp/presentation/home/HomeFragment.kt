package com.asim.jhbatteryapp.presentation.home

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.DialogBatteryPreviewBinding
import com.asim.jhbatteryapp.databinding.FragmentHomeBinding
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.presentation.common.NotificationPermissionDialog
import com.google.android.material.bottomsheet.BottomSheetDialog

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    private lateinit var emojiAdapter: BatteryEmojiAdapter
    private lateinit var categoryAdapter: CategoryTabAdapter

    private var isPermissionDialogShowing = false
    private var hasPromptedNotificationInSession = false
    private var hasPromptedAccessibilityInSession = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPermissionDialogShowing = false
        hasPromptedNotificationInSession = true
        if (isGranted) {
            Toast.makeText(requireContext(), "Notification permission granted!", Toast.LENGTH_SHORT).show()
        }
        if (!com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext()) && !hasPromptedAccessibilityInSession) {
            checkAccessibilityPermissionOnHome()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (_binding == null) {
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            setupRecyclerViews()
            setupListeners()
        } else {
            (_binding?.root?.parent as? ViewGroup)?.removeView(_binding?.root)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
    }

    private fun checkPermissionsOnHome() {
        if (!isAdded || isPermissionDialogShowing) return

        val hasAccessibility = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())

        // 1. Accessibility is required for the status bar & emoji battery: If OFF, show dialog again!
        if (!hasAccessibility) {
            checkAccessibilityPermissionOnHome()
            return
        }

        // 2. Notification prompt (once per session when accessibility is enabled)
        val hasNotification = NotificationPermissionDialog.hasNotificationPermission(requireContext())
        if (!hasNotification && !hasPromptedNotificationInSession) {
            hasPromptedNotificationInSession = true
            isPermissionDialogShowing = true
            com.asim.jhbatteryapp.core.Utils.MyUtils.showDontMissOutDialog(
                context = requireContext(),
                onTurnOn = {
                    isPermissionDialogShowing = false
                    showNotifyPermissionDialog()
                },
                onMaybeLater = {
                    isPermissionDialogShowing = false
                }
            )
        }
    }

    private fun showNotifyPermissionDialog() {
        if (!isAdded || isPermissionDialogShowing) return
        isPermissionDialogShowing = true

        NotificationPermissionDialog.show(
            context = requireContext(),
            onAllow = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    isPermissionDialogShowing = false
                    hasPromptedNotificationInSession = true
                    try {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                        }
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            },
            onNotNow = {
                isPermissionDialogShowing = false
                hasPromptedNotificationInSession = true
            }
        )
    }

    private fun checkAccessibilityPermissionOnHome() {
        if (!isAdded || isPermissionDialogShowing) return
        if (com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
            return
        }

        isPermissionDialogShowing = true
        com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
            context = requireContext(),
            onSettingsOpened = {
                isPermissionDialogShowing = false
            },
            onDismissed = {
                isPermissionDialogShowing = false
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshEmojiBatteryState()
        checkPermissionsOnHome()
    }

    private fun setupRecyclerViews() {
        // Category Tabs
        categoryAdapter = CategoryTabAdapter { category ->
            viewModel.selectCategory(category.id)
        }
        binding.rvCategoryTabs.itemAnimator = null
        binding.rvCategoryTabs.setHasFixedSize(true)
        binding.rvCategoryTabs.adapter = categoryAdapter

        // Main Emoji Grid (3 columns)
        emojiAdapter = BatteryEmojiAdapter { item ->
            showPreviewBottomSheet(item)
        }
        binding.rvEmojiGrid.itemAnimator = null
        binding.rvEmojiGrid.setHasFixedSize(true)
        binding.rvEmojiGrid.adapter = emojiAdapter
    }

    private fun setupListeners() {
        // Navigate to SearchFragment using NavGraph action!
        binding.btnOpenSearch.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        // Banner Start Button & Entire Card
        val bannerClickListener = View.OnClickListener {
            val isCurrentlyActive = viewModel.isEmojiBatteryEnabled.value ?: false
            if (isCurrentlyActive) {
                viewModel.toggleEmojiBattery()
                if (com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
                    com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateCustomSettings()
                } else {
                    com.asim.jhbatteryapp.service.StatusBarOverlayService.stop(requireContext())
                }
                Toast.makeText(requireContext(), "Emoji battery deactivated", Toast.LENGTH_SHORT).show()
            } else {
                if (com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
                    viewModel.toggleEmojiBattery()
                    com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateCustomSettings()
                    Toast.makeText(requireContext(), "Emoji battery activated!", Toast.LENGTH_SHORT).show()
                } else {
                    com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
                        context = requireContext()
                    )
                }
            }
        }
        binding.llEnableBanner.setOnClickListener(bannerClickListener)
        binding.btnBannerStart.setOnClickListener(bannerClickListener)

        // Quick Action Cards
        binding.cardStickerShop.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_stickerDisplayFragment)
        }
        binding.cardAnimation.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_statusBarAnimationFragment)
        }
        binding.cardThemes.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_statusBarThemeFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.setCategories(categories)
        }

        viewModel.emojis.observe(viewLifecycleOwner) { emojis ->
            emojiAdapter.submitList(emojis)
        }

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

    private fun showPreviewBottomSheet(item: BatteryEmojiItem) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogBatteryPreviewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.cvPreviewContainer.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), item.backgroundColorRes)
        )
        dialogBinding.ivDialogMascot.setImageResource(item.previewDrawableRes)
        dialogBinding.tvDialogTitle.text = item.title

        dialogBinding.btnApplyStyle.setOnButtonClickListener {
            val hasAccessibility = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
            if (!hasAccessibility) {
                dialog.dismiss()
                checkAccessibilityPermissionOnHome()
                return@setOnButtonClickListener
            }
            val prefRepo = com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl()
            prefRepo.setAppliedBatteryIconRes(item.previewDrawableRes)
            prefRepo.setBatteryIconTintable(false)
            prefRepo.setEmojiBatteryEnabled(true)
            com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateBatteryIcon(item.previewDrawableRes, isTintable = false)
            Toast.makeText(requireContext(), "${item.title} applied as battery icon!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isPermissionDialogShowing = false
        // Retain _binding cached across fragment transitions so return is instant with zero blink
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
