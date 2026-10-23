package com.asim.jhbatteryapp.presentation.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.data.datasource.PreferenceLocalDataSource
import com.asim.jhbatteryapp.databinding.ActivityLanguageBinding
import com.asim.jhbatteryapp.presentation.home.MainActivity
import com.asim.jhbatteryapp.presentation.onboarding.OnboardingActivity
import java.util.Locale

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageAdapter

    private val languages by lazy {
        val currentLangTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val activeCode = if (currentLangTag.isNotEmpty()) {
            currentLangTag.split("-").first().lowercase()
        } else {
            Locale.getDefault().language.lowercase()
        }

        val list = listOf(
            LanguageItem("en", getString(R.string.lang_english), getString(R.string.lang_english_native), R.drawable.uk_flag),
            LanguageItem("ar", getString(R.string.lang_arabic), getString(R.string.lang_arabic_native), R.drawable.arabic_flag1),
            LanguageItem("de", getString(R.string.lang_german), getString(R.string.lang_german_native), R.drawable.german_flag),
            LanguageItem("as", getString(R.string.lang_assamese), getString(R.string.lang_assamese_native), R.drawable.india_flag),
            LanguageItem("fa", getString(R.string.lang_persian), getString(R.string.lang_persian_native), R.drawable.persian_flag),
            LanguageItem("fr", getString(R.string.lang_french), getString(R.string.lang_french_native), R.drawable.french_flag),
            LanguageItem("hi", getString(R.string.lang_hindi), getString(R.string.lang_hindi_native), R.drawable.india_flag),
            LanguageItem("in", getString(R.string.lang_indonesian), getString(R.string.lang_indonesian_native), R.drawable.indonesian_flag),
            LanguageItem("ja", getString(R.string.lang_japanese), getString(R.string.lang_japanese_native), R.drawable.japan_flag),
            LanguageItem("ko", getString(R.string.lang_korean), getString(R.string.lang_korean_native), R.drawable.korean_flag),
            LanguageItem("ms", getString(R.string.lang_malay), getString(R.string.lang_malay_native), R.drawable.malashia_flag),
            LanguageItem("pt", getString(R.string.lang_portuguese), getString(R.string.lang_portuguese_native), R.drawable.portugal_flag),
            LanguageItem("ru", getString(R.string.lang_russian), getString(R.string.lang_russian_native), R.drawable.russian_flag),
            LanguageItem("es", getString(R.string.lang_spanish), getString(R.string.lang_spanish_native), R.drawable.spanish_flag),
            LanguageItem("th", getString(R.string.lang_thai), getString(R.string.lang_thai_native), R.drawable.thai_flag),
            LanguageItem("tr", getString(R.string.lang_turkish), getString(R.string.lang_turkish_native), R.drawable.turkish_flag),
            LanguageItem("uk", getString(R.string.lang_ukrainian), getString(R.string.lang_ukrainian_native), R.drawable.ukraine_flag),
            LanguageItem("ur", getString(R.string.lang_urdu), getString(R.string.lang_urdu_native), R.drawable.pak_flag),
            LanguageItem("vi", getString(R.string.lang_vietnamese), getString(R.string.lang_vietnamese_native), R.drawable.vietnam),
            LanguageItem("zh", getString(R.string.lang_chinese), getString(R.string.lang_chinese_native), R.drawable.china_flag),
        )

        // Mark active language as selected
        var matched = false
        for (item in list) {
            if (item.code == activeCode || (activeCode == "id" && item.code == "in") || (activeCode == "he" && item.code == "iw")) {
                item.isSelected = true
                matched = true
                break
            }
        }
        if (!matched && list.isNotEmpty()) {
            list[0].isSelected = true
        }

        list
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        val isFromSplash = intent.getBooleanExtra(EXTRA_FROM_SPLASH, false)

        setupHeader(isFromSplash)
        setupRecyclerView()
        setupListeners(isFromSplash)
        setupBackPressHandler()
    }

    private fun setupHeader(isFromSplash: Boolean) {
        if (isFromSplash) {
            binding.topHeader.backButton.visibility = View.GONE
        } else {
            binding.topHeader.backButton.visibility = View.VISIBLE
            binding.topHeader.backButton.setOnClickListener {
                com.universal.remote.ads.AdsController.back(this, "is_show_language_back_inter") {
                    finish()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = LanguageAdapter(languages) { _ ->
            // Selected language item updated in adapter
        }
        binding.rvLanguages.adapter = adapter
    }

    private fun setupListeners(isFromSplash: Boolean) {
        binding.btnApplyLanguage.setOnClickListener {
            val selected = adapter.getSelectedLanguage()

            val targetLocaleCode = when (selected.code.lowercase()) {
                "in" -> "id"
                "iw" -> "he"
                else -> selected.code.lowercase()
            }

            val currentLocales = AppCompatDelegate.getApplicationLocales()
            val currentLangTag = if (!currentLocales.isEmpty) {
                currentLocales.toLanguageTags()
            } else {
                Locale.getDefault().toLanguageTag()
            }

            val currentActiveCode = Locale.forLanguageTag(currentLangTag)
                .language
                .lowercase()

            val isLanguageChanged = currentActiveCode != targetLocaleCode

            val preferences = PreferenceLocalDataSource(this)
            preferences.setLanguageSelected(true)

            if (isLanguageChanged) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(targetLocaleCode)
                )
            }

            com.universal.remote.ads.AdsController.launchInterstitial(
                activity = this,
                flag = "is_show_admob_language_done_btn_inter",
                unitKey = "admob_language_done_btn_inter_id"
            ) {
                if (isFromSplash) {
                    val nextActivity = if (preferences.isOnboardingCompleted()) {
                        MainActivity::class.java
                    } else {
                        OnboardingActivity::class.java
                    }
                    startActivity(Intent(this, nextActivity))
                    finish()
                } else {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("START_DESTINATION", "settingsFragment")
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.topHeader.backButton.visibility == View.VISIBLE) {
                    binding.topHeader.backButton.performClick()
                } else {
                    finish()
                }
            }
        })
    }

    companion object {
        const val EXTRA_FROM_SPLASH = "EXTRA_FROM_SPLASH"
        const val EXTRA_FROM_SETTINGS = "EXTRA_FROM_SETTINGS"
    }
}
