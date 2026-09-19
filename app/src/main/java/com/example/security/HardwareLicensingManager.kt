package com.example.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class LicenseState(
    val isLicensed: Boolean,
    val androidId: String,
    val stationName: String,
    val expiryTimestamp: Long,
    val activeKey: String?,
    val isExpired: Boolean
)

object HardwareLicensingManager {

    private const val PREFS_NAME = "adi_nebried_license_store"
    private const val KEY_IS_LICENSED = "is_licensed"
    private const val KEY_EXPIRY_TS = "expiry_ts"
    private const val KEY_STATION_NAME = "station_name"
    private const val KEY_ACTIVE_KEY = "active_key"
    private const val KEY_BYPASS_MODE = "bypass_mode"

    const val SECURE_SALT = "AdiNebriEd@2026_SecureSalt"
    const val DEFAULT_STATION = "Adi NebriEd"

    @SuppressLint("HardwareIds")
    fun getHardwareAndroidId(context: Context): String {
        return try {
            val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (id.isNullOrBlank()) "EMU_ADI_NEBRIED_DEV_01" else id
        } catch (e: Exception) {
            "EMU_ADI_NEBRIED_DEV_01"
        }
    }

    fun generateActivationKey(androidId: String, expiryTimestamp: Long): String {
        val payload = "$androidId$SECURE_SALT$expiryTimestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(payload.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isLicenseValid(context: Context): LicenseState {
        val prefs = getPrefs(context)
        val androidId = getHardwareAndroidId(context)
        val bypass = prefs.getBoolean(KEY_BYPASS_MODE, false)

        if (bypass) {
            val futureExpiry = System.currentTimeMillis() + (365L * 24 * 3600 * 1000)
            return LicenseState(
                isLicensed = true,
                androidId = androidId,
                stationName = prefs.getString(KEY_STATION_NAME, DEFAULT_STATION) ?: DEFAULT_STATION,
                expiryTimestamp = futureExpiry,
                activeKey = "DEV_OVERRIDE_ACTIVE",
                isExpired = false
            )
        }

        val isLicensed = prefs.getBoolean(KEY_IS_LICENSED, false)
        val expiryTs = prefs.getLong(KEY_EXPIRY_TS, 0L)
        val activeKey = prefs.getString(KEY_ACTIVE_KEY, null)
        val station = prefs.getString(KEY_STATION_NAME, DEFAULT_STATION) ?: DEFAULT_STATION

        val now = System.currentTimeMillis()
        val isExpired = now > expiryTs

        if (!isLicensed || activeKey.isNullOrBlank() || isExpired) {
            return LicenseState(
                isLicensed = false,
                androidId = androidId,
                stationName = station,
                expiryTimestamp = expiryTs,
                activeKey = activeKey,
                isExpired = isExpired && isLicensed
            )
        }

        // Validate cryptographically
        val expectedKey = generateActivationKey(androidId, expiryTs)
        val isValidKey = activeKey.equals(expectedKey, ignoreCase = true)

        return LicenseState(
            isLicensed = isValidKey,
            androidId = androidId,
            stationName = station,
            expiryTimestamp = expiryTs,
            activeKey = activeKey,
            isExpired = false
        )
    }

    fun activateLicense(
        context: Context,
        enteredKey: String,
        expiryTimestamp: Long,
        stationName: String = DEFAULT_STATION
    ): Boolean {
        val androidId = getHardwareAndroidId(context)
        val expected = generateActivationKey(androidId, expiryTimestamp)
        if (enteredKey.trim().equals(expected, ignoreCase = true)) {
            val now = System.currentTimeMillis()
            if (expiryTimestamp > now) {
                getPrefs(context).edit()
                    .putBoolean(KEY_IS_LICENSED, true)
                    .putLong(KEY_EXPIRY_TS, expiryTimestamp)
                    .putString(KEY_ACTIVE_KEY, enteredKey.trim())
                    .putString(KEY_STATION_NAME, stationName)
                    .putBoolean(KEY_BYPASS_MODE, false)
                    .apply()
                return true
            }
        }
        return false
    }

    fun setEmergencyBypass(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BYPASS_MODE, enabled).apply()
    }

    fun isBypassActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BYPASS_MODE, false)
    }

    fun clearLicense(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    fun generateQuickKeyForDevice(context: Context, daysValid: Int = 365): Pair<Long, String> {
        val androidId = getHardwareAndroidId(context)
        val expiryTs = System.currentTimeMillis() + (daysValid.toLong() * 24 * 3600 * 1000)
        val key = generateActivationKey(androidId, expiryTs)
        return Pair(expiryTs, key)
    }
}
