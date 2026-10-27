package com.jhw.batteryemoji.data.repository

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.jhw.batteryemoji.domain.model.AccessibilityPermissionStatus
import com.jhw.batteryemoji.domain.repository.AccessibilityPermissionRepository
import com.jhw.batteryemoji.service.StatusBarAccessibilityService

/**
 * Reads the Accessibility state through public APIs only: [AccessibilityManager] first, then the
 * documented [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES] list as a fallback for OEMs that are
 * slow to update the manager after the user flips the switch.
 */
class AccessibilityPermissionRepositoryImpl(
    private val context: Context
) : AccessibilityPermissionRepository {

    private val serviceComponent = ComponentName(context, StatusBarAccessibilityService::class.java)

    override fun getStatus() = AccessibilityPermissionStatus(
        isServiceEnabled = isServiceEnabled(),
        mayNeedRestrictedSettings = mayNeedRestrictedSettings()
    )

    private fun isServiceEnabled(): Boolean {
        if (StatusBarAccessibilityService.isRunning) return true

        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledInManager = manager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .orEmpty()
            .any { info ->
                val serviceInfo = info.resolveInfo?.serviceInfo ?: return@any false
                ComponentName(serviceInfo.packageName, serviceInfo.name) == serviceComponent
            }
        if (enabledInManager) return true

        val enabledSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledSetting.split(':').any { ComponentName.unflattenFromString(it) == serviceComponent }
    }

    /**
     * Android 13 restricts Accessibility for apps installed from a downloaded or local APK file.
     * [PackageInstaller.PACKAGE_SOURCE_STORE] and [PackageInstaller.PACKAGE_SOURCE_OTHER] (e.g. adb)
     * installs are not restricted; anything unknown is treated as "may be restricted" so the guide
     * shows rather than leaving the user stuck.
     */
    private fun mayNeedRestrictedSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val source = try {
            context.packageManager.getInstallSourceInfo(context.packageName).packageSource
        } catch (_: Exception) {
            return true
        }
        return source != PackageInstaller.PACKAGE_SOURCE_STORE &&
                source != PackageInstaller.PACKAGE_SOURCE_OTHER
    }
}
