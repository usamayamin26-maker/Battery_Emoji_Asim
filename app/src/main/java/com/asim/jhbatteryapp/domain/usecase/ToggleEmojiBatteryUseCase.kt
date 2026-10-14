package com.asim.jhbatteryapp.domain.usecase

import com.asim.jhbatteryapp.domain.repository.PreferenceRepository

class ToggleEmojiBatteryUseCase(
    private val repository: PreferenceRepository
) {
    fun isEnabled(): Boolean = repository.isEmojiBatteryEnabled()

    operator fun invoke(enabled: Boolean) {
        repository.setEmojiBatteryEnabled(enabled)
    }
}
