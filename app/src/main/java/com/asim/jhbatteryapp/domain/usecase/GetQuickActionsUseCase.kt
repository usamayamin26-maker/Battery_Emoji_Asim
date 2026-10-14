package com.asim.jhbatteryapp.domain.usecase

import com.asim.jhbatteryapp.domain.model.QuickActionItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository

class GetQuickActionsUseCase(
    private val repository: BatteryRepository
) {
    operator fun invoke(): List<QuickActionItem> {
        return repository.getQuickActions()
    }
}
