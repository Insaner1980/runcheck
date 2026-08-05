package com.runcheck.domain.model

data class UserPreferences(
    val monitoringInterval: MonitoringInterval = MonitoringInterval.THIRTY,
    val notificationsEnabled: Boolean = true,
    val dataRetention: DataRetention = DataRetention.THREE_MONTHS,
    // Per-notification toggles
    val notifLowBattery: Boolean = true,
    val notifHighTemp: Boolean = true,
    val notifLowStorage: Boolean = true,
    val notifChargeComplete: Boolean = false,
    // Alert thresholds
    val alertBatteryThreshold: Int = AlertThresholds.DEFAULT_BATTERY_PERCENT,
    val alertTempThreshold: Int = AlertThresholds.DEFAULT_TEMPERATURE_C,
    val alertStorageThreshold: Int = AlertThresholds.DEFAULT_STORAGE_PERCENT,
    // Display
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    // Live notification
    val liveNotificationEnabled: Boolean = false,
    val liveNotifCurrent: Boolean = true,
    val liveNotifDrainRate: Boolean = true,
    val liveNotifTemperature: Boolean = true,
    val liveNotifScreenStats: Boolean = false,
    val liveNotifRemainingTime: Boolean = false,
    // Weekly report
    val weeklyReportEnabled: Boolean = false,
    // Info cards
    val showInfoCards: Boolean = true,
)

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

object AlertThresholds {
    const val MIN_BATTERY_PERCENT = 5
    const val MAX_BATTERY_PERCENT = 50
    const val DEFAULT_BATTERY_PERCENT = 20
    const val MIN_TEMPERATURE_C = 35
    const val MAX_TEMPERATURE_C = 50
    const val DEFAULT_TEMPERATURE_C = 42
    const val MIN_STORAGE_PERCENT = 70
    const val MAX_STORAGE_PERCENT = 99
    const val DEFAULT_STORAGE_PERCENT = 90
}
