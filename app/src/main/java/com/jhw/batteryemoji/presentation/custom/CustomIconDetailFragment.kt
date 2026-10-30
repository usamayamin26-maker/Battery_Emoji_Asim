package com.jhw.batteryemoji.presentation.custom

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.core.Utils.AssetConfig
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.data.repository.PreferenceRepositoryImpl
import com.jhw.batteryemoji.data.repository.RemoteAssetRepositoryImpl
import com.jhw.batteryemoji.databinding.FragmentCustomIconDetailBinding
import com.jhw.batteryemoji.domain.model.AssetManifest
import com.jhw.batteryemoji.domain.model.BatteryEmojiItem
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import com.jhw.batteryemoji.domain.model.StatusBarSettings
import com.jhw.batteryemoji.domain.repository.PreferenceRepository
import com.jhw.batteryemoji.presentation.animation.StatusBarAnimationAdapter
import com.jhw.batteryemoji.presentation.animation.StatusBarAnimationViewModel
import com.jhw.batteryemoji.service.StatusBarAccessibilityService
import com.universal.remote.ads.AdsController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomIconDetailFragment : Fragment() {

    companion object {
        private const val SCREEN_KEY = "custom_detail"
    }

    private var _binding: FragmentCustomIconDetailBinding? = null
    private val binding get() = _binding!!

    private val preferenceRepository: PreferenceRepository = PreferenceRepositoryImpl()

    private var iconId: String = "airplane"
    private var selectedColor: Int = Color.parseColor("#9333EA")
    private var selectedSizeDp: Int = 20
    private var isEnabled: Boolean = true
    private var selectedEmotion: String = "love"

    /** Remote emotion picked but not applied yet; downloaded on Apply. */
    private var selectedRemoteEmotion: RemoteAssetItem? = null
    private var remoteEmotionAdapter: CustomStatusEmojiAdapter? = null
    private var selectedDateFormat: String = "EEE, MMM dd"
    private var selectedDateStyle: Int = 0

    private lateinit var colorAdapter: ColorSwatchAdapter

    // Animation picker (iconId == "animation"): same list as the Animation screen, applied on Apply
    private val animationViewModel: StatusBarAnimationViewModel by activityViewModels()
    private var animationAdapter: StatusBarAnimationAdapter? = null
    private var selectedLocalAnimation: BatteryEmojiItem? = null
    private var selectedRemoteAnimation: RemoteAssetItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iconId = arguments?.getString("iconId") ?: "airplane"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomIconDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadInitialValues()
        setupTopBar()
        setupSizeSlider()
        setupColorSwatches()
        setupSpecializedSections()
        updateLivePreview()
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

    private fun loadInitialValues() {
        val currentlyEnabled = preferenceRepository.isCustomIconEnabled(iconId) ||
                (iconId == "signal" && preferenceRepository.isCustomIconEnabled("sim")) ||
                (iconId == "sim" && preferenceRepository.isCustomIconEnabled("signal"))
        isEnabled = currentlyEnabled
        if (iconId == "animation") {
            val mSize = preferenceRepository.getMascotSize()
            val cSize = preferenceRepository.getCustomIconSize("animation")
            selectedSizeDp = StatusBarSettings.resolveAnimationSizeDp(cSize, mSize)
        } else {
            selectedSizeDp = preferenceRepository.getCustomIconSize(iconId)
            if (selectedSizeDp < 10) selectedSizeDp = 20
        }
        val purpleColor = ContextCompat.getColor(requireContext(), R.color.primary_purple)
        val savedColor = preferenceRepository.getCustomIconColor(iconId)
        selectedColor = if (savedColor == 0 || savedColor == Color.WHITE) {
            purpleColor
        } else {
            savedColor
        }
        selectedEmotion = preferenceRepository.getEmotionStyle()
        selectedDateFormat = preferenceRepository.getDateFormat()
        selectedDateStyle = preferenceRepository.getDateStyle()
    }

    private fun setupTopBar() {
        val title = when (iconId) {
            "airplane" -> getString(R.string.icon_airplane)
            "ringer" -> getString(R.string.icon_ringer)
            "calendar" -> getString(R.string.icon_date_time)
            "hotspot" -> getString(R.string.icon_hotspot)
            "emotion" -> getString(R.string.icon_emotion)
            "wifi" -> getString(R.string.icon_wifi)
            "signal", "sim" -> getString(R.string.icon_sim_signal)
            "data" -> getString(R.string.icon_data)
            "animation" -> getString(R.string.animation)
            "charge" -> getString(R.string.icon_charge)
            else -> iconId.replaceFirstChar { it.uppercase() }
        }

        binding.topHeader.title = title
        binding.tvDetailSizeLabel.text = getString(R.string.item_size_format, title)

        AdsController.routeSystemBack(this, binding.topHeader.backButton)
        binding.topHeader.backButton.setOnClickListener {
            AdsController.back(this, "is_show_custom_detail_back_inter") {
                findNavController().popBackStack()
            }
        }

        binding.topHeader.actionButton.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_custom_detail_btn_inter") {
                saveAndApply()
            }
        }

        binding.btnApplyDetail.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_custom_detail_btn_inter") {
                saveAndApply()
            }
        }

        binding.swDetailEnable.isChecked = isEnabled
        binding.swDetailEnable.setOnCheckedChangeListener { _, checked ->
            isEnabled = checked
        }
    }

    private fun setupSizeSlider() {
        binding.sbDetailSize.max = if (iconId == "animation") StatusBarSettings.MAX_ANIMATION_SIZE_DP else 36
        binding.sbDetailSize.progress = selectedSizeDp
        binding.tvDetailSizeValue.text = "${selectedSizeDp}dp"

        binding.sbDetailSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = if (progress < 10) 10 else progress
                selectedSizeDp = clamped
                binding.tvDetailSizeValue.text = "${clamped}dp"
                updateLivePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupColorSwatches() {
        val swatches = listOf(
            ColorSwatchItem("eyedropper", 0, isEyedropper = true),
            ColorSwatchItem("black", Color.parseColor("#181926")),
            ColorSwatchItem("white", Color.WHITE),
            ColorSwatchItem("purple", Color.parseColor("#9333EA")),
            ColorSwatchItem("blue", Color.parseColor("#2A86FF")),
            ColorSwatchItem("pink", Color.parseColor("#F72585")),
            ColorSwatchItem("green", Color.parseColor("#38D39F")),
            ColorSwatchItem("orange", Color.parseColor("#FF7A00"))
        )

        colorAdapter = ColorSwatchAdapter { swatch ->
            if (swatch.isEyedropper) {
                selectedColor = Color.parseColor("#9333EA")
            } else {
                selectedColor = swatch.colorInt
            }
            updateLivePreview()
        }

        binding.rvDetailColorSwatches.adapter = colorAdapter

        val initialSwatchId = swatches.firstOrNull { it.colorInt == selectedColor }?.id ?: "purple"
        colorAdapter.setItems(swatches, initialSwatchId)
    }

    private fun setupSpecializedSections() {
        if (iconId == "emotion") {
            binding.clDetailEmotionSection.visibility = View.VISIBLE
            highlightSelectedEmotion(selectedEmotion)

            setupRemoteEmotions()

            binding.cardEmotionLove.setOnClickListener {
                selectedEmotion = "love"
                highlightSelectedEmotion("love")
                updateLivePreview()
            }
            binding.cardEmotionSad.setOnClickListener {
                selectedEmotion = "sad"
                highlightSelectedEmotion("sad")
                updateLivePreview()
            }
            binding.cardEmotionHappy.setOnClickListener {
                selectedEmotion = "happy"
                highlightSelectedEmotion("happy")
                updateLivePreview()
            }
        } else {
            binding.clDetailEmotionSection.visibility = View.GONE
        }

        if (iconId == "calendar") {
            binding.clDetailDateSection.visibility = View.VISIBLE

            val now = Date()
            val f1 = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(now)
            val f2 = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(now)
            val f3 = SimpleDateFormat("dd MMM", Locale.getDefault()).format(now)

            binding.tvDateFormatText1.text = f1
            binding.tvDateFormatText2.text = f2
            binding.tvDateFormatText3.text = f3

            binding.tvDateStylePreview1.text = f1
            binding.tvDateStylePreview2.text = f1
            binding.tvDateStylePreview3.text = f1

            highlightSelectedDateFormat(selectedDateFormat)
            highlightSelectedDateStyle(selectedDateStyle)

            binding.clDateFormatOpt1.setOnClickListener {
                selectedDateFormat = "EEE, MMM dd"
                highlightSelectedDateFormat(selectedDateFormat)
                updateLivePreview()
            }
            binding.clDateFormatOpt2.setOnClickListener {
                selectedDateFormat = "MM/dd/yyyy"
                highlightSelectedDateFormat(selectedDateFormat)
                updateLivePreview()
            }
            binding.clDateFormatOpt3.setOnClickListener {
                selectedDateFormat = "dd MMM"
                highlightSelectedDateFormat(selectedDateFormat)
                updateLivePreview()
            }

            binding.clDateStyleOpt1.setOnClickListener {
                selectedDateStyle = 0
                highlightSelectedDateStyle(0)
            }
            binding.clDateStyleOpt2.setOnClickListener {
                selectedDateStyle = 1
                highlightSelectedDateStyle(1)
            }
            binding.clDateStyleOpt3.setOnClickListener {
                selectedDateStyle = 2
                highlightSelectedDateStyle(2)
            }
        } else {
            binding.clDetailDateSection.visibility = View.GONE
        }

        if (iconId == "animation") {
            binding.clDetailColorSection.visibility = View.GONE
            binding.clDetailAnimationSection.visibility = View.VISIBLE
            setupAnimationPicker()
        } else {
            binding.clDetailColorSection.visibility = View.VISIBLE
            binding.clDetailAnimationSection.visibility = View.GONE
        }
    }

    /** Animation grid shared with the Animation screen; the applied animation starts highlighted. */
    private fun setupAnimationPicker() {
        val adapter = StatusBarAnimationAdapter(
            onRemoteItemClick = { remote ->
                selectedLocalAnimation = null
                selectedRemoteAnimation = remote
                animationAdapter?.setSelectedId(remote.id)
                updateLivePreview()
            }
        ) { item ->
            selectedRemoteAnimation = null
            selectedLocalAnimation = item
            animationAdapter?.setSelectedId(item.id)
            updateLivePreview()
        }
        animationAdapter = adapter
        binding.rvDetailAnimations.adapter = adapter

        animationViewModel.animations.observe(viewLifecycleOwner) { anims ->
            val appliedRes = preferenceRepository.getAppliedAnimationRawRes()
            val appliedId = anims.find { appliedRes != 0 && it.lottieRawRes == appliedRes }?.id ?: ""
            adapter.setItems(anims, selectedLocalAnimation?.id ?: selectedRemoteAnimation?.id ?: appliedId)
        }
        animationViewModel.remoteAnimations.observe(viewLifecycleOwner) { remote ->
            adapter.submitRemoteList(remote)
            if (selectedLocalAnimation == null && selectedRemoteAnimation == null) {
                // The applied remote file is named after its asset id
                preferenceRepository.getStatusBarSettings().remoteAnimationPath?.let { path ->
                    val appliedId = java.io.File(path).nameWithoutExtension
                    if (remote.any { it.id == appliedId }) adapter.setSelectedId(appliedId)
                }
            }
        }
    }

    /** Remote emotions from the manifest, shown under the built-in three (cached manifest first). */
    private fun setupRemoteEmotions() {
        val adapter = CustomStatusEmojiAdapter { item ->
            val remote = item.remote ?: return@CustomStatusEmojiAdapter
            selectedRemoteEmotion = remote
            selectedEmotion = StatusBarSettings.EMOTION_STYLE_REMOTE
            highlightSelectedEmotion(selectedEmotion)
            updateLivePreview()
        }
        remoteEmotionAdapter = adapter
        binding.rvDetailRemoteEmotions.adapter = adapter

        val repository = RemoteAssetRepositoryImpl()
        viewLifecycleOwner.lifecycleScope.launch {
            repository.loadCachedManifest()?.let { showRemoteEmotions(it) }
            repository.fetchManifest().onSuccess { showRemoteEmotions(it) }
        }
    }

    private fun showRemoteEmotions(manifest: AssetManifest) {
        val items = manifest.getByType(AssetConfig.Type.EMOTION)
            .map { CustomStatusEmojiItem("remote_${it.id}", it.name, 0, remote = it) }
        val b = _binding ?: return
        b.rvDetailRemoteEmotions.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        // res -1 matches no item, so only the picked remote emotion (if any) is highlighted
        remoteEmotionAdapter?.setItems(items, -1, selectedRemoteEmotion?.id)
    }

    private fun highlightSelectedEmotion(emotion: String) {
        if (emotion != StatusBarSettings.EMOTION_STYLE_REMOTE) {
            selectedRemoteEmotion = null
            remoteEmotionAdapter?.setSelection(-1, null)
        }
        val selectedBg = R.drawable.bg_custom_icon_item_selected
        val unselectedBg = R.drawable.bg_custom_panel_card

        binding.cardEmotionLove.setBackgroundResource(if (emotion == "love") selectedBg else unselectedBg)
        binding.cardEmotionSad.setBackgroundResource(if (emotion == "sad") selectedBg else unselectedBg)
        binding.cardEmotionHappy.setBackgroundResource(if (emotion == "happy") selectedBg else unselectedBg)
    }

    private fun highlightSelectedDateFormat(format: String) {
        binding.ivRadioDate1.visibility = if (format == "EEE, MMM dd") View.VISIBLE else View.GONE
        binding.ivRadioDate2.visibility = if (format == "MM/dd/yyyy") View.VISIBLE else View.GONE
        binding.ivRadioDate3.visibility = if (format == "dd MMM") View.VISIBLE else View.GONE
    }

    private fun highlightSelectedDateStyle(style: Int) {
        binding.ivRadioStyle1.visibility = if (style == 0) View.VISIBLE else View.GONE
        binding.ivRadioStyle2.visibility = if (style == 1) View.VISIBLE else View.GONE
        binding.ivRadioStyle3.visibility = if (style == 2) View.VISIBLE else View.GONE
    }

    private fun updateLivePreview() {
        val density = resources.displayMetrics.density
        val sizePx = (selectedSizeDp * density * 1.4f).toInt()

        if (iconId == "calendar") {
            binding.ivDetailIconPreview.visibility = View.GONE
            binding.lottieDetailAnimationPreview.visibility = View.GONE
            binding.tvDetailDatePreview.visibility = View.VISIBLE
            try {
                val sdf = SimpleDateFormat(selectedDateFormat, Locale.getDefault())
                binding.tvDetailDatePreview.text = sdf.format(Date())
            } catch (e: Exception) {
                binding.tvDetailDatePreview.text = "Wed, Oct 07"
            }
            binding.tvDetailDatePreview.textSize = (selectedSizeDp * 0.8f).coerceIn(10f, 24f)
            binding.tvDetailDatePreview.setTextColor(selectedColor)
            return
        }

        if (iconId == "animation") {
            binding.tvDetailDatePreview.visibility = View.GONE
            val pendingRemote = selectedRemoteAnimation
            val pendingLocal = selectedLocalAnimation
            if (pendingRemote != null || (pendingLocal != null && pendingLocal.isLottie && pendingLocal.lottieRawRes != 0)) {
                binding.ivDetailIconPreview.visibility = View.GONE
                binding.lottieDetailAnimationPreview.visibility = View.VISIBLE
                val p = binding.lottieDetailAnimationPreview.layoutParams
                p.width = (sizePx * 1.35f).toInt()
                p.height = sizePx
                binding.lottieDetailAnimationPreview.layoutParams = p
                if (pendingRemote != null) {
                    RemoteImageLoader.loadLottie(pendingRemote, binding.lottieDetailAnimationPreview)
                } else if (pendingLocal != null && binding.lottieDetailAnimationPreview.tag != pendingLocal.lottieRawRes) {
                    binding.lottieDetailAnimationPreview.tag = pendingLocal.lottieRawRes
                    binding.lottieDetailAnimationPreview.setAnimation(pendingLocal.lottieRawRes)
                    binding.lottieDetailAnimationPreview.playAnimation()
                }
                return
            }
            if (pendingLocal != null) {
                binding.lottieDetailAnimationPreview.visibility = View.GONE
                binding.ivDetailIconPreview.visibility = View.VISIBLE
                val p = binding.ivDetailIconPreview.layoutParams
                p.width = (sizePx * 1.35f).toInt()
                p.height = sizePx
                binding.ivDetailIconPreview.layoutParams = p
                binding.ivDetailIconPreview.setImageResource(pendingLocal.previewDrawableRes)
                binding.ivDetailIconPreview.imageTintList = null
                return
            }
            val isLottie = preferenceRepository.isAppliedAnimationLottie()
            val animRes = preferenceRepository.getAppliedAnimationRawRes()
            val effectiveAnimRes = if (animRes != 0) animRes else R.raw.rabit

            if (isLottie || animRes != 0) {
                binding.ivDetailIconPreview.visibility = View.GONE
                binding.lottieDetailAnimationPreview.visibility = View.VISIBLE
                val p = binding.lottieDetailAnimationPreview.layoutParams
                p.width = (sizePx * 1.35f).toInt()
                p.height = sizePx
                binding.lottieDetailAnimationPreview.layoutParams = p

                val currentRes = binding.lottieDetailAnimationPreview.tag as? Int
                if (currentRes != effectiveAnimRes) {
                    binding.lottieDetailAnimationPreview.tag = effectiveAnimRes
                    binding.lottieDetailAnimationPreview.setAnimation(effectiveAnimRes)
                    binding.lottieDetailAnimationPreview.playAnimation()
                } else if (!binding.lottieDetailAnimationPreview.isAnimating) {
                    binding.lottieDetailAnimationPreview.playAnimation()
                }
            } else {
                binding.lottieDetailAnimationPreview.visibility = View.GONE
                binding.ivDetailIconPreview.visibility = View.VISIBLE
                val mascotRes = preferenceRepository.getAppliedMascotDrawableRes().let {
                    if (it != 0) it else R.drawable.ic_blueprint_girl_pink
                }
                val p = binding.ivDetailIconPreview.layoutParams
                p.width = (sizePx * 1.35f).toInt()
                p.height = sizePx
                binding.ivDetailIconPreview.layoutParams = p
                binding.ivDetailIconPreview.setImageResource(mascotRes)
                binding.ivDetailIconPreview.imageTintList = null
            }
            return
        }

        binding.lottieDetailAnimationPreview.visibility = View.GONE
        binding.tvDetailDatePreview.visibility = View.GONE
        binding.ivDetailIconPreview.visibility = View.VISIBLE

        val p = binding.ivDetailIconPreview.layoutParams
        p.width = sizePx
        p.height = sizePx
        binding.ivDetailIconPreview.layoutParams = p

        if (iconId == "emotion") {
            val res = when (selectedEmotion) {
                "sad" -> R.drawable.ic_reaction_sad
                "happy" -> R.drawable.ic_reaction_happy
                else -> R.drawable.ic_reaction_love
            }
            val pendingRemote = selectedRemoteEmotion
            val appliedRemotePath = preferenceRepository.getEmotionPath()
                .takeIf { selectedEmotion == StatusBarSettings.EMOTION_STYLE_REMOTE && it.isNotEmpty() }
            if (pendingRemote != null) {
                RemoteImageLoader.loadImage(pendingRemote, binding.ivDetailIconPreview)
            } else if (appliedRemotePath != null) {
                binding.ivDetailIconPreview.tag = null
                binding.ivDetailIconPreview.setImageBitmap(
                    RemoteImageLoader.decodeFileForDisplay(appliedRemotePath, sizePx, sizePx)
                )
            } else {
                binding.ivDetailIconPreview.tag = null
                binding.ivDetailIconPreview.setImageResource(res)
            }
            binding.ivDetailIconPreview.imageTintList = null
        } else {
            val res = when (iconId) {
                "airplane" -> R.drawable.ic_airplane
                "ringer" -> R.drawable.ic_ringer_silent
                "hotspot" -> R.drawable.ic_hotspot
                "wifi" -> R.drawable.wifi
                "signal", "sim" -> R.drawable.ic_status_signal
                "data" -> R.drawable.ic_data_swap
                "charge" -> R.drawable.ic_status_bolt
                else -> R.drawable.ic_airplane
            }
            binding.ivDetailIconPreview.setImageResource(res)
            binding.ivDetailIconPreview.imageTintList = ColorStateList.valueOf(selectedColor)
        }
    }

    private fun saveAndApply() {
        if (iconId == "animation") {
            applySelectedAnimation()
            return
        }
        val remoteEmotion = selectedRemoteEmotion
        if (iconId != "emotion" || remoteEmotion == null) {
            save(remoteEmotionPath = null)
            return
        }
        if (!com.jhw.batteryemoji.core.Utils.MyUtils.isNetworkAvailable(requireContext())) {
            com.jhw.batteryemoji.core.Utils.MyUtils.showNoInternetDialog(
                context = requireContext(),
                onRetry = { saveAndApply() }
            )
            return
        }
        // A remote emotion is saved here (from the cache, downloading if needed) for the overlay
        val appContext = requireContext().applicationContext
        binding.btnApplyDetail.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val path = RemoteImageLoader.saveForApply(remoteEmotion)
            val b = _binding ?: return@launch
            b.btnApplyDetail.isEnabled = true
            if (path == null) {
                Toast.makeText(appContext, R.string.remote_asset_download_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            save(path)
        }
    }

    /** Applies the animation picked in the grid (if any), then saves size / enable like other icons. */
    private fun applySelectedAnimation() {
        selectedLocalAnimation?.let { local ->
            animationViewModel.applyAnimation(local)
            animationViewModel.selectAnimation(local)
            save(remoteEmotionPath = null)
            return
        }
        val remote = selectedRemoteAnimation
        if (remote == null) {
            save(remoteEmotionPath = null)
            return
        }
        if (!com.jhw.batteryemoji.core.Utils.MyUtils.isNetworkAvailable(requireContext())) {
            com.jhw.batteryemoji.core.Utils.MyUtils.showNoInternetDialog(
                context = requireContext(),
                onRetry = { applySelectedAnimation() }
            )
            return
        }
        val appContext = requireContext().applicationContext
        binding.btnApplyDetail.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val path = RemoteImageLoader.saveForApply(remote)
            val b = _binding ?: return@launch
            b.btnApplyDetail.isEnabled = true
            if (path == null) {
                Toast.makeText(appContext, R.string.remote_asset_download_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            animationViewModel.applyRemoteAnimation(path)
            animationViewModel.selectRemoteAnimation(remote)
            save(remoteEmotionPath = null)
        }
    }

    private fun save(remoteEmotionPath: String?) {
        val current = preferenceRepository.getStatusBarSettings()
        val targets = if (iconId == "signal" || iconId == "sim") setOf("signal", "sim") else setOf(iconId)
        val newEnabled = if (isEnabled) {
            current.enabledCustomIcons + targets
        } else {
            current.enabledCustomIcons - targets
        }
        val newColors = current.iconColors.toMutableMap().apply {
            targets.forEach { put(it, selectedColor) }
        }
        val newSizes = current.iconSizes.toMutableMap().apply {
            targets.forEach { put(it, selectedSizeDp) }
        }

        if (iconId == "animation") {
            preferenceRepository.setMascotSize(selectedSizeDp)
            preferenceRepository.setCustomIconSize("animation", selectedSizeDp)
            preferenceRepository.setEmojiBatteryEnabled(isEnabled)
        }

        val updated = current.copy(
            enabledCustomIcons = newEnabled,
            iconColors = newColors,
            iconSizes = newSizes,
            mascotSizeDp = if (iconId == "animation") selectedSizeDp else current.mascotSizeDp,
            isEmojiBatteryEnabled = if (iconId == "animation") isEnabled else current.isEmojiBatteryEnabled,
            emotionStyle = if (iconId == "emotion") selectedEmotion else current.emotionStyle,
            // A built-in style drops the file; a remote one keeps the new (or already applied) file
            emotionPath = when {
                iconId != "emotion" -> current.emotionPath
                remoteEmotionPath != null -> remoteEmotionPath
                selectedEmotion == StatusBarSettings.EMOTION_STYLE_REMOTE -> current.emotionPath
                else -> ""
            },
            dateFormat = if (iconId == "calendar") selectedDateFormat else current.dateFormat,
            dateStyle = if (iconId == "calendar") selectedDateStyle else current.dateStyle
        )
        preferenceRepository.updateStatusBarSettings(updated)

        // Trigger real-time status bar overlay update if service is running
        StatusBarAccessibilityService.updateCustomSettings()

        Toast.makeText(requireContext(), getString(R.string.item_customized_format, binding.topHeader.title), Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
