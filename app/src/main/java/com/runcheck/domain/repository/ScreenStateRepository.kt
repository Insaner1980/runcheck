package com.runcheck.domain.repository

import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.ScreenUsageStats
import com.runcheck.domain.model.SleepAnalysis

interface ScreenStateRepository {
    fun initialize()

    fun onScreenTurnedOn()

    fun onScreenTurnedOff()

    fun onPowerConnected()

    fun onPowerDisconnected()

    fun onDeviceIdleModeChanged()

    fun updateChargingStatus(chargingStatus: ChargingStatus)

    fun getScreenUsageStats(): ScreenUsageStats?

    fun getSleepAnalysis(): SleepAnalysis?
}
