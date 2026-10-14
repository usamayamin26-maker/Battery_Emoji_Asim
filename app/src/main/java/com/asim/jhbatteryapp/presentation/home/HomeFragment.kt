package com.asim.jhbatteryapp.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.DialogBatteryPreviewBinding
import com.asim.jhbatteryapp.databinding.FragmentHomeBinding
import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.presentation.common.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialog

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels {
        ViewModelFactory(requireContext())
    }

    private lateinit var emojiAdapter: BatteryEmojiAdapter
    private lateinit var categoryAdapter: CategoryTabAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Category Tabs
        categoryAdapter = CategoryTabAdapter { category ->
            viewModel.selectCategory(category.id)
        }
        binding.rvCategoryTabs.adapter = categoryAdapter

        // Main Emoji Grid (3 columns)
        emojiAdapter = BatteryEmojiAdapter { item ->
            showPreviewBottomSheet(item)
        }
        binding.rvEmojiGrid.adapter = emojiAdapter
    }

    private fun setupListeners() {
        // Navigate to SearchFragment using NavGraph action!
        binding.btnOpenSearch.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        // Banner Start Button
        binding.btnBannerStart.setOnClickListener {
            viewModel.toggleEmojiBattery()
        }

        // Quick Action Cards
        binding.cardStickerShop.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Sticker Shop…", Toast.LENGTH_SHORT).show()
        }
        binding.cardAnimation.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Animations…", Toast.LENGTH_SHORT).show()
        }
        binding.cardThemes.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Themes…", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.setCategories(categories)
        }

        viewModel.emojis.observe(viewLifecycleOwner) { emojis ->
            emojiAdapter.submitList(emojis)
        }

        viewModel.isEmojiBatteryEnabled.observe(viewLifecycleOwner) { enabled ->
            if (enabled) {
                binding.tvBannerText.text = "Emoji battery is active"
                binding.btnBannerStart.text = "Active"
            } else {
                binding.tvBannerText.text = getString(R.string.enable_emoji_battery_banner)
                binding.btnBannerStart.text = getString(R.string.start)
            }
        }
    }

    private fun showPreviewBottomSheet(item: BatteryEmojiItem) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogBatteryPreviewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.cvPreviewContainer.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), item.backgroundColorRes)
        )
        dialogBinding.ivDialogMascot.setImageResource(item.previewDrawableRes)
        dialogBinding.tvDialogTitle.text = item.title

        dialogBinding.btnApplyStyle.setOnButtonClickListener {
            Toast.makeText(requireContext(), "${item.title} applied to battery bar!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
