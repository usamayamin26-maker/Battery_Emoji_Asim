package com.asim.jhbatteryapp.presentation.animation

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
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.FragmentStatusBarAnimationBinding
import com.universal.remote.ads.AdsController

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
        animationAdapter = StatusBarAnimationAdapter { selectedItem ->
            viewModel.selectAnimation(selectedItem)
            pulsePreviewMascot()
        }
        binding.rvAnimGrid.adapter = animationAdapter
    }

    private fun setupListeners() {
        binding.ivAnimBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAnimBannerStart.setOnClickListener {
            viewModel.toggleBatteryActive()
            applyCurrentAnimation()
        }

        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_statusBarAnimationFragment_to_animationSeeAllFragment)
        }

        binding.sbDateSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clampedProgress = progress.coerceIn(10, 100)
                binding.tvDateSizeValue.text = "${clampedProgress}dp"
                if (fromUser) {
                    viewModel.setDateSize(clampedProgress)
                }
                // Live scale preview
                val scale = 0.6f + (clampedProgress / 100f) * 0.7f
                binding.ivPreviewMascot.scaleX = scale
                binding.ivPreviewMascot.scaleY = scale
                binding.lottiePreviewMascot.scaleX = scale
                binding.lottiePreviewMascot.scaleY = scale
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnApplyAnimation.setOnClickListener {
            applyCurrentAnimation()
        }
    }

    private var isAwaitingPermission = false

    private fun applyCurrentAnimation() {
        val selected = viewModel.selectedAnimation.value ?: return
        val size = viewModel.dateSize.value ?: 50
        viewModel.applyAnimation(selected)

        val isAccessibilityEnabled = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
        val hasOverlay = com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(requireContext())

        if (isAccessibilityEnabled) {
            com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateAnimation(
                animationRawRes = if (selected.isLottie) selected.lottieRawRes else 0,
                isLottie = selected.isLottie,
                fallbackDrawableRes = selected.previewDrawableRes,
                sizeDp = size
            )
            com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateCustomSettings()
            Toast.makeText(
                requireContext(),
                getString(R.string.animation_applied_success),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
                context = requireContext(),
                onSettingsOpened = {
                    isAwaitingPermission = true
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val selected = viewModel.selectedAnimation.value
        if (selected != null && isAwaitingPermission) {
            val isAccessibility = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
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
                animationAdapter.setSelectedId(mascot.id)
            }
        }

        viewModel.dateSize.observe(viewLifecycleOwner) { size ->
            binding.sbDateSize.progress = size
            binding.tvDateSizeValue.text = "${size}dp"
            val scale = 0.6f + (size / 100f) * 0.7f
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
