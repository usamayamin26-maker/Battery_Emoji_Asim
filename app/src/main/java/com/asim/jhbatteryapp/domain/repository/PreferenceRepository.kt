package com.asim.jhbatteryapp.domain.repository

interface PreferenceRepository {
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
    fun isEmojiBatteryEnabled(): Boolean
    fun setEmojiBatteryEnabled(enabled: Boolean)
}
