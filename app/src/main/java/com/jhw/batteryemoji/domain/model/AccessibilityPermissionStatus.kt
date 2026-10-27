package com.jhw.batteryemoji.domain.model

/**
 * Where the app's Accessibility service stands, as far as public Android APIs can tell.
 *
 * @param isServiceEnabled the user has turned the service on in Accessibility settings.
 * @param mayNeedRestrictedSettings Android 13+ and the app was installed from a file rather than an
 *   app store, so the system may grey out the switch until the user taps "Allow restricted settings"
 *   in App Info. Android has no API that says whether that block is active, so this is a best guess.
 */
data class AccessibilityPermissionStatus(
    val isServiceEnabled: Boolean,
    val mayNeedRestrictedSettings: Boolean
)
