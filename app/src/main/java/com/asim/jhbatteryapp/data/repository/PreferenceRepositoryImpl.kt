package com.asim.jhbatteryapp.data.repository

import com.asim.jhbatteryapp.data.datasource.PreferenceLocalDataSource
import com.asim.jhbatteryapp.domain.repository.PreferenceRepository

class PreferenceRepositoryImpl(
    private val preferenceLocalDataSource: PreferenceLocalDataSource
) : PreferenceRepository {

    override fun isOnboardingCompleted(): Boolean {
        return preferenceLocalDataSource.isOnboardingCompleted()
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        preferenceLocalDataSource.setOnboardingCompleted(completed)
    }

    override fun isEmojiBatteryEnabled(): Boolean {
        return preferenceLocalDataSource.isEmojiBatteryEnabled()
    }

    override fun setEmojiBatteryEnabled(enabled: Boolean) {
        preferenceLocalDataSource.setEmojiBatteryEnabled(enabled)
    }
}
