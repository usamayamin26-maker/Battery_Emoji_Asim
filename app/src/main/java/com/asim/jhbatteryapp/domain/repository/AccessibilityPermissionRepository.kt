package com.asim.jhbatteryapp.domain.repository

import com.asim.jhbatteryapp.domain.model.AccessibilityPermissionStatus

interface AccessibilityPermissionRepository {
    fun getStatus(): AccessibilityPermissionStatus
}
