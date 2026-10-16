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

class StatusBarAnimationFragment : Fragment() {

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

        val prefRepo = com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl()
        prefRepo.setAppliedMascotDrawableRes(selected.previewDrawableRes)
        prefRepo.setMascotSize(size)
        prefRepo.setAppliedBatteryIconRes(selected.previewDrawableRes)
        prefRepo.setBatteryIconTintable(false)
        prefRepo.setEmojiBatteryEnabled(true)

        val isAccessibilityEnabled = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
        val hasOverlay = com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(requireContext())

        if (isAccessibilityEnabled) {
            com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateMascot(
                mascotRes = selected.previewDrawableRes
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
                binding.ivPreviewMascot.setImageResource(mascot.previewDrawableRes)
                animationAdapter.setSelectedId(mascot.id)
            }
        }

        viewModel.dateSize.observe(viewLifecycleOwner) { size ->
            binding.sbDateSize.progress = size
            binding.tvDateSizeValue.text = "${size}dp"
            val scale = 0.6f + (size / 100f) * 0.7f
            binding.ivPreviewMascot.scaleX = scale
            binding.ivPreviewMascot.scaleY = scale
        }

        viewModel.isBatteryActive.observe(viewLifecycleOwner) { isActive ->
            if (isActive) {
                binding.tvAnimBannerText.text = "Emoji battery is active"
                binding.btnAnimBannerStart.text = "Active"
            } else {
                binding.tvAnimBannerText.text = getString(R.string.enable_emoji_battery_banner)
                binding.btnAnimBannerStart.text = getString(R.string.start)
            }
        }
    }

    private fun pulsePreviewMascot() {
        val scaleUpX = ObjectAnimator.ofFloat(binding.ivPreviewMascot, View.SCALE_X, 1.15f, 1.0f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.ivPreviewMascot, View.SCALE_Y, 1.15f, 1.0f)
        AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
            duration = 300
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
