package com.asim.jhbatteryapp.presentation.animation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.databinding.FragmentAnimationSeeAllBinding
import com.universal.remote.ads.AdsController

class AnimationSeeAllFragment : Fragment() {

    companion object {
        private const val SCREEN_KEY = "animation_see_all"
    }

    private var _binding: FragmentAnimationSeeAllBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatusBarAnimationViewModel by activityViewModels()

    private lateinit var animationAdapter: StatusBarAnimationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimationSeeAllBinding.inflate(inflater, container, false)
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
        }
        binding.rvSeeAllGrid.adapter = animationAdapter
    }

    private fun setupListeners() {
        binding.ivSeeAllBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.ivSeeAllConfirm.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.animations.observe(viewLifecycleOwner) { anims ->
            val selectedId = viewModel.selectedAnimation.value?.id ?: (anims.firstOrNull()?.id ?: "")
            animationAdapter.setItems(anims, selectedId)
        }

        viewModel.selectedAnimation.observe(viewLifecycleOwner) { mascot ->
            if (mascot != null) {
                animationAdapter.setSelectedId(mascot.id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
