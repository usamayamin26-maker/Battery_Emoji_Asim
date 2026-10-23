package com.asim.jhbatteryapp.domain.usecase

import com.asim.jhbatteryapp.domain.model.AccessibilityPermissionStatus
import com.asim.jhbatteryapp.domain.repository.AccessibilityPermissionRepository

class GetAccessibilityPermissionStatusUseCase(
    private val repository: AccessibilityPermissionRepository
) {
    operator fun invoke(): AccessibilityPermissionStatus = repository.getStatus()
}
