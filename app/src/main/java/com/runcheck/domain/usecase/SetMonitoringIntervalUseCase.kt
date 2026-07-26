package com.runcheck.domain.usecase

import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetMonitoringIntervalUseCase
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val monitorScheduler: MonitoringScheduler,
    ) {
        private val updateMutex = Mutex()

        suspend operator fun invoke(interval: MonitoringInterval) {
            updateMutex.withLock {
                userPreferencesRepository.setMonitoringInterval(interval)
                monitorScheduler.schedule(interval)
            }
        }
    }
