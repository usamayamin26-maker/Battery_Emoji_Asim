package com.jhw.batteryemoji.domain.usecase

import com.jhw.batteryemoji.domain.model.AccessibilityPermissionStatus
import com.jhw.batteryemoji.domain.repository.AccessibilityPermissionRepository

class GetAccessibilityPermissionStatusUseCase(
    private val repository: AccessibilityPermissionRepository
) {
    operator fun invoke(): AccessibilityPermissionStatus = repository.getStatus()
}
