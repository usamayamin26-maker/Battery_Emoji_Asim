package com.asim.jhbatteryapp.presentation.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ActivityOnboardingBinding
import com.asim.jhbatteryapp.databinding.ItemOnboardingPageBinding
import com.asim.jhbatteryapp.presentation.home.MainActivity

// Simple Data Model for Onboarding Screen
data class OnboardingSlide(
    val imageRes: Int,
    val title: String,
    val subtitle: String
)

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    // Easily add or modify your drawable images and strings here!
    private val onboardingSlides by lazy {
        listOf(
            OnboardingSlide(
                imageRes = R.drawable.ob_1, // Replace with your drawable
                title = getString(R.string.onboarding_title_1),
                subtitle = getString(R.string.onboarding_desc_1)
            ),
            OnboardingSlide(
                imageRes = R.drawable.ob_2, // Replace with your drawable
                title = getString(R.string.onboarding_title_2),
                subtitle = getString(R.string.onboarding_desc_2)
            ),
            OnboardingSlide(
                imageRes = R.drawable.ob_3, // Replace with your drawable
                title = getString(R.string.onboarding_title_3),
                subtitle = getString(R.string.onboarding_desc_3)
            ),

        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }

        ViewCompat.requestApplyInsets(binding.root)

        setupViewPager()
        setupListeners()
    }

    private fun setupViewPager() {
        binding.vpOnboarding.adapter = SimpleOnboardingAdapter(onboardingSlides)

        binding.vpOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)

                // Update button text on last slide
                if (position == onboardingSlides.size - 1) {
                   // binding.llPageIndicators.visibility = ViewGroup.GONE
                    binding.btnGetStarted.text = getString(R.string.get_started)
                } else {
                    binding.btnGetStarted.text = getString(R.string.next)
                }
            }
        })
    }

    private fun setupListeners() {
        // Skip Button -> Directly navigate to Home
        binding.tvSkip.setOnClickListener {
            com.universal.remote.ads.AdsController.launchInterstitial(
                activity = this,
                flag = "is_show_admob_onboarding_skip_btn_inter"
            ) {
                navigateToHome()
            }
        }

        binding.btnGetStarted.setOnClickListener {
            val currentItem = binding.vpOnboarding.currentItem
            if (currentItem < onboardingSlides.size - 1) {
                binding.vpOnboarding.setCurrentItem(currentItem + 1, true)
            } else {
                com.universal.remote.ads.AdsController.launchInterstitial(
                    activity = this,
                    flag = "is_show_admob_onboarding_get_started_inter",
                    unitKey = "admob_onboarding_get_started_inter_id"
                ) {
                    navigateToHome()
                }
            }
        }
    }

    private fun navigateToHome() {
        com.asim.jhbatteryapp.data.datasource.PreferenceLocalDataSource(this).setOnboardingCompleted(true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun updateIndicators(selectedIndex: Int) {
        val activeWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._18sdp)
        val inactiveWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._5sdp)
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        for (i in dots.indices) {
            val dot = dots[i]
            val lp = dot.layoutParams
            if (i == selectedIndex) {
                dot.setBackgroundResource(R.drawable.bg_indicator_active)
                lp.width = activeWidth
            } else {
                dot.setBackgroundResource(R.drawable.bg_indicator_inactive)
                lp.width = inactiveWidth
            }
            dot.layoutParams = lp
        }
    }

    // Simple Adapter for Onboarding ViewPager2
    private inner class SimpleOnboardingAdapter(
        private val slides: List<OnboardingSlide>
    ) : RecyclerView.Adapter<SimpleOnboardingAdapter.SlideViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
            val itemBinding = ItemOnboardingPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return SlideViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
            holder.bind(slides[position])
        }

        override fun getItemCount(): Int = slides.size

        inner class SlideViewHolder(
            private val itemBinding: ItemOnboardingPageBinding
        ) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(slide: OnboardingSlide) {
                itemBinding.ivOnboardingImage.setImageResource(slide.imageRes)
                itemBinding.tvOnboardingTitle.text = slide.title
                itemBinding.tvOnboardingSubtitle.text = slide.subtitle
            }
        }
    }
}
