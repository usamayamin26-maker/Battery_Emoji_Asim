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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.remote.RemoteImageLoader
import com.asim.jhbatteryapp.databinding.DialogBatteryPreviewBinding
import com.asim.jhbatteryapp.databinding.FragmentHomeBinding
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.model.RemoteAssetItem
import com.asim.jhbatteryapp.presentation.common.NotificationPermissionDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.universal.remote.ads.AdsController
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    companion object {
        private const val SCREEN_KEY = "home"
    }

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
            Toast.makeText(requireContext(), getString(R.string.notification_permission_granted), Toast.LENGTH_SHORT).show()
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
                        com.asim.jhbatteryapp.JHBatteryApplication.skipNextWelcome()
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
        emojiAdapter = BatteryEmojiAdapter(
            onRemoteItemClick = { remoteItem ->
                val act = activity ?: return@BatteryEmojiAdapter
                AdsController.interstitial(act, "is_show_home_btn_inter") {
                    showRemotePreviewBottomSheet(remoteItem)
                }
            }
        ) { item ->
            val act = activity ?: return@BatteryEmojiAdapter
            AdsController.interstitial(act, "is_show_home_btn_inter") {
                showPreviewBottomSheet(item)
            }
        }
        binding.rvEmojiGrid.itemAnimator = null
        binding.rvEmojiGrid.setHasFixedSize(true)
        binding.rvEmojiGrid.setItemViewCacheSize(30)
        binding.rvEmojiGrid.adapter = emojiAdapter
    }

    private fun setupListeners() {
        // Navigate to SearchFragment using NavGraph action!
        binding.topHeader.actionButton.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_home_btn_inter") {
                findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
            }
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
                Toast.makeText(requireContext(), getString(R.string.emoji_battery_deactivated), Toast.LENGTH_SHORT).show()
            } else {
                if (com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())) {
                    viewModel.toggleEmojiBattery()
                    com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateCustomSettings()
                    Toast.makeText(requireContext(), getString(R.string.emoji_battery_activated), Toast.LENGTH_SHORT).show()
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
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_home_btn_inter") {
                findNavController().navigate(R.id.action_homeFragment_to_stickerDisplayFragment)
            }
        }
        binding.cardAnimation.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_home_btn_inter") {
                findNavController().navigate(R.id.action_homeFragment_to_statusBarAnimationFragment)
            }
        }
        binding.cardThemes.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_home_btn_inter") {
                findNavController().navigate(R.id.action_homeFragment_to_statusBarThemeFragment)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.setCategories(categories)
        }

        viewModel.emojis.observe(viewLifecycleOwner) { emojis ->
            emojiAdapter.submitList(emojis)
            com.asim.jhbatteryapp.core.Utils.ImageLoaderUtils.preload(
                context = requireContext(),
                drawableResList = emojis.map { it.previewDrawableRes }
            )
        }

        // Remote emojis from the JSON manifest, appended after local items
        viewModel.remoteEmojis.observe(viewLifecycleOwner) { remote ->
            emojiAdapter.submitRemoteList(remote)
        }

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

    private fun showPreviewBottomSheet(item: BatteryEmojiItem) {
        com.asim.jhbatteryapp.core.Utils.MyUtils.genBottomSheetDialog(
            context = requireContext(),
            style = R.style.Style_Rounded_Bottom_Sheet,
            isCancelable = true,
            bindingInflater = DialogBatteryPreviewBinding::inflate
        ) { dialogBinding ->
            dialogBinding.cvPreviewContainer.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), item.backgroundColorRes)
            )
            com.asim.jhbatteryapp.core.Utils.ImageLoaderUtils.loadOptimizedEmoji(
                imageView = dialogBinding.ivDialogMascot,
                drawableRes = item.previewDrawableRes,
                targetWidthDp = 160,
                targetHeightDp = 160
            )
            dialogBinding.tvDialogTitle.text = item.title

            dialogBinding.btnApplyStyle.setOnClickListener {
                val hasAccessibility = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
                if (!hasAccessibility) {
                    dismiss()
                    checkAccessibilityPermissionOnHome()
                    return@setOnClickListener
                }
                val prefRepo = com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl()
                prefRepo.setAppliedBatteryIconRes(item.previewDrawableRes)
                prefRepo.setBatteryIconTintable(false)
                prefRepo.setEmojiBatteryEnabled(true)
                com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateBatteryIcon(item.previewDrawableRes, isTintable = false)
                Toast.makeText(requireContext(), getString(R.string.item_applied_battery_format, item.title), Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    /** Same preview sheet as [showPreviewBottomSheet], for an emoji from the remote manifest. */
    private fun showRemotePreviewBottomSheet(item: RemoteAssetItem) {
        com.asim.jhbatteryapp.core.Utils.MyUtils.genBottomSheetDialog(
            context = requireContext(),
            style = R.style.Style_Rounded_Bottom_Sheet,
            isCancelable = true,
            bindingInflater = DialogBatteryPreviewBinding::inflate
        ) { dialogBinding ->
            val bgColor = try {
                android.graphics.Color.parseColor(item.backgroundColor)
            } catch (_: Throwable) {
                ContextCompat.getColor(requireContext(), R.color.card_bg_pink)
            }
            dialogBinding.cvPreviewContainer.setCardBackgroundColor(bgColor)
            RemoteImageLoader.loadImage(item, dialogBinding.ivDialogMascot)
            dialogBinding.tvDialogTitle.text = item.name

            dialogBinding.btnApplyStyle.setOnClickListener {
                val hasAccessibility = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
                if (!hasAccessibility) {
                    dismiss()
                    checkAccessibilityPermissionOnHome()
                    return@setOnClickListener
                }
                dialogBinding.btnApplyStyle.isEnabled = false
                val appContext = requireContext().applicationContext
                viewLifecycleOwner.lifecycleScope.launch {
                    val path = RemoteImageLoader.saveForApply(item)
                    if (path == null) {
                        Toast.makeText(appContext, R.string.remote_asset_download_failed, Toast.LENGTH_SHORT).show()
                        dialogBinding.btnApplyStyle.isEnabled = true
                        return@launch
                    }
                    val prefRepo = com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl()
                    prefRepo.setAppliedBatteryIconPath(path)
                    // Res 0 tells the overlay to draw the downloaded file instead of a drawable
                    prefRepo.setAppliedBatteryIconRes(0)
                    prefRepo.setBatteryIconTintable(false)
                    prefRepo.setEmojiBatteryEnabled(true)
                    com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateCustomSettings()
                    Toast.makeText(appContext, appContext.getString(R.string.item_applied_battery_format, item.name), Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isPermissionDialogShowing = false
        AdsController.forgetSlots(SCREEN_KEY)
        // Retain _binding cached across fragment transitions so return is instant with zero blink
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
