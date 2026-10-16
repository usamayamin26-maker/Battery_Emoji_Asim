package com.asim.jhbatteryapp.presentation.sticker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.FragmentStickerDisplayBinding
import com.asim.jhbatteryapp.domain.model.StickerItem

class StickerDisplayFragment : Fragment() {

    private var _binding: FragmentStickerDisplayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StickerDisplayViewModel by activityViewModels()

    private lateinit var myStickerAdapter: MyStickerAdapter
    private lateinit var gridAdapter: StickerGridAdapter
    private lateinit var tabAdapter: StickerTabAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStickerDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // My Sticker Horizontal List
        myStickerAdapter = MyStickerAdapter(
            onItemClick = { clickedItem ->
                onStickerSelected(clickedItem)
            },
            onRemoveClick = { itemToRemove ->
                viewModel.removeSticker(itemToRemove.id)
                val remaining = viewModel.myStickers.value
                if (remaining.isNullOrEmpty()) {
                    com.asim.jhbatteryapp.service.FloatingStickerService.stop(requireContext())
                    com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl().setFloatingStickerEnabled(false)
                } else {
                    val nextActive = remaining.firstOrNull()
                    if (nextActive != null && com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(requireContext())) {
                        com.asim.jhbatteryapp.service.FloatingStickerService.updateStickerDrawable(nextActive.drawableRes)
                    }
                }
            }
        )
        binding.rvMyStickers.adapter = myStickerAdapter

        // Category Tabs
        tabAdapter = StickerTabAdapter { selectedCategoryKey ->
            viewModel.selectCategory(selectedCategoryKey)
        }
        binding.rvStickerTabs.adapter = tabAdapter

