package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.ThrottlingEvent
import com.devicepulse.domain.model.ThermalStatus
import com.devicepulse.domain.repository.ThrottlingRepository
import javax.inject.Inject

class RecordThrottlingEventUseCase @Inject constructor(
    private val throttlingRepository: ThrottlingRepository
) {
    suspend operator fun invoke(
        thermalStatus: ThermalStatus,
        batteryTempC: Float,
        cpuTempC: Float?,
        foregroundApp: String?
    ): Long {
        val event = ThrottlingEvent(
            timestamp = System.currentTimeMillis(),
            thermalStatus = thermalStatus.name,
            batteryTempC = batteryTempC,
            cpuTempC = cpuTempC,
            foregroundApp = foregroundApp,
            durationMs = null
        )
        return throttlingRepository.insert(event)
    }
}
