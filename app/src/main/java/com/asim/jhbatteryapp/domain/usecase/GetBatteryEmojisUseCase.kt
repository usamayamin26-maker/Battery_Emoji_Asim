package com.asim.jhbatteryapp.domain.usecase

import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository

class GetBatteryEmojisUseCase(
    private val repository: BatteryRepository
) {
    operator fun invoke(categoryId: String? = null): List<BatteryEmojiItem> {
        return repository.getBatteryEmojis(categoryId)
    }
}
