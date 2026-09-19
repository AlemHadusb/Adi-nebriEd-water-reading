package com.example.security

import android.content.Context
import android.content.Intent
import android.provider.Settings

data class TimeIntegrityStatus(
    val isAutoTimeEnabled: Boolean,
    val isAutoTimeZoneEnabled: Boolean,
    val isTampered: Boolean,
    val violationMessage: String?
)

object TimeIntegrityManager {

    @Volatile
    var bypassTamperCheckForDev: Boolean = false

    fun checkDeviceTimeIntegrity(context: Context): TimeIntegrityStatus {
        if (bypassTamperCheckForDev) {
            return TimeIntegrityStatus(
                isAutoTimeEnabled = true,
                isAutoTimeZoneEnabled = true,
                isTampered = false,
                violationMessage = null
            )
        }

        return try {
            val contentResolver = context.contentResolver
            val autoTime = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 0)
            val autoTimeZone = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, 0)

            val isAutoTime = autoTime == 1
            val isAutoTimeZone = autoTimeZone == 1
            val isTampered = !isAutoTime || !isAutoTimeZone

            val message = if (isTampered) {
                "Security Policy Violation: Automatic Date & Time has been disabled. The Adi NebriEd Water System is locked until automatic network time and time zone are re-enabled in Android system settings."
            } else {
                null
            }

            TimeIntegrityStatus(
                isAutoTimeEnabled = isAutoTime,
                isAutoTimeZoneEnabled = isAutoTimeZone,
                isTampered = isTampered,
                violationMessage = message
            )
        } catch (e: Exception) {
            // In case of restricted access or test environments
            TimeIntegrityStatus(
                isAutoTimeEnabled = true,
                isAutoTimeZoneEnabled = true,
                isTampered = false,
                violationMessage = null
            )
        }
    }

    fun openDateSettingsIntent(): Intent {
        return Intent(Settings.ACTION_DATE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
