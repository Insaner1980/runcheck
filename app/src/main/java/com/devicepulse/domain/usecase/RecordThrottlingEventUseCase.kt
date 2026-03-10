package com.devicepulse.domain.usecase

import com.devicepulse.data.db.dao.ThrottlingEventDao
import com.devicepulse.data.db.entity.ThrottlingEventEntity
import javax.inject.Inject

class RecordThrottlingEventUseCase @Inject constructor(
    private val throttlingEventDao: ThrottlingEventDao
) {
    suspend operator fun invoke(
        thermalStatus: String,
        batteryTempC: Float,
        cpuTempC: Float?,
        foregroundApp: String?
    ) {
        val event = ThrottlingEventEntity(
            timestamp = System.currentTimeMillis(),
            thermalStatus = thermalStatus,
            batteryTempC = batteryTempC,
            cpuTempC = cpuTempC,
            foregroundApp = foregroundApp,
            durationMs = null
        )
        throttlingEventDao.insert(event)
    }
}
