package com.asim.jhbatteryapp.data.datasource

import android.content.Context
import android.content.SharedPreferences

class PreferenceLocalDataSource(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isEmojiBatteryEnabled(): Boolean {
        return prefs.getBoolean(KEY_EMOJI_BATTERY_ENABLED, true)
    }

    fun setEmojiBatteryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EMOJI_BATTERY_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "jh_battery_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_EMOJI_BATTERY_ENABLED = "key_emoji_battery_enabled"
    }
}
