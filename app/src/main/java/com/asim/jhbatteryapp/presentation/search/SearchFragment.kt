package com.asim.jhbatteryapp.presentation.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.DialogBatteryPreviewBinding
import com.asim.jhbatteryapp.databinding.FragmentSearchBinding
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.presentation.home.BatteryEmojiAdapter
import com.asim.jhbatteryapp.presentation.home.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class SearchFragment : Fragment() {

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
    }

    private fun setupRecyclerViews() {
        // Recent Grid (3 columns)
        recentAdapter = BatteryEmojiAdapter { item ->
            showPreviewBottomSheet(item)
        }
        binding.rvRecentGrid.adapter = recentAdapter

        // Top Picks Grid (3 columns)
        topPicksAdapter = BatteryEmojiAdapter { item ->
            showPreviewBottomSheet(item)
        }
        binding.rvTopPicksGrid.adapter = topPicksAdapter
    }

    private fun setupListeners() {
        // Back Button -> Navigate back using NavController!
        binding.btnCloseSearch.setOnClickListener {
            findNavController().navigateUp()
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

        // Filter Chips (All, Heart, Cute Emoji, Animations, Anime)
        binding.chipAll.setOnClickListener {
            highlightChip(binding.chipAll)
            viewModel.search("")
        }
        binding.chipHeart.setOnClickListener {
            highlightChip(binding.chipHeart)
            viewModel.search("heart")
        }
        binding.chipCuteEmoji.setOnClickListener {
            highlightChip(binding.chipCuteEmoji)
            viewModel.search("cute")
        }
        binding.chipAnimations.setOnClickListener {
            highlightChip(binding.chipAnimations)
            viewModel.search("anime")
        }
        binding.chipAnime.setOnClickListener {
            highlightChip(binding.chipAnime)
            viewModel.search("anime")
        }
    }

    private fun observeViewModel() {
        viewModel.recentEmojis.observe(viewLifecycleOwner) { recentList ->
            recentAdapter.submitList(recentList)
        }

        viewModel.topPickEmojis.observe(viewLifecycleOwner) { topPicksList ->
            topPicksAdapter.submitList(topPicksList)
        }
    }

    private fun highlightChip(activeChip: TextView) {
        val chips = listOf(
            binding.chipAll,
            binding.chipHeart,
            binding.chipCuteEmoji,
            binding.chipAnimations,
            binding.chipAnime
        )
        for (chip in chips) {
            if (chip == activeChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.tab_indicator_purple))
                chip.typeface = android.graphics.Typeface.DEFAULT_BOLD
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                chip.typeface = android.graphics.Typeface.DEFAULT
            }
        }
    }

    private fun showPreviewBottomSheet(item: BatteryEmojiItem) {
        com.asim.jhbatteryapp.core.Utils.MyUtils.genBottomSheetDialog(
            context = requireContext(),
            isCancelable = true,
            style = R.style.Style_Rounded_Bottom_Sheet,
            bindingInflater = DialogBatteryPreviewBinding::inflate
        ) { dialogBinding ->
            dialogBinding.cvPreviewContainer.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), item.backgroundColorRes)
            )
            dialogBinding.ivDialogMascot.setImageResource(item.previewDrawableRes)
            dialogBinding.tvDialogTitle.text = item.title

            dialogBinding.btnApplyStyle.setOnClickListener {
                val hasAccessibility = com.asim.jhbatteryapp.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(requireContext())
                if (!hasAccessibility) {
                    dismiss()
                    com.asim.jhbatteryapp.presentation.common.StatusBarPermissionHelper.showAccessibilityDialog(requireContext())
                    return@setOnClickListener
                }
                val prefRepo = com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl()
                prefRepo.setAppliedBatteryIconRes(item.previewDrawableRes)
                prefRepo.setBatteryIconTintable(false)
                prefRepo.setEmojiBatteryEnabled(true)
                com.asim.jhbatteryapp.service.StatusBarAccessibilityService.updateBatteryIcon(item.previewDrawableRes, isTintable = false)
                Toast.makeText(requireContext(), "${item.title} applied as battery icon!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
