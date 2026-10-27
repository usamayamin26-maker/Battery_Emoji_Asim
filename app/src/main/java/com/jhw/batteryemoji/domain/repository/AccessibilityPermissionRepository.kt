package com.jhw.batteryemoji.domain.repository

import com.jhw.batteryemoji.domain.model.AccessibilityPermissionStatus

interface AccessibilityPermissionRepository {
    fun getStatus(): AccessibilityPermissionStatus
}
