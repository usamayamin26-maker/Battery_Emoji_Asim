package com.jhw.batteryemoji.presentation.animation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.FragmentStatusBarAnimationBinding
import com.universal.remote.ads.AdsController
import kotlinx.coroutines.launch

class StatusBarAnimationFragment : Fragment() {

    companion object {
        private const val SCREEN_KEY = "animation"
    }

    private var _binding: FragmentStatusBarAnimationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatusBarAnimationViewModel by activityViewModels()

    private lateinit var animationAdapter: StatusBarAnimationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusBarAnimationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        animationAdapter = StatusBarAnimationAdapter(
            onRemoteItemClick = { remoteItem ->
                viewModel.selectRemoteAnimation(remoteItem)
                pulsePreviewMascot()
            }
        ) { selectedItem ->
            viewModel.selectAnimation(selectedItem)
            pulsePreviewMascot()
        }
        binding.rvAnimGrid.adapter = animationAdapter
    }

    private fun setupListeners() {
        AdsController.routeSystemBack(this, binding.topHeader.backButton)
        binding.topHeader.backButton.setOnClickListener {
            AdsController.back(this, "is_show_animation_back_inter") {
                findNavController().navigateUp()
            }
        }

        binding.btnAnimBannerStart.setOnClickListener {
            viewModel.toggleBatteryActive()
            applyCurrentAnimation()
        }

        binding.tvSeeAll.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_animation_btn_inter") {
                findNavController().navigate(R.id.action_statusBarAnimationFragment_to_animationSeeAllFragment)
            }
        }

        binding.sbDateSize.max = com.jhw.batteryemoji.domain.model.StatusBarSettings.MAX_ANIMATION_SIZE_DP
        binding.sbDateSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clampedProgress = progress.coerceIn(com.jhw.batteryemoji.domain.model.StatusBarSettings.MIN_ANIMATION_SIZE_DP, com.jhw.batteryemoji.domain.model.StatusBarSettings.MAX_ANIMATION_SIZE_DP)
                binding.tvDateSizeValue.text = "${clampedProgress}dp"
                if (fromUser) {
                    viewModel.setDateSize(clampedProgress)
                }
                // Live scale preview
                val scale = previewScale(clampedProgress)
                binding.ivPreviewMascot.scaleX = scale
                binding.ivPreviewMascot.scaleY = scale
                binding.lottiePreviewMascot.scaleX = scale
                binding.lottiePreviewMascot.scaleY = scale
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnApplyAnimation.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_animation_btn_inter") {
                applyCurrentAnimation()
            }
        }
    }

    private var isAwaitingPermission = false

    private fun applyCurrentAnimation() {
        viewModel.selectedRemoteAnimation.value?.let { remote ->
            applyRemoteAnimation(remote)
            return
        }
        val selected = viewModel.selectedAnimation.value ?: return
        val size = viewModel.dateSize.value ?: 50
        viewModel.applyAnimation(selected)

        val isAccessibilityEnabled = com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
        val hasOverlay = com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(requireContext())

        if (isAccessibilityEnabled) {
            com.jhw.batteryemoji.service.StatusBarAccessibilityService.updateAnimation(
                animationRawRes = if (selected.isLottie) selected.lottieRawRes else 0,
                isLottie = selected.isLottie,
                fallbackDrawableRes = selected.previewDrawableRes,
                sizeDp = size
            )
            com.jhw.batteryemoji.service.StatusBarAccessibilityService.updateCustomSettings()
            Toast.makeText(
                requireContext(),
                getString(R.string.animation_applied_success),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
                context = requireContext(),
                onSettingsOpened = {
                    isAwaitingPermission = true
                }
            )
        }
    }

    /** Downloads the remote animation, saves it, then applies it like a local one. */
    private fun applyRemoteAnimation(item: com.jhw.batteryemoji.domain.model.RemoteAssetItem) {
        val appContext = requireContext().applicationContext
        binding.btnApplyAnimation.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val path = RemoteImageLoader.saveForApply(item)
            _binding?.btnApplyAnimation?.isEnabled = true
            if (path == null) {
                Toast.makeText(appContext, R.string.remote_asset_download_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            viewModel.applyRemoteAnimation(path)
            val ctx = context ?: return@launch
            if (com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(ctx)) {
                com.jhw.batteryemoji.service.StatusBarAccessibilityService.updateCustomSettings()
                Toast.makeText(appContext, R.string.animation_applied_success, Toast.LENGTH_SHORT).show()
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
        val selected = viewModel.selectedRemoteAnimation.value ?: viewModel.selectedAnimation.value
        if (selected != null && isAwaitingPermission) {
            val isAccessibility = com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
            if (isAccessibility) {
                isAwaitingPermission = false
                applyCurrentAnimation()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.animations.observe(viewLifecycleOwner) { anims ->
            val selectedId = viewModel.selectedAnimation.value?.id ?: (anims.firstOrNull()?.id ?: "")
            animationAdapter.setItems(anims, selectedId)
        }

        // Remote animations from the JSON manifest, appended after local items
        viewModel.remoteAnimations.observe(viewLifecycleOwner) { remote ->
            animationAdapter.submitRemoteList(remote)
        }

        viewModel.selectedAnimation.observe(viewLifecycleOwner) { mascot ->
            if (mascot != null) {
                if (mascot.isLottie && mascot.lottieRawRes != 0) {
                    binding.ivPreviewMascot.visibility = View.GONE
                    binding.lottiePreviewMascot.visibility = View.VISIBLE
                    binding.lottiePreviewMascot.setAnimation(mascot.lottieRawRes)
                    binding.lottiePreviewMascot.playAnimation()
                } else {
                    binding.lottiePreviewMascot.visibility = View.GONE
                    binding.lottiePreviewMascot.cancelAnimation()
                    binding.ivPreviewMascot.visibility = View.VISIBLE
                    binding.ivPreviewMascot.setImageResource(mascot.previewDrawableRes)
                }
                // Cancel any in-flight remote preview load targeting this view
                binding.lottiePreviewMascot.tag = null
                animationAdapter.setSelectedId(mascot.id)
            }
        }

        // Remote selection overrides the local preview (registered after it on purpose)
        viewModel.selectedRemoteAnimation.observe(viewLifecycleOwner) { remote ->
            if (remote == null) return@observe
            binding.ivPreviewMascot.visibility = View.GONE
            binding.lottiePreviewMascot.visibility = View.VISIBLE
            RemoteImageLoader.loadLottie(remote, binding.lottiePreviewMascot)
            animationAdapter.setSelectedId(remote.id)
        }

        viewModel.dateSize.observe(viewLifecycleOwner) { size ->
            binding.sbDateSize.progress = size
            binding.tvDateSizeValue.text = "${size}dp"
            val scale = previewScale(size)
            binding.ivPreviewMascot.scaleX = scale
            binding.ivPreviewMascot.scaleY = scale
            binding.lottiePreviewMascot.scaleX = scale
            binding.lottiePreviewMascot.scaleY = scale
        }

        viewModel.isBatteryActive.observe(viewLifecycleOwner) { isActive ->
            if (isActive) {
                binding.tvAnimBannerText.text = getString(R.string.emoji_battery_is_active)
                binding.btnAnimBannerStart.text = getString(R.string.status_active)
            } else {
                binding.tvAnimBannerText.text = getString(R.string.enable_emoji_battery_banner)
                binding.btnAnimBannerStart.text = getString(R.string.start)
            }
        }
    }

    /** Preview mascot scale for a size on the slider (0.6x at the smallest up to 1.3x at the largest). */
    private fun previewScale(sizeDp: Int): Float =
        0.6f + (sizeDp.toFloat() / com.jhw.batteryemoji.domain.model.StatusBarSettings.MAX_ANIMATION_SIZE_DP) * 0.7f

    private fun pulsePreviewMascot() {
        val targetView = if (binding.lottiePreviewMascot.visibility == View.VISIBLE) {
            binding.lottiePreviewMascot
        } else {
            binding.ivPreviewMascot
        }
        val currentScaleX = targetView.scaleX
        val currentScaleY = targetView.scaleY
        val scaleUpX = ObjectAnimator.ofFloat(targetView, View.SCALE_X, currentScaleX * 1.15f, currentScaleX)
        val scaleUpY = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, currentScaleY * 1.15f, currentScaleY)
        AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
            duration = 300
            start()
        }
    }

    override fun onDestroyView() {
        binding.lottiePreviewMascot.cancelAnimation()
        super.onDestroyView()
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
