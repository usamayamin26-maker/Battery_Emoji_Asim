package com.asim.jhbatteryapp.presentation.custom

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
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.FragmentStatusBarCustomBinding
import com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper
import com.asim.jhbatteryapp.presentation.home.BatteryEmojiAdapter
import com.asim.jhbatteryapp.service.StatusBarAccessibilityService
import com.asim.jhbatteryapp.service.StatusBarOverlayService

class StatusBarCustomFragment : Fragment() {

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

        mascotAdapter = BatteryEmojiAdapter { emojiItem ->
            viewModel.selectMascot(emojiItem)
        }
        binding.rvBatteryMascots.adapter = mascotAdapter

        customIconAdapter = CustomStatusIconAdapter { iconItem ->
            val bundle = Bundle().apply {
                putString("iconId", iconItem.id)
            }
            findNavController().navigate(
                R.id.action_statusBarCustomFragment_to_customIconDetailFragment,
                bundle
            )
        }
        binding.rvCustomizeIcons.adapter = customIconAdapter

        // Emojis below custom icons (shows on left side with time in status bar)
        customLeftEmojiAdapter = CustomStatusEmojiAdapter { emojiItem ->
            viewModel.selectLeftEmoji(emojiItem)
            val res = if (emojiItem.isNone) 0 else emojiItem.drawableRes
            StatusBarAccessibilityService.updateLeftEmoji(res)
        }
        binding.rvCustomLeftEmojis.adapter = customLeftEmojiAdapter
    }

    private fun setupTabListeners() {
        binding.ivDetailBack.setOnClickListener {
            findNavController().navigateUp()
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
        val muted = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        binding.btnBatteryTabTrending.setTextColor(if (category == "trending") purple else muted)
        binding.btnBatteryTabHalloween.setTextColor(if (category == "halloween") purple else muted)
        binding.btnBatteryTabWC2.setTextColor(if (category == "wc2") purple else muted)
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
    }

    private fun setupApplyButton() {
        binding.btnApplyCustom.setOnClickListener {
            val hasAccessibility = StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())

            if (!hasAccessibility) {
                StatusBarPermissionHelper.showAccessibilityDialog(requireContext())
                return@setOnClickListener
            }

            viewModel.applyCustomizations()
            StatusBarAccessibilityService.updateCustomSettings()

            Toast.makeText(
                requireContext(),
                getString(R.string.apply_custom_success),
                Toast.LENGTH_SHORT
            ).show()
        }
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
                binding.btnSubTabBackground.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_purple))
            } else {
                binding.btnSubTabBackground.setBackgroundResource(R.drawable.bg_tab_pill_active)
                binding.btnSubTabBackground.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.btnSubTabIcon.setBackgroundResource(R.drawable.bg_tab_pill_inactive)
                binding.btnSubTabIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_purple))
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

        // Background Color Live Update
        viewModel.backgroundColor.observe(viewLifecycleOwner) { color ->
            binding.clStatusBarPreview.background = ColorDrawable(color)
        }

        // Icon & Content Color Live Update
        viewModel.iconColor.observe(viewLifecycleOwner) { color ->
            binding.tvPreviewTime.setTextColor(color)
            binding.tvPreviewBattery.setTextColor(color)
            binding.ivPreviewSignal.imageTintList = ColorStateList.valueOf(color)
            binding.ivPreviewWifi.imageTintList = ColorStateList.valueOf(color)
            binding.ivPreviewBolt.imageTintList = ColorStateList.valueOf(color)

            // Battery icon tint depends on whether it's tintable
            if (viewModel.isBatteryIconTintable.value == true) {
                binding.ivPreviewBattery.imageTintList = ColorStateList.valueOf(color)
            } else {
                binding.ivPreviewBattery.imageTintList = null
            }
        }

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

//        // Dynamic Battery Icon Live Update (Shape, Custom Icon, or Mascot Emoji)
//        viewModel.selectedBatteryIconRes.observe(viewLifecycleOwner) { res ->
//            binding.ivPreviewBattery.setImageResource(res)
//            binding.ivMiniBatteryIcon.setImageResource(res)
//        }

