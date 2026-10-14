package com.asim.jhbatteryapp.domain.usecase

import com.asim.jhbatteryapp.domain.model.CategoryItem
import com.asim.jhbatteryapp.domain.repository.BatteryRepository

class GetCategoriesUseCase(
    private val repository: BatteryRepository
) {
    operator fun invoke(): List<CategoryItem> {
        return repository.getCategories()
    }
}
