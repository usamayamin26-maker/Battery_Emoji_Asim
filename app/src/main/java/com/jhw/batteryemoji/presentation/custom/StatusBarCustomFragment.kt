package com.jhw.batteryemoji.presentation.custom

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.FragmentStatusBarCustomBinding
import com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper
import com.jhw.batteryemoji.presentation.home.BatteryEmojiAdapter
import com.jhw.batteryemoji.service.StatusBarAccessibilityService
import com.jhw.batteryemoji.service.StatusBarOverlayService
import com.universal.remote.ads.AdsController
import kotlinx.coroutines.launch

class StatusBarCustomFragment : Fragment() {

    companion object {
        private const val SCREEN_KEY = "custom"
    }

    private var _binding: FragmentStatusBarCustomBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatusBarCustomViewModel by viewModels()

    private lateinit var colorSwatchAdapter: ColorSwatchAdapter
    private lateinit var batteryStyleAdapter: BatteryStyleAdapter
    private lateinit var mascotAdapter: BatteryEmojiAdapter
    private lateinit var customIconAdapter: CustomStatusIconAdapter
    private lateinit var customLeftEmojiAdapter: CustomStatusEmojiAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusBarCustomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPreviewCapsule()
        setupRecyclerViews()
        setupTabListeners()
        setupSliderListeners()
        setupApplyButton()
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

