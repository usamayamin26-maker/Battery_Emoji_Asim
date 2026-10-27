package com.jhw.batteryemoji.domain.usecase

import com.jhw.batteryemoji.domain.model.AccessibilityPermissionStatus
import com.jhw.batteryemoji.domain.repository.AccessibilityPermissionRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAccessibilityPermissionStatusUseCaseTest {

    private class FakeRepository(var current: AccessibilityPermissionStatus) : AccessibilityPermissionRepository {
        override fun getStatus() = current
    }

    @Test
    fun `returns the repository status on every call`() {
        val repository = FakeRepository(AccessibilityPermissionStatus(isServiceEnabled = false, mayNeedRestrictedSettings = true))
        val useCase = GetAccessibilityPermissionStatusUseCase(repository)

        assertEquals(AccessibilityPermissionStatus(false, true), useCase())

        // User enabled the service in Settings and came back: the next read reflects it
        repository.current = AccessibilityPermissionStatus(isServiceEnabled = true, mayNeedRestrictedSettings = true)
        assertEquals(AccessibilityPermissionStatus(true, true), useCase())
    }
}
