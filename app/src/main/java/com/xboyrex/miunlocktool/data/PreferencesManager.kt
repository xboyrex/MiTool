package com.xboyrex.miunlocktool.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "miunlocktool_xboyrex_preferences"
        private const val KEY_TOKEN = "key_token"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_SSECURITY = "key_ssecurity"
        private const val KEY_PASS_TOKEN = "key_pass_token"
        private const val KEY_DEVICE_ID = "key_device_id"
        private const val KEY_UNLOCK_ZONE = "key_unlock_zone"
        private const val KEY_LANG = "key_lang"
        private const val KEY_NTP_SERVER = "key_ntp_server"
        private const val KEY_BURST_COUNT = "key_burst_count"
        private const val KEY_BURST_INTERVAL = "key_burst_interval"
        private const val KEY_MODE = "key_execution_mode"
        private const val KEY_TIMING_PROFILE = "key_timing_profile"
        private const val KEY_CUSTOM_TIMINGS = "key_custom_timings"
        private const val KEY_HYPER_ISLAND = "key_hyper_island"
        private const val KEY_AUTO_NTP = "key_auto_ntp"
        private const val KEY_AUTO_PING = "key_auto_ping"

        const val LANG_SYSTEM = "system"
        const val LANG_EN = "en"
        const val LANG_HI = "hi"
        const val LANG_RU = "ru"
        const val LANG_UK = "uk"
        const val LANG_ES = "es"
        const val LANG_PT = "pt"
        const val LANG_ID = "id"
        const val LANG_ZH = "zh"

        const val DEFAULT_NTP = "time.google.com"
        const val DEFAULT_BURST_COUNT = 20
        const val DEFAULT_BURST_INTERVAL_MS = 100L

        const val PROFILE_EUROPE = "europe"
        const val PROFILE_ASIA = "asia"
        const val PROFILE_USA_AFRICA = "usa_africa"
        const val PROFILE_CUSTOM = "custom"

        val EUROPE_TIMINGS = listOf(58.700, 58.800, 59.100, 59.300, 59.500, 59.800)
        val ASIA_TIMINGS = listOf(58.800, 59.100, 59.300, 59.500, 59.700)
        val USA_AFRICA_TIMINGS = listOf(58.500, 58.800, 59.100, 59.300, 59.500, 59.700)

        fun parseTimings(input: String): List<Double> {
            val list = mutableListOf<Double>()
            val parts = input.split(',', ';', ' ', '\n', '\t')
            for (p in parts) {
                val clean = p.trim().replace(':', '.')
                val num = clean.toDoubleOrNull()
                if (num != null && num in 0.0..60.0) {
                    list.add(Math.round(num * 1000.0) / 1000.0)
                }
            }
            return if (list.isNotEmpty()) list.sorted() else ASIA_TIMINGS
        }
    }

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value.trim()).apply()

    var userId: String
        get() = prefs.getString(KEY_USER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ID, value.trim()).apply()

    var ssecurity: String
        get() = prefs.getString(KEY_SSECURITY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SSECURITY, value.trim()).apply()

    var passToken: String
        get() = prefs.getString(KEY_PASS_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASS_TOKEN, value.trim()).apply()

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value.trim()).apply()

    var unlockZone: String
        get() = prefs.getString(KEY_UNLOCK_ZONE, "India") ?: "India"
        set(value) = prefs.edit().putString(KEY_UNLOCK_ZONE, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANG, LANG_EN) ?: LANG_EN
        set(value) = prefs.edit().putString(KEY_LANG, value).apply()

    var ntpServer: String
        get() = prefs.getString(KEY_NTP_SERVER, DEFAULT_NTP) ?: DEFAULT_NTP
        set(value) = prefs.edit().putString(KEY_NTP_SERVER, value).apply()

    var burstCount: Int
        get() = prefs.getInt(KEY_BURST_COUNT, DEFAULT_BURST_COUNT)
        set(value) = prefs.edit().putInt(KEY_BURST_COUNT, value).apply()

    var burstIntervalMs: Long
        get() = prefs.getLong(KEY_BURST_INTERVAL, DEFAULT_BURST_INTERVAL_MS)
        set(value) = prefs.edit().putLong(KEY_BURST_INTERVAL, value).apply()

    var executionMode: Int
        get() = prefs.getInt(KEY_MODE, ExecutionMode.AUTO.id)
        set(value) = prefs.edit().putInt(KEY_MODE, value).apply()

    var timingProfile: String
        get() = prefs.getString(KEY_TIMING_PROFILE, PROFILE_ASIA) ?: PROFILE_ASIA
        set(value) = prefs.edit().putString(KEY_TIMING_PROFILE, value).apply()

    var customTimings: String
        get() = prefs.getString(KEY_CUSTOM_TIMINGS, "58.800, 59.100, 59.300, 59.500, 59.700")
            ?: "58.800, 59.100, 59.300, 59.500, 59.700"
        set(value) = prefs.edit().putString(KEY_CUSTOM_TIMINGS, value).apply()

    var hyperIslandEnabled: Boolean
        get() = prefs.getBoolean(KEY_HYPER_ISLAND, true)
        set(value) = prefs.edit().putBoolean(KEY_HYPER_ISLAND, value).apply()

    var autoNtpEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_NTP, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_NTP, value).apply()

    var autoPingEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PING, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PING, value).apply()

    fun getActiveTimings(): List<Double> {
        return when (timingProfile) {
            PROFILE_EUROPE -> EUROPE_TIMINGS
            PROFILE_USA_AFRICA -> USA_AFRICA_TIMINGS
            PROFILE_CUSTOM -> parseTimings(customTimings)
            else -> ASIA_TIMINGS
        }
    }
}