//        viewModel.isBatteryIconTintable.observe(viewLifecycleOwner) { isTintable ->
//            val color = viewModel.iconColor.value ?: Color.WHITE
//            if (isTintable) {
//                binding.ivPreviewBattery.imageTintList = ColorStateList.valueOf(color)
//            } else {
//                binding.ivPreviewBattery.imageTintList = null
//            }
//        }

        // Color Swatches
        viewModel.colorSwatches.observe(viewLifecycleOwner) { swatches ->
            colorSwatchAdapter.setItems(swatches, "pink")
        }

        // Battery Styles
        viewModel.batteryStyles.observe(viewLifecycleOwner) { styles ->
            batteryStyleAdapter.setItems(styles, "bolt")
        }

        // Mascot emojis
        viewModel.batteryMascots.observe(viewLifecycleOwner) { emojis ->
            mascotAdapter.submitList(emojis)
        }

        // Status bar icons
        viewModel.statusIcons.observe(viewLifecycleOwner) { icons ->
            val enabled = viewModel.enabledCustomIcons.value ?: emptySet()
            customIconAdapter.setItems(icons, enabled)
        }

        // Multi-select custom status bar icons Live Update
        viewModel.enabledCustomIcons.observe(viewLifecycleOwner) { enabledSet ->
            customIconAdapter.setEnabledIds(enabledSet)
            updatePreviewCustomIcons(enabledSet)
        }

        // Left Status Emojis list
        viewModel.leftStatusEmojis.observe(viewLifecycleOwner) { emojis ->
            val selected = viewModel.selectedLeftEmojiRes.value ?: 0
            customLeftEmojiAdapter.setItems(emojis, selected)
        }

        // Left Status Emoji selection Live Update (shows on left side with time)
        viewModel.selectedLeftEmojiRes.observe(viewLifecycleOwner) { emojiRes ->
            customLeftEmojiAdapter.setSelectedRes(emojiRes)
            if (emojiRes != 0) {
                binding.ivPreviewLeftEmoji.visibility = View.VISIBLE
                binding.ivPreviewLeftEmoji.setImageResource(emojiRes)
                binding.ivPreviewLeftEmoji.imageTintList = null
            } else {
                binding.ivPreviewLeftEmoji.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshEnabledCustomIcons()
    }

    private fun updatePreviewCustomIcons(enabledSet: Set<String>) {
        val density = resources.displayMetrics.density

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
            binding.ivPreviewEmotion.setImageResource(emotionRes)
            val s = (viewModel.preferenceRepository.getCustomIconSize("emotion") * density).toInt()
            binding.ivPreviewEmotion.layoutParams.width = s
            binding.ivPreviewEmotion.layoutParams.height = s
        } else {
            binding.ivPreviewEmotion.visibility = View.GONE
        }

        // Animation Sparkle
        if (enabledSet.contains("animation")) {
            binding.ivPreviewAnimation.visibility = View.VISIBLE
            binding.ivPreviewAnimation.imageTintList = ColorStateList.valueOf(viewModel.preferenceRepository.getCustomIconColor("animation"))
            val s = (viewModel.preferenceRepository.getCustomIconSize("animation") * density).toInt()
            binding.ivPreviewAnimation.layoutParams.width = s
            binding.ivPreviewAnimation.layoutParams.height = s
        } else {
            binding.ivPreviewAnimation.visibility = View.GONE
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
            (viewModel.iconColor.value ?: Color.WHITE)
        }
        binding.ivPreviewSignal.imageTintList = ColorStateList.valueOf(signalColor)
        binding.ivPreviewSignal.visibility = View.VISIBLE
        if (isSimEnabled) {
            val s = (viewModel.preferenceRepository.getCustomIconSize("signal") * density).toInt()
            binding.ivPreviewSignal.layoutParams.width = s
            binding.ivPreviewSignal.layoutParams.height = s
        }

        // Wifi
        val wifiColor = if (enabledSet.contains("wifi")) viewModel.preferenceRepository.getCustomIconColor("wifi") else (viewModel.iconColor.value ?: Color.WHITE)
        binding.ivPreviewWifi.imageTintList = ColorStateList.valueOf(wifiColor)
        if (enabledSet.contains("wifi")) {
            val s = (viewModel.preferenceRepository.getCustomIconSize("wifi") * density).toInt()
            binding.ivPreviewWifi.layoutParams.width = s
            binding.ivPreviewWifi.layoutParams.height = s
        }

        // Charge Bolt
        val chargeColor = if (enabledSet.contains("charge")) viewModel.preferenceRepository.getCustomIconColor("charge") else (viewModel.iconColor.value ?: Color.WHITE)
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
        val muted = ContextCompat.getColor(requireContext(), R.color.text_secondary)

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
        super.onDestroyView()
        _binding = null
    }
}