    private fun setupPreviewCapsule() {
        // Ensure preview capsule has smooth rounded corners
        binding.clStatusBarPreview.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(
                    0, 0, view.width, view.height,
                    resources.getDimension(com.intuit.sdp.R.dimen._22sdp)
                )
            }
        }
        binding.clStatusBarPreview.clipToOutline = true
    }

    private fun setupRecyclerViews() {
        // Color swatches in Appearance tab
        colorSwatchAdapter = ColorSwatchAdapter { swatch ->
            viewModel.selectColor(swatch)
        }
        binding.rvColorSwatches.adapter = colorSwatchAdapter

        batteryStyleAdapter = BatteryStyleAdapter { styleItem ->
            viewModel.selectBatteryStyle(styleItem)
        }
        binding.rvBatteryStyles.adapter = batteryStyleAdapter

        mascotAdapter = BatteryEmojiAdapter(
            onRemoteItemClick = { remoteMascot -> viewModel.selectRemoteMascot(remoteMascot) },
            onItemClick = { emojiItem -> viewModel.selectMascot(emojiItem) }
        )
        binding.rvBatteryMascots.adapter = mascotAdapter

        customIconAdapter = CustomStatusIconAdapter(
            onIconClicked = { iconItem ->
                val bundle = Bundle().apply {
                    putString("iconId", iconItem.id)
                }
                val act = activity ?: return@CustomStatusIconAdapter
                AdsController.interstitial(act, "is_show_custom_btn_inter") {
                    findNavController().navigate(
                        R.id.action_statusBarCustomFragment_to_customIconDetailFragment,
                        bundle
                    )
                }
            },
            onCheckClicked = { iconItem ->
                viewModel.toggleCustomIcon(iconItem.id)
            }
        )
        binding.rvCustomizeIcons.adapter = customIconAdapter

        // Emojis below custom icons (shows on left side with time in status bar)
        customLeftEmojiAdapter = CustomStatusEmojiAdapter { emojiItem ->
            viewModel.selectLeftEmoji(emojiItem)
            // Removed immediate service update to prevent mode flipping
        }
        binding.rvCustomLeftEmojis.adapter = customLeftEmojiAdapter
    }

    private fun setupTabListeners() {
        AdsController.routeSystemBack(this, binding.topHeader.backButton)
        binding.topHeader.backButton.setOnClickListener {
            AdsController.back(this, "is_show_custom_back_inter") {
                findNavController().navigateUp()
            }
        }

        binding.tabSize.setOnClickListener {
            viewModel.setTab(StatusBarCustomViewModel.CustomTab.SIZE)
        }

        binding.tabAppearance.setOnClickListener {
            viewModel.setTab(StatusBarCustomViewModel.CustomTab.APPEARANCE)
        }

        binding.tabBattery.setOnClickListener {
            viewModel.setTab(StatusBarCustomViewModel.CustomTab.BATTERY)
        }

        binding.tabCustomizeIcon.setOnClickListener {
            viewModel.setTab(StatusBarCustomViewModel.CustomTab.CUSTOMIZE_ICON)
        }

        // Appearance sub-tabs
        binding.btnSubTabIcon.setOnClickListener {
            viewModel.setAppearanceSubTab(StatusBarCustomViewModel.AppearanceSubTab.ICON)
        }

        binding.btnSubTabBackground.setOnClickListener {
            viewModel.setAppearanceSubTab(StatusBarCustomViewModel.AppearanceSubTab.BACKGROUND)
        }

        // Battery category filter tabs
        binding.btnBatteryTabTrending.setOnClickListener {
            updateBatteryCategoryFilter("trending")
        }
        binding.btnBatteryTabHalloween.setOnClickListener {
            updateBatteryCategoryFilter("halloween")
        }
        binding.btnBatteryTabWC2.setOnClickListener {
            updateBatteryCategoryFilter("wc2")
        }
    }

    private fun updateBatteryCategoryFilter(category: String) {
        val purple = ContextCompat.getColor(requireContext(), R.color.primary_purple)
        val muted = ContextCompat.getColor(requireContext(), R.color.tab_inactive)

        binding.btnBatteryTabTrending.setTextColor(if (category == "trending") purple else muted)
        binding.btnBatteryTabHalloween.setTextColor(if (category == "halloween") purple else muted)
        binding.btnBatteryTabWC2.setTextColor(if (category == "wc2") purple else muted)
        viewModel.setBatteryCategory(category)
    }

    private fun setupSliderListeners() {
        // Height slider
        binding.sbHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = if (progress < 22) 22 else progress
                binding.tvHeightValue.text = "${clamped}dp"
                if (fromUser) {
                    viewModel.setHeight(clamped)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Left Margin slider
        binding.sbLeftMargin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvLeftMarginValue.text = "${progress}dp"
                if (fromUser) {
                    viewModel.setLeftMargin(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Right Margin slider
        binding.sbRightMargin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvRightMarginValue.text = "${progress}dp"
                if (fromUser) {
                    viewModel.setRightMargin(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Battery Percentage text size slider
        binding.sbPercentageSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = if (progress < 8) 8 else progress
                binding.tvPercentageSizeValue.text = "${clamped}dp"
                if (fromUser) {
                    viewModel.setBatteryPercentageSize(clamped)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Battery Icon Size slider
        binding.sbIconSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = if (progress < 12) 12 else progress
                binding.tvIconSizeValue.text = "${clamped}dp"
                if (fromUser) {
                    viewModel.setBatteryIconSize(clamped)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updatePreviewMargins() {
        val density = resources.displayMetrics.density
        val lm = viewModel.leftMarginDp.value ?: 0
        val rm = viewModel.rightMarginDp.value ?: 16
        val lmPx = (lm * density).toInt()
        val rmPx = (rm * density).toInt()
        binding.clStatusBarPreview.setPadding(lmPx, 0, rmPx, 0)
        
        // Ensure preview background stays correct during padding changes
        updatePreviewAppearance()
    }

    private fun setupApplyButton() {
        binding.btnApplyCustom.setOnClickListener {
            val hasAccessibility = StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())

            if (!hasAccessibility) {
                StatusBarPermissionHelper.showAccessibilityDialog(requireContext())
                return@setOnClickListener
            }

            val remoteBattery = viewModel.selectedRemoteBattery.value
            val remoteLeftEmoji = viewModel.selectedRemoteLeftEmoji
            if (remoteBattery == null && remoteLeftEmoji == null) {
                applyAndNotify(null, null)
                return@setOnClickListener
            }
            // Remote picks are saved here (from the cache, downloading if needed) for the overlay
            val appContext = requireContext().applicationContext
            binding.btnApplyCustom.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val batteryPath = remoteBattery?.let { RemoteImageLoader.saveForApply(it) }
                val leftEmojiPath = remoteLeftEmoji?.let {
                    // Keep the battery file saved above: its path isn't stored until applyAndNotify
                    RemoteImageLoader.saveForApply(it, alsoKeep = listOfNotNull(batteryPath))
                }
                _binding?.btnApplyCustom?.isEnabled = true
                if ((remoteBattery != null && batteryPath == null) ||
                    (remoteLeftEmoji != null && leftEmojiPath == null)
                ) {
                    Toast.makeText(appContext, R.string.remote_asset_download_failed, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                applyAndNotify(batteryPath, leftEmojiPath)
            }
        }
    }

    private fun applyAndNotify(remoteBatteryIconPath: String?, remoteLeftEmojiPath: String?) {
        viewModel.applyCustomizations(remoteBatteryIconPath, remoteLeftEmojiPath)
        StatusBarAccessibilityService.updateCustomSettings()

        // Immediately sync left emoji as well
        val res = viewModel.selectedLeftEmojiRes.value ?: 0
        StatusBarAccessibilityService.updateMascot(res)

        val ctx = context ?: return
        Toast.makeText(
            ctx,
            getString(R.string.apply_custom_success),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun observeViewModel() {
        // Main Tab selection
        viewModel.currentTab.observe(viewLifecycleOwner) { tab ->
            renderTab(tab)
        }

        // Appearance Sub-Tab
        viewModel.appearanceSubTab.observe(viewLifecycleOwner) { subTab ->
            if (subTab == StatusBarCustomViewModel.AppearanceSubTab.ICON) {
                binding.btnSubTabIcon.setBackgroundResource(R.drawable.bg_tab_pill_active)
                binding.btnSubTabIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.btnSubTabBackground.setBackgroundResource(R.drawable.bg_tab_pill_inactive)
                binding.btnSubTabBackground.setTextColor(ContextCompat.getColor(requireContext(), R.color.tab_inactive))
            } else {
                binding.btnSubTabBackground.setBackgroundResource(R.drawable.bg_tab_pill_active)
                binding.btnSubTabBackground.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.btnSubTabIcon.setBackgroundResource(R.drawable.bg_tab_pill_inactive)
                binding.btnSubTabIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.tab_inactive))
            }
        }

        // Height Live Update
        viewModel.heightDp.observe(viewLifecycleOwner) { h ->
            binding.tvHeightValue.text = "${h}dp"
            if (binding.sbHeight.progress != h) {
                binding.sbHeight.progress = h
            }
            val density = resources.displayMetrics.density
            val previewParams = binding.clStatusBarPreview.layoutParams
            previewParams.height = (h * density).toInt()
            binding.clStatusBarPreview.layoutParams = previewParams
            
            // Re-apply preview background after layout changes
            updatePreviewAppearance()
        }

        // Left Margin Live Update
        viewModel.leftMarginDp.observe(viewLifecycleOwner) { lm ->
            binding.tvLeftMarginValue.text = "${lm}dp"
            if (binding.sbLeftMargin.progress != lm) {
                binding.sbLeftMargin.progress = lm
            }
            updatePreviewMargins()
        }

        // Right Margin Live Update
        viewModel.rightMarginDp.observe(viewLifecycleOwner) { rm ->
            binding.tvRightMarginValue.text = "${rm}dp"
            if (binding.sbRightMargin.progress != rm) {
                binding.sbRightMargin.progress = rm
            }
            updatePreviewMargins()
        }

        // Appearance & Background Live Update
        viewModel.backgroundColor.observe(viewLifecycleOwner) { updatePreviewAppearance() }
        viewModel.iconColor.observe(viewLifecycleOwner) { updatePreviewAppearance() }
        viewModel.lastAppliedThemeMode.observe(viewLifecycleOwner) { updatePreviewAppearance() }
        viewModel.appliedThemeDrawableRes.observe(viewLifecycleOwner) { updatePreviewAppearance() }
        viewModel.isAppliedThemeGradient.observe(viewLifecycleOwner) { updatePreviewAppearance() }
        viewModel.isBatteryIconTintable.observe(viewLifecycleOwner) { updatePreviewAppearance() }

        // Battery Percentage Size Live Update
        viewModel.batteryPercentageSizeSp.observe(viewLifecycleOwner) { sp ->
            binding.tvPercentageSizeValue.text = "${sp}dp"
            if (binding.sbPercentageSize.progress != sp) {
                binding.sbPercentageSize.progress = sp
            }
            binding.tvPreviewBattery.textSize = sp.toFloat()
            binding.tvPercentageMiniPreview.textSize = sp.toFloat()
        }

        // Battery Icon Size Live Update
        viewModel.batteryIconSizeDp.observe(viewLifecycleOwner) { size ->
            binding.tvIconSizeValue.text = "${size}dp"
            if (binding.sbIconSize.progress != size) {
                binding.sbIconSize.progress = size
            }
            val density = resources.displayMetrics.density
            val iconWidth = (size * 1.5f * density).toInt()
            val iconHeight = (size * density).toInt()

            val pParams = binding.ivPreviewBattery.layoutParams
            pParams.width = iconWidth
            pParams.height = iconHeight
            binding.ivPreviewBattery.layoutParams = pParams
        }

        // Dynamic Battery Icon Live Update (Shape, Custom Icon, or Mascot Emoji)
        viewModel.selectedBatteryIconRes.observe(viewLifecycleOwner) { res ->
            // res 0 = a remote battery icon is picked, or a downloaded file is already applied
            val pendingRemote = viewModel.selectedRemoteBattery.value
            val remotePath = viewModel.preferenceRepository.getAppliedBatteryIconPath()
            if (res == 0 && pendingRemote != null) {
                RemoteImageLoader.loadImage(pendingRemote, binding.ivPreviewBattery)
            } else if (res == 0 && remotePath.isNotEmpty()) {
                // Clear the tag so a remote load still in flight doesn't overwrite this icon
                binding.ivPreviewBattery.tag = null
                binding.ivPreviewBattery.setImageBitmap(
                    RemoteImageLoader.decodeFileForDisplay(remotePath, 200, 200)
                )
            } else {
                binding.ivPreviewBattery.tag = null
                binding.ivPreviewBattery.setImageResource(res)
            }
            // binding.ivMiniBatteryIcon.setImageResource(res)
        }

        // Color Swatches
        viewModel.colorSwatches.observe(viewLifecycleOwner) { swatches ->
            colorSwatchAdapter.setItems(swatches, "pink")
        }

        // Battery Styles
        viewModel.batteryStyles.observe(viewLifecycleOwner) { styles ->
            batteryStyleAdapter.setItems(styles, viewModel.selectedBatteryStyle.value ?: "bolt")
        }

        // Mascot emojis
        viewModel.batteryMascots.observe(viewLifecycleOwner) { emojis ->
            mascotAdapter.submitList(emojis)
        }

        // Remote mascots, appended after the built-in ones (submitList clears them, so observe after)
        viewModel.remoteMascots.observe(viewLifecycleOwner) { remote ->
            mascotAdapter.submitRemoteList(remote)
        }

        // Status bar icons
        viewModel.statusIcons.observe(viewLifecycleOwner) { icons ->
            val enabled = viewModel.enabledCustomIcons.value ?: emptySet()
            customIconAdapter.setItems(icons, enabled)
        }

        // Multi-select custom status bar icons Live Update
        viewModel.enabledCustomIcons.observe(viewLifecycleOwner) { enabledSet ->
            customIconAdapter.setEnabledIds(enabledSet)
            updatePreviewAppearance()
        }

        // Left Status Emojis list
        viewModel.leftStatusEmojis.observe(viewLifecycleOwner) { emojis ->
            val selected = viewModel.selectedLeftEmojiRes.value ?: 0
            customLeftEmojiAdapter.setItems(emojis, selected, viewModel.selectedLeftEmojiRemoteId)
        }

        // Left Status Emoji selection Live Update (shows on left side with time)
        viewModel.selectedLeftEmojiRes.observe(viewLifecycleOwner) { emojiRes ->
            customLeftEmojiAdapter.setSelection(emojiRes, viewModel.selectedLeftEmojiRemoteId)
            updatePreviewAppearance()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshEnabledCustomIcons()
    }

    private fun updatePreviewAppearance() {
        if (_binding == null) return
        val mode = viewModel.lastAppliedThemeMode.value ?: "theme"
        val customIconColor = viewModel.iconColor.value ?: Color.WHITE
        val isBatteryTintable = viewModel.isBatteryIconTintable.value ?: true

        val effectiveColor: Int
        if (mode == "theme") {
            val themeRes = viewModel.appliedThemeDrawableRes.value ?: R.drawable.bg_theme_grad_pink
            val isGradient = viewModel.isAppliedThemeGradient.value ?: true

            val rectRes = StatusBarAccessibilityService.getOverlayRectDrawable(themeRes)
            binding.clStatusBarPreview.setBackgroundResource(rectRes)

            effectiveColor = if (isGradient) Color.WHITE else customIconColor
        } else {
            val bgColor = viewModel.backgroundColor.value ?: Color.parseColor("#F72585")
            binding.clStatusBarPreview.background = ColorDrawable(bgColor)
            effectiveColor = customIconColor
        }

        binding.tvPreviewTime.setTextColor(effectiveColor)
        binding.tvPreviewBattery.setTextColor(effectiveColor)

        if (isBatteryTintable) {
            binding.ivPreviewBattery.imageTintList = ColorStateList.valueOf(effectiveColor)
        } else {
            binding.ivPreviewBattery.imageTintList = null
        }

        updatePreviewCustomIcons(viewModel.enabledCustomIcons.value ?: emptySet(), effectiveColor)
    }

    private fun updatePreviewCustomIcons(enabledSet: Set<String>, effectiveColor: Int) {
        val density = resources.displayMetrics.density

        // Left Mascot Emoji (Beside time on left side)
        val leftEmojiRes = viewModel.selectedLeftEmojiRes.value.let {
            if (it != null && it != 0) it
            else viewModel.preferenceRepository.getLeftStatusEmojiRes().let { saved ->
                if (saved != 0) saved
                else if (viewModel.lastAppliedThemeMode.value != "custom" && !viewModel.preferenceRepository.isAppliedAnimationLottie()) {
                    viewModel.preferenceRepository.getAppliedMascotDrawableRes()
                } else 0
            }
        }
        // A remote left emoji: picked now (from cache/network) or applied earlier (saved file)
        val remoteLeftEmoji = viewModel.selectedRemoteLeftEmoji
        val remoteLeftEmojiPath = viewModel.leftEmojiPath
            .takeIf { it.isNotEmpty() && (viewModel.selectedLeftEmojiRes.value ?: 0) == 0 }
        if (remoteLeftEmoji != null || remoteLeftEmojiPath != null) {
            binding.ivPreviewLeftEmoji.visibility = View.VISIBLE
            binding.ivPreviewLeftEmoji.imageTintList = null
            val emojiSize = (16 * density).toInt()
            val lp = binding.ivPreviewLeftEmoji.layoutParams
            lp.width = (emojiSize * 1.35f).toInt()
            lp.height = emojiSize
            binding.ivPreviewLeftEmoji.layoutParams = lp
            if (remoteLeftEmoji != null) {
                RemoteImageLoader.loadImage(remoteLeftEmoji, binding.ivPreviewLeftEmoji)
            } else if (remoteLeftEmojiPath != null) {
                binding.ivPreviewLeftEmoji.tag = null
                binding.ivPreviewLeftEmoji.setImageBitmap(
                    RemoteImageLoader.decodeFileForDisplay(remoteLeftEmojiPath, lp.width, lp.height)
                )
            }
        } else if (leftEmojiRes != 0) {
            binding.ivPreviewLeftEmoji.tag = null
            binding.ivPreviewLeftEmoji.visibility = View.VISIBLE
            binding.ivPreviewLeftEmoji.setImageResource(leftEmojiRes)
            binding.ivPreviewLeftEmoji.imageTintList = null
            val emojiSize = (16 * density).toInt()
            val lp = binding.ivPreviewLeftEmoji.layoutParams
            lp.width = (emojiSize * 1.35f).toInt()
            lp.height = emojiSize
            binding.ivPreviewLeftEmoji.layoutParams = lp
        } else {
            binding.ivPreviewLeftEmoji.visibility = View.GONE
        }

        // Date
        if (enabledSet.contains("calendar")) {
            binding.tvPreviewDate.visibility = View.VISIBLE
            val format = viewModel.preferenceRepository.getDateFormat()
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                binding.tvPreviewDate.text = sdf.format(java.util.Date())
            } catch (e: Exception) {
                binding.tvPreviewDate.text = "Wed, Oct 07"
            }
            binding.tvPreviewDate.setTextColor(viewModel.preferenceRepository.getCustomIconColor("calendar"))
            val dtSize = viewModel.preferenceRepository.getCustomIconSize("calendar")
            binding.tvPreviewDate.textSize = (dtSize * 0.55f).coerceIn(8f, 16f)
        } else {
            binding.tvPreviewDate.visibility = View.GONE
        }

        // Airplane
        if (enabledSet.contains("airplane")) {
            binding.ivPreviewAirplane.visibility = View.VISIBLE
            binding.ivPreviewAirplane.imageTintList = ColorStateList.valueOf(viewModel.preferenceRepository.getCustomIconColor("airplane"))
            val s = (viewModel.preferenceRepository.getCustomIconSize("airplane") * density).toInt()
            binding.ivPreviewAirplane.layoutParams.width = s
            binding.ivPreviewAirplane.layoutParams.height = s
        } else {
            binding.ivPreviewAirplane.visibility = View.GONE
        }

        // Ringer
        if (enabledSet.contains("ringer")) {
            binding.ivPreviewRinger.visibility = View.VISIBLE
            binding.ivPreviewRinger.imageTintList = ColorStateList.valueOf(viewModel.preferenceRepository.getCustomIconColor("ringer"))
            val s = (viewModel.preferenceRepository.getCustomIconSize("ringer") * density).toInt()
            binding.ivPreviewRinger.layoutParams.width = s
            binding.ivPreviewRinger.layoutParams.height = s
        } else {
            binding.ivPreviewRinger.visibility = View.GONE
        }

        // Hotspot
        if (enabledSet.contains("hotspot")) {
            binding.ivPreviewHotspot.visibility = View.VISIBLE
            binding.ivPreviewHotspot.imageTintList = ColorStateList.valueOf(viewModel.preferenceRepository.getCustomIconColor("hotspot"))
            val s = (viewModel.preferenceRepository.getCustomIconSize("hotspot") * density).toInt()
            binding.ivPreviewHotspot.layoutParams.width = s
            binding.ivPreviewHotspot.layoutParams.height = s
        } else {
            binding.ivPreviewHotspot.visibility = View.GONE
        }

        // Emotion
        if (enabledSet.contains("emotion")) {
            binding.ivPreviewEmotion.visibility = View.VISIBLE
            val emotionStyle = viewModel.preferenceRepository.getEmotionStyle()
            val emotionRes = when (emotionStyle) {
                "sad" -> R.drawable.ic_reaction_sad
                "happy" -> R.drawable.ic_reaction_happy
                else -> R.drawable.ic_reaction_love
            }
            val s = (viewModel.preferenceRepository.getCustomIconSize("emotion") * density).toInt()
            val remoteEmotionPath = viewModel.preferenceRepository.getStatusBarSettings().remoteEmotionPath
            if (remoteEmotionPath != null) {
                binding.ivPreviewEmotion.setImageBitmap(RemoteImageLoader.decodeFileForDisplay(remoteEmotionPath, s, s))
            } else {
                binding.ivPreviewEmotion.setImageResource(emotionRes)
            }
            binding.ivPreviewEmotion.layoutParams.width = s
            binding.ivPreviewEmotion.layoutParams.height = s
        } else {
            binding.ivPreviewEmotion.visibility = View.GONE
        }

        // Animation Sparkle - Do not show redundant sparkle icon
        binding.ivPreviewAnimation.visibility = View.GONE

        // Left Animation Preview (Sub se last on left side, towards center notch)
        val isAnimEnabled = viewModel.preferenceRepository.isEmojiBatteryEnabled() &&
                (!viewModel.preferenceRepository.getStatusBarSettings().iconSizes.containsKey("animation") || enabledSet.contains("animation"))
        val animSize = com.jhw.batteryemoji.domain.model.StatusBarSettings.resolveAnimationSizeDp(
            viewModel.preferenceRepository.getCustomIconSize("animation"),
            viewModel.preferenceRepository.getMascotSize()
        )
        val animSizePx = (animSize * density).toInt()
        val appliedAnimRes = viewModel.preferenceRepository.getAppliedAnimationRawRes()
        val isAppliedLottie = viewModel.preferenceRepository.isAppliedAnimationLottie()

        val remoteAnimPath = viewModel.preferenceRepository.getStatusBarSettings().remoteAnimationPath
        if (isAnimEnabled && remoteAnimPath != null) {
            binding.lottiePreviewLeftAnimation.visibility = View.VISIBLE
            val lp = binding.lottiePreviewLeftAnimation.layoutParams
            lp.width = (animSizePx * 1.35f).toInt()
            lp.height = animSizePx
            binding.lottiePreviewLeftAnimation.layoutParams = lp
            RemoteImageLoader.bindAnimationFile(
                binding.lottiePreviewLeftAnimation, remoteAnimPath, lp.width, lp.height
            )
        } else if (isAnimEnabled && isAppliedLottie && appliedAnimRes != 0) {
            binding.lottiePreviewLeftAnimation.visibility = View.VISIBLE
            val lp = binding.lottiePreviewLeftAnimation.layoutParams
            lp.width = (animSizePx * 1.35f).toInt()
            lp.height = animSizePx
            binding.lottiePreviewLeftAnimation.layoutParams = lp
            val cur = binding.lottiePreviewLeftAnimation.tag as? Int
            if (cur != appliedAnimRes) {
                binding.lottiePreviewLeftAnimation.tag = appliedAnimRes
                binding.lottiePreviewLeftAnimation.setAnimation(appliedAnimRes)
                binding.lottiePreviewLeftAnimation.playAnimation()
            } else if (!binding.lottiePreviewLeftAnimation.isAnimating) {
                binding.lottiePreviewLeftAnimation.playAnimation()
            }
        } else {
            binding.lottiePreviewLeftAnimation.visibility = View.GONE
            binding.lottiePreviewLeftAnimation.cancelAnimation()
            binding.lottiePreviewLeftAnimation.tag = null
        }

        // Data
        if (enabledSet.contains("data")) {
            binding.ivPreviewData.visibility = View.VISIBLE
            binding.ivPreviewData.imageTintList = ColorStateList.valueOf(viewModel.preferenceRepository.getCustomIconColor("data"))
            val s = (viewModel.preferenceRepository.getCustomIconSize("data") * density).toInt()
            binding.ivPreviewData.layoutParams.width = s
            binding.ivPreviewData.layoutParams.height = s
        } else {
            binding.ivPreviewData.visibility = View.GONE
        }

        // Signal / SIM
        val isSimEnabled = enabledSet.contains("signal") || enabledSet.contains("sim")
        val signalColor = if (isSimEnabled) {
            viewModel.preferenceRepository.getCustomIconColor("signal")
        } else {
            effectiveColor
        }
        binding.ivPreviewSignal.imageTintList = ColorStateList.valueOf(signalColor)
        binding.ivPreviewSignal.visibility = View.VISIBLE
        if (isSimEnabled) {
            val s = (viewModel.preferenceRepository.getCustomIconSize("signal") * density).toInt()
            binding.ivPreviewSignal.layoutParams.width = s
            binding.ivPreviewSignal.layoutParams.height = s
        }

        // Wifi
        val wifiColor = if (enabledSet.contains("wifi")) viewModel.preferenceRepository.getCustomIconColor("wifi") else effectiveColor
        binding.ivPreviewWifi.imageTintList = ColorStateList.valueOf(wifiColor)
        if (enabledSet.contains("wifi")) {
            val s = (viewModel.preferenceRepository.getCustomIconSize("wifi") * density).toInt()
            binding.ivPreviewWifi.layoutParams.width = s
            binding.ivPreviewWifi.layoutParams.height = s
        }

        // Charge Bolt
        val chargeColor = if (enabledSet.contains("charge")) viewModel.preferenceRepository.getCustomIconColor("charge") else effectiveColor
        binding.ivPreviewBolt.imageTintList = ColorStateList.valueOf(chargeColor)
        if (enabledSet.contains("charge")) {
            binding.ivPreviewBolt.visibility = View.VISIBLE
            val s = (viewModel.preferenceRepository.getCustomIconSize("charge") * density).toInt()
            binding.ivPreviewBolt.layoutParams.width = s
            binding.ivPreviewBolt.layoutParams.height = s
        }
    }

    private fun renderTab(tab: StatusBarCustomViewModel.CustomTab) {
        val purple = ContextCompat.getColor(requireContext(), R.color.primary_purple)
        val muted = ContextCompat.getColor(requireContext(), R.color.tab_inactive)

        // Reset indicators
        binding.vTabSizeIndicator.visibility = View.INVISIBLE
        binding.vTabAppearanceIndicator.visibility = View.INVISIBLE
        binding.vTabBatteryIndicator.visibility = View.INVISIBLE
        binding.vTabCustomizeIconIndicator.visibility = View.INVISIBLE

        binding.tvTabSize.setTextColor(muted)
        binding.tvTabAppearance.setTextColor(muted)
        binding.tvTabBattery.setTextColor(muted)
        binding.tvTabCustomizeIcon.setTextColor(muted)

        // Hide all panels
        binding.clSizePanel.visibility = View.GONE
        binding.clAppearancePanel.visibility = View.GONE
        binding.clBatteryPanel.visibility = View.GONE
        binding.clCustomizeIconPanel.visibility = View.GONE

        when (tab) {
            StatusBarCustomViewModel.CustomTab.SIZE -> {
                binding.tvTabSize.setTextColor(purple)
                binding.vTabSizeIndicator.visibility = View.VISIBLE
                binding.clSizePanel.visibility = View.VISIBLE
            }
            StatusBarCustomViewModel.CustomTab.APPEARANCE -> {
                binding.tvTabAppearance.setTextColor(purple)
                binding.vTabAppearanceIndicator.visibility = View.VISIBLE
                binding.clAppearancePanel.visibility = View.VISIBLE
            }
            StatusBarCustomViewModel.CustomTab.BATTERY -> {
                binding.tvTabBattery.setTextColor(purple)
                binding.vTabBatteryIndicator.visibility = View.VISIBLE
                binding.clBatteryPanel.visibility = View.VISIBLE
            }
            StatusBarCustomViewModel.CustomTab.CUSTOMIZE_ICON -> {
                binding.tvTabCustomizeIcon.setTextColor(purple)
                binding.vTabCustomizeIconIndicator.visibility = View.VISIBLE
                binding.clCustomizeIconPanel.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        binding.lottiePreviewLeftAnimation.cancelAnimation()
        super.onDestroyView()
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
