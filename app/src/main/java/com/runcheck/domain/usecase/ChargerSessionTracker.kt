package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargerSessionTracker @Inject constructor(
    private val chargerRepository: ChargerRepository,
    private val batteryRepository: BatteryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    private val mutex = Mutex()

    suspend fun onBatteryState(state: BatteryState, timestamp: Long = System.currentTimeMillis()) {
        mutex.withLock {
            val selectedChargerId = userPreferencesRepository.getSelectedChargerId()
            val activeSession = chargerRepository.getActiveSession()
            val isCharging = state.chargingStatus == ChargingStatus.CHARGING

            when {
                isCharging && selectedChargerId != null && activeSession == null -> {
                    chargerRepository.insertSession(
                        ChargingSession(
                            chargerId = selectedChargerId,
                            startTime = timestamp,
                            endTime = null,
                            startLevel = state.level,
                            endLevel = null,
                            avgCurrentMa = null,
                            maxCurrentMa = null,
                            avgVoltageMv = null,
                            avgPowerMw = null,
                            plugType = state.plugType.name
                        )
                    )
                }

                isCharging && selectedChargerId != null && activeSession != null &&
                    activeSession.chargerId != selectedChargerId -> {
                    completeSession(activeSession, state, timestamp)
                    chargerRepository.insertSession(
                        ChargingSession(
                            chargerId = selectedChargerId,
                            startTime = timestamp,
                            endTime = null,
                            startLevel = state.level,
                            endLevel = null,
                            avgCurrentMa = null,
                            maxCurrentMa = null,
                            avgVoltageMv = null,
                            avgPowerMw = null,
                            plugType = state.plugType.name
                        )
                    )
                }

                activeSession != null && (!isCharging || selectedChargerId == null) -> {
                    completeSession(activeSession, state, timestamp)
                }
            }
        }
    }

    private suspend fun completeSession(
        session: ChargingSession,
        state: BatteryState,
        timestamp: Long
    ) {
        val readings = batteryRepository.getReadingsSinceSync(session.startTime)
            .filter { reading ->
                reading.timestamp in session.startTime..timestamp &&
                    reading.status == ChargingStatus.CHARGING.name
            }

        val currentValues = readings.mapNotNull { it.currentMa }
        val voltageValues = readings.map { it.voltageMv }
        val powerValues = readings.mapNotNull { reading ->
            reading.currentMa?.let { currentMa ->
                (currentMa * reading.voltageMv) / 1000
            }
        }
        val fallbackCurrent = state.currentMa.takeIf {
            state.chargingStatus == ChargingStatus.CHARGING && it.confidence != Confidence.UNAVAILABLE
        }?.value
        val effectiveCurrentValues = if (currentValues.isEmpty() && fallbackCurrent != null) {
            listOf(fallbackCurrent)
        } else {
            currentValues
        }
        val effectiveVoltageValues = if (voltageValues.isEmpty() && fallbackCurrent != null) {
            listOf(state.voltageMv)
        } else {
            voltageValues
        }
        val effectivePowerValues = if (powerValues.isEmpty() && fallbackCurrent != null) {
            listOf((fallbackCurrent * state.voltageMv) / 1000)
        } else {
            powerValues
        }

        chargerRepository.completeSession(
            id = session.id,
            endTime = timestamp,
            endLevel = state.level,
            avgCurrentMa = effectiveCurrentValues.averageOrNull(),
            maxCurrentMa = effectiveCurrentValues.maxOrNull(),
            avgVoltageMv = effectiveVoltageValues.averageOrNull(),
            avgPowerMw = effectivePowerValues.averageOrNull()
        )
    }
}

private fun List<Int>.averageOrNull(): Int? =
    takeIf(List<Int>::isNotEmpty)?.average()?.toInt()
