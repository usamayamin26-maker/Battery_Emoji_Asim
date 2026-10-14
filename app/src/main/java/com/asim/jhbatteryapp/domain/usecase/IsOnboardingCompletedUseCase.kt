package com.asim.jhbatteryapp.domain.usecase

import com.asim.jhbatteryapp.domain.repository.PreferenceRepository

class IsOnboardingCompletedUseCase(
    private val repository: PreferenceRepository
) {
    operator fun invoke(): Boolean {
        return repository.isOnboardingCompleted()
    }
}
