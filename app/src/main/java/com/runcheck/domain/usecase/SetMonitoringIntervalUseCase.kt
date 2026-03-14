package com.runcheck.domain.usecase

import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetMonitoringIntervalUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val monitorScheduler: MonitoringScheduler
) {
    suspend operator fun invoke(interval: MonitoringInterval) {
        userPreferencesRepository.setMonitoringInterval(interval)
        monitorScheduler.schedule(interval)
    }
}
