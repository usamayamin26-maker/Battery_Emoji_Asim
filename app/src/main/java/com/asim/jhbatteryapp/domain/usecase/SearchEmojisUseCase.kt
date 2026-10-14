package com.asim.jhbatteryapp.domain.usecase

import com.asim.jhbatteryapp.domain.model.BatteryEmojiItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository

class SearchEmojisUseCase(
    private val repository: BatteryRepository
) {
    operator fun invoke(query: String): List<BatteryEmojiItem> {
        return repository.searchEmojis(query)
    }
}
