package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargerSessionTracker
    @Inject
    constructor(
        private val chargerRepository: ChargerRepository,
        private val batteryRepository: BatteryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val transactionRunner: DatabaseTransactionRunner,
    ) {
        private val mutex = Mutex()
        private var lastProcessedTimestamp = Long.MIN_VALUE
        private var lastObservedStatus: ChargingStatus? = null
        private var lastObservationTimestamp = 0L

        suspend fun onBatteryState(
            state: BatteryState,
            timestamp: Long = System.currentTimeMillis(),
        ) {
            mutex.withLock {
                processBatteryState(state, timestamp)
            }
        }

        suspend fun onObservedBatteryState(
            state: BatteryState,
            timestamp: Long = System.currentTimeMillis(),
        ) {
            mutex.withLock {
                if (timestamp < lastProcessedTimestamp) return@withLock
                if (lastObservedStatus == state.chargingStatus &&
                    timestamp - lastObservationTimestamp < OBSERVATION_INTERVAL_MS
                ) {
                    return@withLock
                }
                processBatteryState(state, timestamp)
                lastObservedStatus = state.chargingStatus
                lastObservationTimestamp = timestamp
            }
        }

        private suspend fun processBatteryState(
            state: BatteryState,
            timestamp: Long,
        ) {
            if (timestamp < lastProcessedTimestamp) return

            val selectedChargerId = userPreferencesRepository.getSelectedChargerId()
            val activeSession = chargerRepository.getActiveSession()
            val isCharging = state.chargingStatus == ChargingStatus.CHARGING

            when {
                isCharging && selectedChargerId != null && activeSession == null -> {
                    startSession(selectedChargerId, state, timestamp)
                }

                isCharging && selectedChargerId != null && activeSession != null &&
                    activeSession.chargerId != selectedChargerId -> {
                    transactionRunner.runInTransaction {
                        completeSession(activeSession, state, timestamp)
                        startSession(selectedChargerId, state, timestamp)
                    }
                }

                activeSession != null && (!isCharging || selectedChargerId == null) -> {
                    completeSession(activeSession, state, timestamp)
                }
            }

            lastProcessedTimestamp = timestamp
        }

        private suspend fun startSession(
            chargerId: Long,
            state: BatteryState,
            timestamp: Long,
        ) {
            chargerRepository.insertSession(
                ChargingSession(
                    chargerId = chargerId,
                    startTime = timestamp,
                    endTime = null,
                    startLevel = state.level,
                    endLevel = null,
                    avgCurrentMa = null,
                    maxCurrentMa = null,
                    avgVoltageMv = null,
                    avgPowerMw = null,
                    plugType = state.plugType.name,
                ),
            )
        }

        private suspend fun completeSession(
            session: ChargingSession,
            state: BatteryState,
            timestamp: Long,
        ) {
            val readings =
                batteryRepository
                    .getReadingsSinceSync(session.startTime)
                    .filter { reading ->
                        reading.timestamp in session.startTime..timestamp &&
                            reading.status == ChargingStatus.CHARGING.name
                    }

            val currentValues = readings.mapNotNull { it.currentMa }
            val voltageValues = readings.map { it.voltageMv }
            val powerValues =
                readings.mapNotNull { reading ->
                    reading.currentMa?.let { currentMa ->
                        (currentMa * reading.voltageMv) / 1000
                    }
                }
            val fallbackCurrent =
                state.currentMa
                    .takeIf {
                        state.chargingStatus == ChargingStatus.CHARGING && it.confidence != Confidence.UNAVAILABLE
                    }?.value
            val effectiveCurrentValues =
                if (currentValues.isEmpty() && fallbackCurrent != null) {
                    listOf(fallbackCurrent)
                } else {
                    currentValues
                }
            val effectiveVoltageValues =
                if (voltageValues.isEmpty() && fallbackCurrent != null) {
                    listOf(state.voltageMv)
                } else {
                    voltageValues
                }
            val effectivePowerValues =
                if (powerValues.isEmpty() && fallbackCurrent != null) {
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
                avgPowerMw = effectivePowerValues.averageOrNull(),
            )
        }

        private companion object {
            const val OBSERVATION_INTERVAL_MS = 15_000L
        }
    }
