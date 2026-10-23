package com.asim.jhbatteryapp.di

import com.asim.jhbatteryapp.data.repository.AccessibilityPermissionRepositoryImpl
import com.asim.jhbatteryapp.domain.repository.AccessibilityPermissionRepository
import com.asim.jhbatteryapp.domain.usecase.GetAccessibilityPermissionStatusUseCase
import com.asim.jhbatteryapp.presentation.guide.AccessibilityPermissionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<AccessibilityPermissionRepository> { AccessibilityPermissionRepositoryImpl(androidContext()) }
    factory { GetAccessibilityPermissionStatusUseCase(get()) }
    viewModel { AccessibilityPermissionViewModel(get()) }
}
