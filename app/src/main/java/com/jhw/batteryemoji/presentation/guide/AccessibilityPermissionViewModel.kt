package com.jhw.batteryemoji.presentation.guide

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jhw.batteryemoji.domain.model.AccessibilityPermissionStatus
import com.jhw.batteryemoji.domain.usecase.GetAccessibilityPermissionStatusUseCase

class AccessibilityPermissionViewModel(
    private val getStatus: GetAccessibilityPermissionStatusUseCase
) : ViewModel() {

    private val _status = MutableLiveData<AccessibilityPermissionStatus>()
    val status: LiveData<AccessibilityPermissionStatus> = _status

    /** Re-reads the system state; call from onResume so a return from Settings updates the screen. */
    fun refresh() {
        _status.value = getStatus()
    }
}
