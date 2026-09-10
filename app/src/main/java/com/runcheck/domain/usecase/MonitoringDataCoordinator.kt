package com.runcheck.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Serializes history resets with the complete insight read/evaluate/publish operation. */
@Singleton
class MonitoringDataCoordinator
    @Inject
    constructor(
        private val trackThrottlingEvents: TrackThrottlingEventsUseCase,
    ) {
        private val mutex = Mutex()

        suspend fun withInsightGeneration(block: suspend () -> Unit) {
            mutex.withLock { block() }
        }

        suspend fun resetHistory(block: suspend () -> Unit) {
            // Lock order: coordination, thermal tracker, then Room. Never re-enter from block.
            mutex.withLock {
                trackThrottlingEvents.withHistoryReset(block)
            }
        }
    }
