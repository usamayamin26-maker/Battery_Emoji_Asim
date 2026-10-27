package com.jhw.batteryemoji.di

import com.jhw.batteryemoji.data.repository.AccessibilityPermissionRepositoryImpl
import com.jhw.batteryemoji.domain.repository.AccessibilityPermissionRepository
import com.jhw.batteryemoji.domain.usecase.GetAccessibilityPermissionStatusUseCase
import com.jhw.batteryemoji.presentation.guide.AccessibilityPermissionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<AccessibilityPermissionRepository> { AccessibilityPermissionRepositoryImpl(androidContext()) }
    factory { GetAccessibilityPermissionStatusUseCase(get()) }
    viewModel { AccessibilityPermissionViewModel(get()) }
}