        // 4-Column Sticker Grid: Clicking ANY sticker starts floating it on screen immediately!
        gridAdapter = StickerGridAdapter { clickedItem ->
            onStickerSelected(clickedItem)
        }
        binding.rvStickerGrid.adapter = gridAdapter
    }

    private var pendingStickerToFloat: StickerItem? = null

    private fun onStickerSelected(item: StickerItem) {
        viewModel.addOrSelectSticker(item)
        viewModel.setActiveSticker(item)

        val isAccessibilityEnabled = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
        val hasOverlay = com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(requireContext())

        // Check Accessibility permission (primary) or overlay permission (fallback)
        if (!isAccessibilityEnabled && !hasOverlay) {
            pendingStickerToFloat = item
            com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
                context = requireContext(),
                onSettingsOpened = {
                    // Directed to Accessibility Settings
                },
                onDismissed = {
                    pendingStickerToFloat = null
                }
            )
            return
        }

        // 1. Immediately float on screen!
        com.asim.jhbatteryapp.service.FloatingStickerService.start(
            context = requireContext(),
            drawableRes = item.drawableRes,
            sizeDp = item.sizeDp,
            rotationDeg = item.rotationDeg
        )
        com.asim.jhbatteryapp.service.FloatingStickerService.updateStickerDrawable(item.drawableRes)

        // 2. Open adjustment dialog so user can move it on screen with buttons and apply!
        openAdjustmentDialog(item)
    }

    override fun onResume() {
        super.onResume()
        val pending = pendingStickerToFloat
        if (pending != null) {
            val isAccess = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
            if (isAccess) {
                pendingStickerToFloat = null
                onStickerSelected(pending)
            }
        }
    }

    private fun setupListeners() {
        binding.ivStickerBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.clPreviewDashedBox.setOnClickListener {
            viewModel.activeSticker.value?.let { active ->
                onStickerSelected(active)
            }
        }

        binding.btnSaveStickers.setOnClickListener {
            val active = viewModel.activeSticker.value
            if (active == null) {
                Toast.makeText(requireContext(), "Please select a sticker first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isAccessibilityEnabled = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())

            if (!isAccessibilityEnabled) {
                pendingStickerToFloat = active
                com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(
                    context = requireContext(),
                    onSettingsOpened = {},
                    onDismissed = { pendingStickerToFloat = null }
                )
                return@setOnClickListener
            }

            val pref = com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl()
            val activeInst = com.asim.jhbatteryapp.service.FloatingStickerService.instance
            val curX = activeInst?.currentX ?: pref.getFloatingStickerX()
            val curY = activeInst?.currentY ?: pref.getFloatingStickerY()

            pref.setFloatingStickerEnabled(true)
            pref.setFloatingStickerDrawableRes(active.drawableRes)
            pref.setFloatingStickerSize(active.sizeDp)
            pref.setFloatingStickerRotation(active.rotationDeg)
            if (curX > 0 && curY > 0) {
                pref.setFloatingStickerX(curX)
                pref.setFloatingStickerY(curY)
            }

            com.asim.jhbatteryapp.service.FloatingStickerService.start(
                context = requireContext(),
                drawableRes = active.drawableRes,
                x = if (curX > 0) curX else null,
                y = if (curY > 0) curY else null,
                sizeDp = active.sizeDp,
                rotationDeg = active.rotationDeg
            )

            Toast.makeText(
                requireContext(),
                "Sticker fixed on screen! It stays visible even when app is closed.",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigateUp()
        }
    }

    private fun openAdjustmentDialog(sticker: StickerItem) {
        StickerAdjustmentDialog(
            context = requireContext(),
            sticker = sticker,
            onApply = { offsetX, offsetY, sizeDp, rotationDeg ->
                viewModel.updateActiveStickerAdjustment(offsetX, offsetY, sizeDp, rotationDeg)
                val pref = com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl()
                val activeInst = com.asim.jhbatteryapp.service.FloatingStickerService.instance
                val curX = activeInst?.currentX ?: pref.getFloatingStickerX()
                val curY = activeInst?.currentY ?: pref.getFloatingStickerY()

                pref.setFloatingStickerEnabled(true)
                pref.setFloatingStickerDrawableRes(sticker.drawableRes)
                pref.setFloatingStickerSize(sizeDp)
                pref.setFloatingStickerRotation(rotationDeg)
                if (curX > 0 && curY > 0) {
                    pref.setFloatingStickerX(curX)
                    pref.setFloatingStickerY(curY)
                }

                val isAccessibility = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())

                if (isAccessibility) {
                    com.asim.jhbatteryapp.service.FloatingStickerService.start(
                        context = requireContext(),
                        drawableRes = sticker.drawableRes,
                        x = if (curX > 0) curX else null,
                        y = if (curY > 0) curY else null,
                        sizeDp = sizeDp,
                        rotationDeg = rotationDeg
                    )
                }

                Toast.makeText(
                    requireContext(),
                    "Sticker adjusted and fixed on screen!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        ).show()
    }

    private fun observeViewModel() {
        viewModel.tabs.observe(viewLifecycleOwner) { tabs ->
            val currentKey = viewModel.selectedTab.value ?: "hot"
            tabAdapter.setTabs(tabs, currentKey)
        }

        viewModel.selectedTab.observe(viewLifecycleOwner) { key ->
            tabAdapter.setSelectedTab(key)
        }

        viewModel.gridStickers.observe(viewLifecycleOwner) { stickers ->
            gridAdapter.submitList(stickers)
        }

        viewModel.myStickers.observe(viewLifecycleOwner) { myStickers ->
            val activeId = viewModel.activeSticker.value?.id
            myStickerAdapter.submitList(myStickers, activeId)
            binding.tvMyStickerCount.text = "${myStickers.size}/5"
        }

        viewModel.activeSticker.observe(viewLifecycleOwner) { active ->
            if (active != null) {
                binding.ivPreviewSticker.setImageResource(active.drawableRes)
                binding.ivPreviewSticker.visibility = View.VISIBLE

                binding.ivPreviewSticker.translationX = active.offsetX
                binding.ivPreviewSticker.translationY = active.offsetY

                val scale = 0.5f + (active.sizeDp / 100f) * 0.8f
                binding.ivPreviewSticker.scaleX = scale
                binding.ivPreviewSticker.scaleY = scale

                binding.ivPreviewSticker.rotation = active.rotationDeg

                val currentList = viewModel.myStickers.value ?: emptyList()
                myStickerAdapter.submitList(currentList, active.id)
            } else {
                binding.ivPreviewSticker.visibility = View.INVISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
