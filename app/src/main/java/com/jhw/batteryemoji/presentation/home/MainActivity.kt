package com.jhw.batteryemoji.presentation.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.jhw.batteryemoji.R
import com.jhw.batteryemoji.databinding.ActivityMainBinding
import com.jhw.batteryemoji.presentation.components.FloatingBottomBarView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomInset)
            insets
        }

        ViewCompat.requestApplyInsets(binding.root)

        setupNavigation()
        setupBottomNav()
        setupBackPressHandler()

        // Handle specific destination on launch/restart (e.g. after language change)
        val startDest = intent.getStringExtra("START_DESTINATION")
        if (startDest == "settingsFragment") {
            navController.navigate(R.id.settingsFragment)
        }

        // Restore floating screen sticker & foreground service if previously enabled
        val prefRepo = com.jhw.batteryemoji.data.repository.PreferenceRepositoryImpl()
        val hasOverlay = com.jhw.batteryemoji.presentation.common.StatusBarPermissionHelper.hasOverlayPermission(this)
        val isAccessibilityEnabled = com.jhw.batteryemoji.service.StatusBarAccessibilityService.isAccessibilityServiceEnabled(this)

        if (prefRepo.isFloatingStickerEnabled() && (hasOverlay || isAccessibilityEnabled)) {
            com.jhw.batteryemoji.service.FloatingStickerService.start(this)
        }

        // Foreground service: draws the bar when accessibility is off, and otherwise keeps the
        // process alive so the accessibility overlay survives the app being swiped away
        if ((isAccessibilityEnabled || hasOverlay) && (prefRepo.isEmojiBatteryEnabled() || prefRepo.isFloatingStickerEnabled())) {
            com.jhw.batteryemoji.service.StatusBarOverlayService.start(this)
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id == R.id.homeFragment) {
                    com.jhw.batteryemoji.core.Utils.MyUtils.showExitBottomSheet(
                        context = this@MainActivity,
                        onExit = {
                            finish()
                        }
                    )
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Control bottom nav visibility and active tab based on current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    showBottomNav(FloatingBottomBarView.Tab.TRENDING)
                }
                R.id.settingsFragment -> {
                    showBottomNav(FloatingBottomBarView.Tab.SETTING)
                }
                else -> {
                    hideBottomNav()
                }
            }
        }
    }

    private fun showBottomNav(tab: FloatingBottomBarView.Tab) {
        binding.floatingBottomNav.selectTabSilently(tab)
        if (binding.floatingBottomNav.visibility != View.VISIBLE) {
            binding.floatingBottomNav.animate().cancel()
            val height = binding.floatingBottomNav.height.toFloat().takeIf { it > 0 } ?: 250f
            binding.floatingBottomNav.translationY = height + 40f
            binding.floatingBottomNav.alpha = 1f
            binding.floatingBottomNav.visibility = View.VISIBLE
            binding.floatingBottomNav.animate()
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    private fun hideBottomNav() {
        if (binding.floatingBottomNav.visibility == View.VISIBLE) {
            binding.floatingBottomNav.animate().cancel()
            val height = binding.floatingBottomNav.height.toFloat().takeIf { it > 0 } ?: 250f
            binding.floatingBottomNav.animate()
                .translationY(height + 40f)
                .setDuration(180)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction {
                    binding.floatingBottomNav.visibility = View.GONE
                }
                .start()
        }
    }

    private fun setupBottomNav() {
        // Initialize frosted liquid glass blur over content
        binding.floatingBottomNav.setupBlur(binding.root)

        binding.floatingBottomNav.setOnTabSelectedListener { tab ->
            when (tab) {
                FloatingBottomBarView.Tab.TRENDING -> {
                    if (navController.currentDestination?.id != R.id.homeFragment) {
                        navController.popBackStack(R.id.homeFragment, false)
                    }
                    viewModel.selectCategory("hot")
                }
                FloatingBottomBarView.Tab.CUSTOMIZE -> {
                    if (navController.currentDestination?.id != R.id.statusBarCustomFragment) {
                        com.universal.remote.ads.AdsController.interstitial(
                            activity = this@MainActivity,
                            flag = "is_show_home_btn_inter",
                            counter = com.universal.remote.ads.AdsController.Counter.BOTTOM_NAV
                        ) {
                            navController.navigate(R.id.statusBarCustomFragment)
                        }
                    }
                }
                FloatingBottomBarView.Tab.SETTING -> {
                    if (navController.currentDestination?.id != R.id.settingsFragment) {
                        com.universal.remote.ads.AdsController.interstitial(
                            activity = this@MainActivity,
                            flag = "is_show_home_btn_inter",
                            counter = com.universal.remote.ads.AdsController.Counter.BOTTOM_NAV
                        ) {
                            navController.navigate(R.id.settingsFragment)
                        }
                    }
                }
            }
        }
    }

    override fun recreate() {
        super.recreate()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
