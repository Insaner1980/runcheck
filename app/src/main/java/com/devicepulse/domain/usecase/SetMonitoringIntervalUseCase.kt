package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.repository.MonitoringScheduler
import com.devicepulse.domain.repository.UserPreferencesRepository
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
