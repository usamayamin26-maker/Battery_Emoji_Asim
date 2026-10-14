package com.asim.jhbatteryapp.presentation.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.asim.jhbatteryapp.data.datasource.BatteryLocalDataSource
import com.asim.jhbatteryapp.data.datasource.PreferenceLocalDataSource
import com.asim.jhbatteryapp.data.repository.BatteryRepositoryImpl
import com.asim.jhbatteryapp.data.repository.PreferenceRepositoryImpl
import com.asim.jhbatteryapp.domain.usecase.GetBatteryEmojisUseCase
import com.asim.jhbatteryapp.domain.usecase.GetCategoriesUseCase
import com.asim.jhbatteryapp.domain.usecase.GetQuickActionsUseCase
import com.asim.jhbatteryapp.domain.usecase.SearchEmojisUseCase
import com.asim.jhbatteryapp.domain.usecase.ToggleEmojiBatteryUseCase
import com.asim.jhbatteryapp.presentation.home.HomeViewModel

class ViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    private val batteryLocalDataSource by lazy { BatteryLocalDataSource() }
    private val preferenceLocalDataSource by lazy { PreferenceLocalDataSource(context.applicationContext) }

    private val batteryRepository by lazy { BatteryRepositoryImpl(batteryLocalDataSource) }
    private val preferenceRepository by lazy { PreferenceRepositoryImpl(preferenceLocalDataSource) }

    // UseCases for Home MVVM
    private val getBatteryEmojisUseCase by lazy { GetBatteryEmojisUseCase(batteryRepository) }
    private val getCategoriesUseCase by lazy { GetCategoriesUseCase(batteryRepository) }
    private val getQuickActionsUseCase by lazy { GetQuickActionsUseCase(batteryRepository) }
    private val searchEmojisUseCase by lazy { SearchEmojisUseCase(batteryRepository) }
    private val toggleEmojiBatteryUseCase by lazy { ToggleEmojiBatteryUseCase(preferenceRepository) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    getCategoriesUseCase,
                    getBatteryEmojisUseCase,
                    getQuickActionsUseCase,
                    searchEmojisUseCase,
                    toggleEmojiBatteryUseCase
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
