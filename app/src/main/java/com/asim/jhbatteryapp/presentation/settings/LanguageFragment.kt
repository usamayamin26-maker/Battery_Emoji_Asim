package com.asim.jhbatteryapp.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.FragmentLanguageBinding

class LanguageFragment : Fragment() {

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LanguageAdapter

    private val languages by lazy {
        listOf(
            LanguageItem("ar", getString(R.string.lang_arabic), getString(R.string.lang_arabic_native), R.drawable.ic_flag_sa),
            LanguageItem("zh", getString(R.string.lang_chinese), getString(R.string.lang_chinese_native), R.drawable.ic_flag_cn),
            LanguageItem("nl", getString(R.string.lang_dutch), getString(R.string.lang_dutch_native), R.drawable.ic_flag_nl),
            LanguageItem("en", getString(R.string.lang_english), getString(R.string.lang_english_native), R.drawable.ic_flag_us, isSelected = true),
            LanguageItem("fr", getString(R.string.lang_french), getString(R.string.lang_french_native), R.drawable.ic_flag_fr)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = LanguageAdapter(languages) { selected ->
            // Language selection updated
        }
        binding.rvLanguages.adapter = adapter
    }

    private fun setupListeners() {
        // Back Button
        binding.btnBackLanguage.setOnClickListener {
            findNavController().navigateUp()
        }

        // Apply Button
        binding.btnApplyLanguage.setOnClickListener {
            val selected = adapter.getSelectedLanguage()
            Toast.makeText(requireContext(), "Language changed to ${selected.name}", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
