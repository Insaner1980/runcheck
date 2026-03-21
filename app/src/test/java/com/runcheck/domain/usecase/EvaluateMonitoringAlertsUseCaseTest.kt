package com.runcheck.domain.usecase

import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.UserPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluateMonitoringAlertsUseCaseTest {

    private val useCase = EvaluateMonitoringAlertsUseCase()

    @Test
    fun `master toggle disables every alert`() {
        val decision = useCase(
            previous = MonitoringAlertSnapshot(
                batteryLevel = 60,
                batteryTempC = 35f,
                storageUsagePercent = 60f,
                chargingStatus = ChargingStatus.CHARGING
            ),
            current = MonitoringAlertSnapshot(
                batteryLevel = 10,
                batteryTempC = 45f,
                storageUsagePercent = 95f,
                chargingStatus = ChargingStatus.FULL
            ),
            preferences = UserPreferences(
                notificationsEnabled = false,
                notifLowBattery = true,
                notifHighTemp = true,
                notifLowStorage = true,
                notifChargeComplete = true
            )
        )

        assertFalse(decision.lowBattery)
        assertFalse(decision.highTemp)
        assertFalse(decision.lowStorage)
        assertFalse(decision.chargeComplete)
    }

    @Test
    fun `crossing thresholds triggers low battery high temp and low storage`() {
        val decision = useCase(
            previous = MonitoringAlertSnapshot(
                batteryLevel = 35,
                batteryTempC = 41f,
                storageUsagePercent = 89f,
                chargingStatus = ChargingStatus.DISCHARGING
            ),
            current = MonitoringAlertSnapshot(
                batteryLevel = 20,
                batteryTempC = 43f,
                storageUsagePercent = 91f,
                chargingStatus = ChargingStatus.DISCHARGING
            ),
            preferences = UserPreferences(
                alertBatteryThreshold = 20,
                alertTempThreshold = 42,
                alertStorageThreshold = 90
            )
        )

        assertTrue(decision.lowBattery)
        assertTrue(decision.highTemp)
        assertTrue(decision.lowStorage)
    }

    @Test
    fun `staying beyond thresholds does not retrigger repeated alerts`() {
        val decision = useCase(
            previous = MonitoringAlertSnapshot(
                batteryLevel = 15,
                batteryTempC = 44f,
                storageUsagePercent = 94f,
                chargingStatus = ChargingStatus.DISCHARGING
            ),
            current = MonitoringAlertSnapshot(
                batteryLevel = 14,
                batteryTempC = 45f,
                storageUsagePercent = 95f,
                chargingStatus = ChargingStatus.DISCHARGING
            ),
            preferences = UserPreferences()
        )

        assertFalse(decision.lowBattery)
        assertFalse(decision.highTemp)
        assertFalse(decision.lowStorage)
    }

    @Test
    fun `charge complete only fires on charging to full transition`() {
        val decision = useCase(
            previous = MonitoringAlertSnapshot(
                batteryLevel = 99,
                batteryTempC = 36f,
                storageUsagePercent = 70f,
                chargingStatus = ChargingStatus.CHARGING
            ),
            current = MonitoringAlertSnapshot(
                batteryLevel = 100,
                batteryTempC = 35f,
                storageUsagePercent = 70f,
                chargingStatus = ChargingStatus.FULL
            ),
            preferences = UserPreferences(notifChargeComplete = true)
        )

        assertTrue(decision.chargeComplete)
    }

    @Test
    fun `individual toggles stay independent`() {
        val decision = useCase(
            previous = MonitoringAlertSnapshot(
                batteryLevel = 40,
                batteryTempC = 40f,
                storageUsagePercent = 85f,
                chargingStatus = ChargingStatus.CHARGING
            ),
            current = MonitoringAlertSnapshot(
                batteryLevel = 15,
                batteryTempC = 45f,
                storageUsagePercent = 95f,
                chargingStatus = ChargingStatus.FULL
            ),
            preferences = UserPreferences(
                notifLowBattery = false,
                notifHighTemp = true,
                notifLowStorage = false,
                notifChargeComplete = true
            )
        )

        assertFalse(decision.lowBattery)
        assertTrue(decision.highTemp)
        assertFalse(decision.lowStorage)
        assertTrue(decision.chargeComplete)
    }
}
