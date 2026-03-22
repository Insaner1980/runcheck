package com.runcheck.domain.model

data class UserPreferences(
    val monitoringInterval: MonitoringInterval = MonitoringInterval.THIRTY,
    val notificationsEnabled: Boolean = true,
    val dataRetention: DataRetention = DataRetention.THREE_MONTHS,
    val crashReportingEnabled: Boolean = false,
    // Per-notification toggles
    val notifLowBattery: Boolean = true,
    val notifHighTemp: Boolean = true,
    val notifLowStorage: Boolean = true,
    val notifChargeComplete: Boolean = false,
    // Alert thresholds
    val alertBatteryThreshold: Int = 20,
    val alertTempThreshold: Int = 42,
    val alertStorageThreshold: Int = 90,
    // Display
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    // Live notification
    val liveNotificationEnabled: Boolean = false,
    val liveNotifCurrent: Boolean = true,
    val liveNotifDrainRate: Boolean = true,
    val liveNotifTemperature: Boolean = true,
    val liveNotifScreenStats: Boolean = false,
    val liveNotifRemainingTime: Boolean = false
)

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    FINNISH;

    fun toLanguageTag(): String? = when (this) {
        SYSTEM -> null
        ENGLISH -> "en"
        FINNISH -> "fi"
    }
}
