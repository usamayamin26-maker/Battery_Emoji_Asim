package com.asim.jhbatteryapp.presentation.sticker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.remote.RemoteImageLoader
import com.asim.jhbatteryapp.databinding.FragmentStickerDisplayBinding
import com.asim.jhbatteryapp.domain.model.RemoteAssetItem
import com.asim.jhbatteryapp.domain.model.StickerItem
import com.universal.remote.ads.AdsController
import kotlinx.coroutines.launch

class StickerDisplayFragment : Fragment() {

    companion object {
        private const val SCREEN_KEY = "sticker"
    }

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
                        com.asim.jhbatteryapp.service.FloatingStickerService.updateStickerDrawable(nextActive.drawableRes, nextActive.imagePath)
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
        gridAdapter = StickerGridAdapter(
            onRemoteItemClick = { remoteItem -> onRemoteStickerSelected(remoteItem) },
            onItemClick = { clickedItem -> onStickerSelected(clickedItem) }
        )
        binding.rvStickerGrid.adapter = gridAdapter
    }

    private var pendingStickerToFloat: StickerItem? = null
    private var isDownloadingSticker = false

    /** Remote stickers are downloaded to a file first, then go through the same flow as local ones. */
    private fun onRemoteStickerSelected(asset: RemoteAssetItem) {
        if (isDownloadingSticker) return
        isDownloadingSticker = true
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val path = RemoteImageLoader.saveSticker(asset)
            isDownloadingSticker = false
            if (path == null) {
                Toast.makeText(appContext, R.string.remote_asset_download_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            onStickerSelected(
                StickerItem(
                    id = asset.id,
                    title = asset.name,
                    category = asset.category,
                    // Res 0 tells the overlay to draw the downloaded file instead of a drawable
                    drawableRes = 0,
                    imagePath = path
                )
            )
        }
    }

    private fun onStickerSelected(item: StickerItem) {
        viewModel.addOrSelectSticker(item)
        viewModel.setActiveSticker(item)

        // Check Accessibility permission (primary) or overlay permission (fallback)
        if (!canFloatSticker()) {
            pendingStickerToFloat = item
            // Display over other apps is all the sticker needs; onResume floats it once allowed
            com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.showPermissionDialog(
                context = requireContext(),
                onProceed = {
                    if (canFloatSticker()) {
                        pendingStickerToFloat = null
                        onStickerSelected(item)
                    }
                }
            )
            return
        }

        // 1. Immediately float on screen!
        com.asim.jhbatteryapp.service.FloatingStickerService.start(
            context = requireContext(),
            drawableRes = item.drawableRes,
            imagePath = item.imagePath,
            sizeDp = item.sizeDp,
            rotationDeg = item.rotationDeg
        )
        com.asim.jhbatteryapp.service.FloatingStickerService.updateStickerDrawable(item.drawableRes, item.imagePath)

        // 2. Open adjustment dialog so user can move it on screen with buttons and apply!
        openAdjustmentDialog(item)
    }

    override fun onResume() {
        super.onResume()
        val pending = pendingStickerToFloat
        if (pending != null) {
            if (canFloatSticker()) {
                pendingStickerToFloat = null
                onStickerSelected(pending)
            }
        }
    }

    private fun setupListeners() {
        AdsController.routeSystemBack(this, binding.topHeader.backButton)
        binding.topHeader.backButton.setOnClickListener {
            AdsController.back(this, "is_show_sticker_back_inter") {
                findNavController().navigateUp()
            }
        }

        binding.clPreviewDashedBox.setOnClickListener {
            viewModel.activeSticker.value?.let { active ->
                onStickerSelected(active)
            }
        }

        binding.btnSaveStickers.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdsController.interstitial(act, "is_show_sticker_btn_inter") {
                val active = viewModel.activeSticker.value
                if (active == null) {
                    Toast.makeText(requireContext(), getString(R.string.please_select_sticker), Toast.LENGTH_SHORT).show()
                    return@interstitial
                }

                if (!canFloatSticker()) {
                    pendingStickerToFloat = active
                    com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.showPermissionDialog(
                        context = requireContext(),
                        onProceed = {
                            if (canFloatSticker()) {
                                pendingStickerToFloat = null
                                onStickerSelected(active)
                            }
                        }
                    )
                    return@interstitial
                }

                val pref = com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl()
                val activeInst = com.asim.jhbatteryapp.service.FloatingStickerService.instance
                val curX = activeInst?.currentX ?: pref.getFloatingStickerX()
                val curY = activeInst?.currentY ?: pref.getFloatingStickerY()

                pref.setFloatingStickerEnabled(true)
                pref.setFloatingStickerDrawableRes(active.drawableRes)
                pref.setFloatingStickerImagePath(active.imagePath.orEmpty())
                pref.setFloatingStickerSize(active.sizeDp)
                pref.setFloatingStickerRotation(active.rotationDeg)
                if (curX > 0 && curY > 0) {
                    pref.setFloatingStickerX(curX)
                    pref.setFloatingStickerY(curY)
                }

                com.asim.jhbatteryapp.service.FloatingStickerService.start(
                    context = requireContext(),
                    drawableRes = active.drawableRes,
                    imagePath = active.imagePath,
                    x = if (curX > 0) curX else null,
                    y = if (curY > 0) curY else null,
                    sizeDp = active.sizeDp,
                    rotationDeg = active.rotationDeg
                )

                Toast.makeText(
                    requireContext(),
                    R.string.sticker_fixed_on_screen,
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
            }
        }
    }

    /**
     * The floating sticker sits anywhere on screen, not over the status bar, so "Display over other
     * apps" alone is enough: FloatingStickerService draws it as TYPE_APPLICATION_OVERLAY. Accessibility
     * is only preferred when it is already on.
     */
    private fun canFloatSticker(): Boolean {
        val ctx = requireContext()
        return com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(ctx) ||
                com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(ctx)
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
                pref.setFloatingStickerImagePath(sticker.imagePath.orEmpty())
                pref.setFloatingStickerSize(sizeDp)
                pref.setFloatingStickerRotation(rotationDeg)
                if (curX > 0 && curY > 0) {
                    pref.setFloatingStickerX(curX)
                    pref.setFloatingStickerY(curY)
                }

                if (canFloatSticker()) {
                    com.asim.jhbatteryapp.service.FloatingStickerService.start(
                        context = requireContext(),
                        drawableRes = sticker.drawableRes,
                        imagePath = sticker.imagePath,
                        x = if (curX > 0) curX else null,
                        y = if (curY > 0) curY else null,
                        sizeDp = sizeDp,
                        rotationDeg = rotationDeg
                    )
                }

                Toast.makeText(
                    requireContext(),
                    R.string.sticker_adjusted_fixed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        ).show()
    }

    private fun observeViewModel() {
        viewModel.tabs.observe(viewLifecycleOwner) { tabs ->
            val currentKey = viewModel.selectedTab.value ?: "hot"
            // Show translated tab names; unknown keys keep the label from the view model
            val labelRes = mapOf(
                "hot" to R.string.tab_hot,
                "poke" to R.string.tab_pokemon,
                "sanrio" to R.string.tab_sanrio,
                "anime" to R.string.tab_anime
            )
            tabAdapter.setTabs(tabs.map { (key, label) -> key to (labelRes[key]?.let(::getString) ?: label) }, currentKey)
        }

        viewModel.selectedTab.observe(viewLifecycleOwner) { key ->
            tabAdapter.setSelectedTab(key)
        }

        viewModel.gridStickers.observe(viewLifecycleOwner) { stickers ->
            gridAdapter.submitList(stickers)
        }

        // Remote stickers from the JSON manifest, appended after local items
        viewModel.remoteStickers.observe(viewLifecycleOwner) { remote ->
            gridAdapter.submitRemoteList(remote)
        }

        viewModel.myStickers.observe(viewLifecycleOwner) { myStickers ->
            val activeId = viewModel.activeSticker.value?.id
            myStickerAdapter.submitList(myStickers, activeId)
            binding.tvMyStickerCount.text = "${myStickers.size}/5"
        }

        viewModel.activeSticker.observe(viewLifecycleOwner) { active ->
            if (active != null) {
                RemoteImageLoader.bindSticker(binding.ivPreviewSticker, active.drawableRes, active.imagePath)
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
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
