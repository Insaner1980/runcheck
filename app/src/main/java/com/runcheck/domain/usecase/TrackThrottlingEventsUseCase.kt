package com.runcheck.domain.usecase

import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.ThrottlingRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks throttling event transitions based on [ThermalState] changes.
 * Maintains an active throttling event state machine:
 * - Opens an event when thermal status crosses the SEVERE threshold
 * - Updates peak status when the situation worsens
 * - Closes the event when status drops below threshold
 *
 * Must be @Singleton because it holds in-memory state (the active event).
 */
@Singleton
class TrackThrottlingEventsUseCase
    @Inject
    constructor(
        private val throttlingRepository: ThrottlingRepository,
        private val foregroundAppProvider: ForegroundAppProvider,
    ) {
        private val mutex = Mutex()
        private var activeEvent: ActiveThrottlingEvent? = null

        suspend operator fun invoke(state: ThermalState) {
            mutex.withLock {
                val current = activeEvent
                when {
                    // Start new event
                    state.thermalStatus >= THROTTLING_THRESHOLD && current == null -> {
                        val startTimeMs = System.currentTimeMillis()
                        val eventId =
                            throttlingRepository.insert(
                                ThrottlingEvent(
                                    timestamp = startTimeMs,
                                    thermalStatus = state.thermalStatus.name,
                                    batteryTempC = state.batteryTempC,
                                    cpuTempC = state.cpuTempC,
                                    foregroundApp = foregroundAppProvider.getCurrentForegroundApp(),
                                    durationMs = null,
                                ),
                            )
                        activeEvent =
                            ActiveThrottlingEvent(
                                id = eventId,
                                startTimeMs = startTimeMs,
                                peakStatus = state.thermalStatus,
                            )
                    }

                    // Update peak
                    state.thermalStatus >= THROTTLING_THRESHOLD &&
                        current != null &&
                        state.thermalStatus > current.peakStatus -> {
                        throttlingRepository.updateSnapshot(
                            id = current.id,
                            thermalStatus = state.thermalStatus.name,
                            batteryTempC = state.batteryTempC,
                            cpuTempC = state.cpuTempC,
                            foregroundApp = foregroundAppProvider.getCurrentForegroundApp(),
                        )
                        activeEvent = current.copy(peakStatus = state.thermalStatus)
                    }

                    // Close event
                    state.thermalStatus < THROTTLING_THRESHOLD && current != null -> {
                        throttlingRepository.updateDuration(
                            id = current.id,
                            durationMs =
                                (System.currentTimeMillis() - current.startTimeMs)
                                    .coerceAtLeast(0L),
                        )
                        activeEvent = null
                    }
                }
            }
        }

        private data class ActiveThrottlingEvent(
            val id: Long,
            val startTimeMs: Long,
            val peakStatus: ThermalStatus,
        )

        /**
         * Abstraction for obtaining the current foreground app,
         * keeping the domain use case decoupled from the data layer.
         */
        interface ForegroundAppProvider {
            suspend fun getCurrentForegroundApp(): String?
        }

        private companion object {
            val THROTTLING_THRESHOLD = ThermalStatus.SEVERE
        }
    }
