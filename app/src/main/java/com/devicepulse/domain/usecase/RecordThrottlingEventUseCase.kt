package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.ThrottlingEvent
import com.devicepulse.domain.repository.ThrottlingRepository
import javax.inject.Inject

class RecordThrottlingEventUseCase @Inject constructor(
    private val throttlingRepository: ThrottlingRepository
) {
    suspend operator fun invoke(
        thermalStatus: String,
        batteryTempC: Float,
        cpuTempC: Float?,
        foregroundApp: String?
    ) {
        val event = ThrottlingEvent(
            timestamp = System.currentTimeMillis(),
            thermalStatus = thermalStatus,
            batteryTempC = batteryTempC,
            cpuTempC = cpuTempC,
            foregroundApp = foregroundApp,
            durationMs = null
        )
        throttlingRepository.insert(event)
    }
}
