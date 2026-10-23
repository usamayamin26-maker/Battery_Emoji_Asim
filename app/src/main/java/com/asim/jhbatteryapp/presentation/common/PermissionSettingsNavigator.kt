package com.asim.jhbatteryapp.presentation.common

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.asim.jhbatteryapp.JHBatteryApplication

/**
 * Opens the system screens the user needs for Accessibility, using public Settings intents only.
 * Nothing here grants anything: the user always makes the change themselves.
 */
object PermissionSettingsNavigator {

    fun openAccessibilitySettings(activity: Activity): Boolean =
        launch(activity, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    /** This app's App Info page, where Android 13+ shows ⋮ → "Allow restricted settings". */
    fun openAppInfo(activity: Activity): Boolean {
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", activity.packageName, null))
        return launch(activity, appDetails) ||
                launch(activity, Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
    }

    private fun launch(activity: Activity, intent: Intent): Boolean {
        // Coming back from Settings is not a real return from background: no Welcome screen / ad
        JHBatteryApplication.skipNextWelcome()
        return try {
            activity.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
