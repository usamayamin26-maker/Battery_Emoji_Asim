package com.asim.jhbatteryapp.domain.usecase

import com.asim.jhbatteryapp.domain.model.OnboardingPageItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository

class GetOnboardingPagesUseCase(
    private val repository: BatteryRepository
) {
    operator fun invoke(): List<OnboardingPageItem> {
        return repository.getOnboardingPages()
    }
}
