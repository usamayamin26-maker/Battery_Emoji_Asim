package com.asim.jhbatteryapp.presentation.home

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
import com.asim.jhbatteryapp.R
import com.asim.jhbatteryapp.databinding.ActivityMainBinding
import com.asim.jhbatteryapp.presentation.common.ViewModelFactory
import com.asim.jhbatteryapp.presentation.components.FloatingBottomBarView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: HomeViewModel by viewModels { ViewModelFactory(this) }

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
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Control bottom nav visibility and active tab based on current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    binding.floatingBottomNav.visibility = View.VISIBLE
                    binding.floatingBottomNav.selectTabSilently(FloatingBottomBarView.Tab.TRENDING)
                }
                R.id.settingsFragment -> {
                    binding.floatingBottomNav.visibility = View.VISIBLE
                    binding.floatingBottomNav.selectTabSilently(FloatingBottomBarView.Tab.SETTING)
                }
                R.id.searchFragment, R.id.languageFragment -> {
                    binding.floatingBottomNav.visibility = View.GONE
                }
                else -> {
                    binding.floatingBottomNav.visibility = View.VISIBLE
                }
            }
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
                    Toast.makeText(this, "Customize Section", Toast.LENGTH_SHORT).show()
                }
                FloatingBottomBarView.Tab.SETTING -> {
                    if (navController.currentDestination?.id != R.id.settingsFragment) {
                        navController.navigate(R.id.settingsFragment)
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
