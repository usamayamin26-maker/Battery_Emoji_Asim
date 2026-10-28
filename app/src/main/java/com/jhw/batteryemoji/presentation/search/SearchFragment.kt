package com.jhw.batteryemoji.presentation.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.data.remote.RemoteImageLoader
import com.jhw.batteryemoji.databinding.DialogBatteryPreviewBinding
import com.jhw.batteryemoji.databinding.FragmentSearchBinding
import com.jhw.batteryemoji.domain.model.BatteryEmojiItem
import com.jhw.batteryemoji.domain.model.CategoryItem
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import com.jhw.batteryemoji.presentation.home.BatteryEmojiAdapter
import com.jhw.batteryemoji.presentation.home.CategoryTabAdapter
import com.jhw.batteryemoji.presentation.home.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.universal.remote.ads.AdsController
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    companion object {
        private const val SCREEN_KEY = "search"
    }

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    private lateinit var recentAdapter: BatteryEmojiAdapter
    private lateinit var topPicksAdapter: BatteryEmojiAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
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
        // Filter Tabs: the category id doubles as the search query ("" = all)
        val tabAdapter = CategoryTabAdapter(initialSelectedId = "") { category ->
            viewModel.search(category.id)
        }
        tabAdapter.setCategories(
            listOf(
                CategoryItem(id = "", title = getString(R.string.chip_all)),
                CategoryItem(id = "heart", title = getString(R.string.chip_heart)),
                CategoryItem(id = "cute", title = getString(R.string.chip_cute_emoji)),
                CategoryItem(id = "anime", title = getString(R.string.tab_anime))
            )
        )
        binding.rvSearchTabs.itemAnimator = null
        binding.rvSearchTabs.adapter = tabAdapter

        // Recent Grid (3 columns)
        recentAdapter = BatteryEmojiAdapter(
            onRemoteItemClick = { remoteItem ->
                val act = activity ?: return@BatteryEmojiAdapter
                AdsController.interstitial(act, "is_show_search_btn_inter") {
                    showRemotePreviewBottomSheet(remoteItem)
                }
            }
        ) { item ->
            val act = activity ?: return@BatteryEmojiAdapter
            AdsController.interstitial(act, "is_show_search_btn_inter") {
                showPreviewBottomSheet(item)
            }
        }
        binding.rvRecentGrid.itemAnimator = null
        binding.rvRecentGrid.setHasFixedSize(true)
        binding.rvRecentGrid.setItemViewCacheSize(25)
        binding.rvRecentGrid.adapter = recentAdapter

        // Top Picks Grid (3 columns)
        topPicksAdapter = BatteryEmojiAdapter(
            onRemoteItemClick = { remoteItem ->
                val act = activity ?: return@BatteryEmojiAdapter
                AdsController.interstitial(act, "is_show_search_btn_inter") {
                    showRemotePreviewBottomSheet(remoteItem)
                }
            }
        ) { item ->
            val act = activity ?: return@BatteryEmojiAdapter
            AdsController.interstitial(act, "is_show_search_btn_inter") {
                showPreviewBottomSheet(item)
            }
        }
        binding.rvTopPicksGrid.itemAnimator = null
        binding.rvTopPicksGrid.setHasFixedSize(true)
        binding.rvTopPicksGrid.setItemViewCacheSize(25)
        binding.rvTopPicksGrid.adapter = topPicksAdapter
    }

    private fun setupListeners() {
        // Back Button & System Back -> Navigate back using NavController!
        AdsController.routeSystemBack(this, binding.topHeader.backButton)
        binding.topHeader.backButton.setOnClickListener {
            AdsController.back(this, "is_show_search_back_inter") {
                findNavController().navigateUp()
            }
        }

        // Clear Search Query
        binding.btnClearSearch.setOnClickListener {
            binding.etSearchQuery.setText("")
        }

        // Live Search TextWatcher
        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                viewModel.search(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.recentEmojis.observe(viewLifecycleOwner) { recentList ->
            recentAdapter.submitList(recentList)
            updateSectionHeaders()
            com.jhw.batteryemoji.core.Utils.ImageLoaderUtils.preload(
                context = requireContext(),
                drawableResList = recentList.map { it.previewDrawableRes }
            )
        }

        viewModel.topPickEmojis.observe(viewLifecycleOwner) { topPicksList ->
            topPicksAdapter.submitList(topPicksList)
            com.jhw.batteryemoji.core.Utils.ImageLoaderUtils.preload(
                context = requireContext(),
                drawableResList = topPicksList.map { it.previewDrawableRes }
            )
        }

        // Remote emojis from the JSON manifest, appended after local items (same as Home).
        // Registered after the local observers because submitList() clears the remote items.
        viewModel.remoteRecentEmojis.observe(viewLifecycleOwner) { remote ->
            recentAdapter.submitRemoteList(remote)
            updateSectionHeaders()
        }

        viewModel.remoteTopPickEmojis.observe(viewLifecycleOwner) { remote ->
            topPicksAdapter.submitRemoteList(remote)
            updateSectionHeaders()
        }
    }

    /** Hides a section's title when its grid has nothing to show. */
    private fun updateSectionHeaders() {
        val hasRecent = recentAdapter.itemCount > 0
        binding.tvRecentHeader.visibility = if (hasRecent) View.VISIBLE else View.GONE
        binding.rvRecentGrid.visibility = if (hasRecent) View.VISIBLE else View.GONE
        binding.tvTopPicksHeader.visibility = if (topPicksAdapter.itemCount > 0) View.VISIBLE else View.GONE
    }

    private fun showPreviewBottomSheet(item: BatteryEmojiItem) {
        com.jhw.batteryemoji.core.Utils.MyUtils.genBottomSheetDialog(
            context = requireContext(),
            isCancelable = true,
            style = R.style.Style_Rounded_Bottom_Sheet,
            bindingInflater = DialogBatteryPreviewBinding::inflate
        ) { dialogBinding ->
            dialogBinding.cvPreviewContainer.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), item.backgroundColorRes)
            )
            com.jhw.batteryemoji.core.Utils.ImageLoaderUtils.loadOptimizedEmoji(
                imageView = dialogBinding.ivDialogMascot,
                drawableRes = item.previewDrawableRes,
                targetWidthDp = 160,
                targetHeightDp = 160
            )
            dialogBinding.tvDialogTitle.text = item.title

            dialogBinding.btnApplyStyle.setOnClickListener {
                val hasAccessibility = com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
                if (!hasAccessibility) {
                    dismiss()
                    com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(requireContext())
                    return@setOnClickListener
                }
                val prefRepo = com.jhw.batteryemoji.data.repository.PreferenceRepositoryImpl()
                prefRepo.setAppliedBatteryIconRes(item.previewDrawableRes)
                prefRepo.setBatteryIconTintable(false)
                prefRepo.setEmojiBatteryEnabled(true)
                com.jhw.batteryemoji.service.StatusBarAccessibilityService.updateBatteryIcon(item.previewDrawableRes, isTintable = false)
                viewModel.markRecent(item)
                Toast.makeText(requireContext(), getString(R.string.item_applied_battery_format, item.title), Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    /** Same preview sheet as [showPreviewBottomSheet], for an emoji from the remote manifest. */
    private fun showRemotePreviewBottomSheet(item: RemoteAssetItem) {
        com.jhw.batteryemoji.core.Utils.MyUtils.genBottomSheetDialog(
            context = requireContext(),
            isCancelable = true,
            style = R.style.Style_Rounded_Bottom_Sheet,
            bindingInflater = DialogBatteryPreviewBinding::inflate
        ) { dialogBinding ->
            val bgColor = try {
                android.graphics.Color.parseColor(item.backgroundColor)
            } catch (_: Throwable) {
                ContextCompat.getColor(requireContext(), R.color.card_bg_pink)
            }
            dialogBinding.cvPreviewContainer.setCardBackgroundColor(bgColor)
            RemoteImageLoader.loadImage(item, dialogBinding.ivDialogMascot)
            dialogBinding.tvDialogTitle.text = item.name

            dialogBinding.btnApplyStyle.setOnClickListener {
                val hasAccessibility = com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
                if (!hasAccessibility) {
                    dismiss()
                    com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(requireContext())
                    return@setOnClickListener
                }
                dialogBinding.btnApplyStyle.isEnabled = false
                val appContext = requireContext().applicationContext
                viewLifecycleOwner.lifecycleScope.launch {
                    val path = RemoteImageLoader.saveForApply(item)
                    if (path == null) {
                        Toast.makeText(appContext, R.string.remote_asset_download_failed, Toast.LENGTH_SHORT).show()
                        dialogBinding.btnApplyStyle.isEnabled = true
                        return@launch
                    }
                    val prefRepo = com.jhw.batteryemoji.data.repository.PreferenceRepositoryImpl()
                    prefRepo.setAppliedBatteryIconPath(path)
                    // Res 0 tells the overlay to draw the downloaded file instead of a drawable
                    prefRepo.setAppliedBatteryIconRes(0)
                    prefRepo.setBatteryIconTintable(false)
                    prefRepo.setEmojiBatteryEnabled(true)
                    com.jhw.batteryemoji.service.StatusBarAccessibilityService.updateCustomSettings()
                    viewModel.markRecent(item)
                    Toast.makeText(appContext, appContext.getString(R.string.item_applied_battery_format, item.name), Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AdsController.forgetSlots(SCREEN_KEY)
        _binding = null
    }
}
