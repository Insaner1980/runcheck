package com.runcheck.domain.usecase

import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.repository.ThrottlingRepository
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
