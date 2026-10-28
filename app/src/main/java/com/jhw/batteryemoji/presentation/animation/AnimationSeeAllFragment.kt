package com.jhw.batteryemoji.presentation.animation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.jhw.batteryemoji.databinding.FragmentAnimationSeeAllBinding
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
        animationAdapter = StatusBarAnimationAdapter(
            onRemoteItemClick = { remoteItem ->
                viewModel.selectRemoteAnimation(remoteItem)
            }
        ) { selectedItem ->
            viewModel.selectAnimation(selectedItem)
        }
        binding.rvSeeAllGrid.adapter = animationAdapter
    }

    private fun setupListeners() {
        AdsController.routeSystemBack(this, binding.topHeader.backButton)
        binding.topHeader.backButton.setOnClickListener {
            AdsController.back(this, "is_show_animation_see_all_back_inter") {
                findNavController().navigateUp()
            }
        }

        binding.topHeader.actionButton.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_animation_see_all_btn_inter") {
                findNavController().navigateUp()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.animations.observe(viewLifecycleOwner) { anims ->
            val selectedId = viewModel.selectedAnimation.value?.id ?: (anims.firstOrNull()?.id ?: "")
            animationAdapter.setItems(anims, selectedId)
        }

        // Remote animations from the JSON manifest, appended after local items (same as the Animation screen)
        viewModel.remoteAnimations.observe(viewLifecycleOwner) { remote ->
            animationAdapter.submitRemoteList(remote)
        }

        viewModel.selectedAnimation.observe(viewLifecycleOwner) { mascot ->
            if (mascot != null) {
                animationAdapter.setSelectedId(mascot.id)
            }
        }

        // Remote selection overrides the local one (registered after it on purpose)
        viewModel.selectedRemoteAnimation.observe(viewLifecycleOwner) { remote ->
            if (remote != null) animationAdapter.setSelectedId(remote.id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
