package com.runcheck.domain.usecase

import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.UserPreferences
import javax.inject.Inject

data class MonitoringAlertSnapshot(
    val batteryLevel: Int,
    val batteryTempC: Float,
    val storageUsagePercent: Float,
    val chargingStatus: ChargingStatus
)

data class MonitoringAlertDecision(
    val lowBattery: Boolean = false,
    val highTemp: Boolean = false,
    val lowStorage: Boolean = false,
    val chargeComplete: Boolean = false
)

class EvaluateMonitoringAlertsUseCase @Inject constructor() {

    operator fun invoke(
        previous: MonitoringAlertSnapshot?,
        current: MonitoringAlertSnapshot,
        preferences: UserPreferences,
        chargeCompleteFiredPreviously: Boolean = false
    ): MonitoringAlertDecision {
        if (!preferences.notificationsEnabled) {
            return MonitoringAlertDecision()
        }

        return MonitoringAlertDecision(
            lowBattery = preferences.notifLowBattery &&
                current.batteryLevel <= preferences.alertBatteryThreshold &&
                (previous == null || previous.batteryLevel > preferences.alertBatteryThreshold),
            highTemp = preferences.notifHighTemp &&
                current.batteryTempC > preferences.alertTempThreshold &&
                (previous == null || previous.batteryTempC <= preferences.alertTempThreshold),
            lowStorage = preferences.notifLowStorage &&
                current.storageUsagePercent > preferences.alertStorageThreshold &&
                (previous == null || previous.storageUsagePercent <= preferences.alertStorageThreshold),
            chargeComplete = preferences.notifChargeComplete &&
                !chargeCompleteFiredPreviously &&
                previous?.chargingStatus == ChargingStatus.CHARGING &&
                current.chargingStatus == ChargingStatus.FULL
        )
    }
}
