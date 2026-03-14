package com.runcheck.domain.model

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val dynamicColors: Boolean = true,
    val monitoringInterval: MonitoringInterval = MonitoringInterval.THIRTY,
    val notificationsEnabled: Boolean = true,
    val dataRetention: DataRetention = DataRetention.THREE_MONTHS,
    val crashReportingEnabled: Boolean = false
)
