package com.asim.jhbatteryapp.domain.usecase

import com.asim.jhbatteryapp.domain.repository.PreferenceRepository

class SetOnboardingCompletedUseCase(
    private val repository: PreferenceRepository
) {
    operator fun invoke(completed: Boolean) {
        repository.setOnboardingCompleted(completed)
    }
}
