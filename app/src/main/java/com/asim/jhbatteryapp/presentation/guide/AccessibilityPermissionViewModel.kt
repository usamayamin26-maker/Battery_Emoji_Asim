package com.asim.jhbatteryapp.presentation.guide

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.asim.jhbatteryapp.domain.model.AccessibilityPermissionStatus
import com.asim.jhbatteryapp.domain.usecase.GetAccessibilityPermissionStatusUseCase

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
