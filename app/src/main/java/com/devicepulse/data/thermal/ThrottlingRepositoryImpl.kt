package com.devicepulse.data.thermal

import com.devicepulse.data.db.dao.ThrottlingEventDao
import com.devicepulse.data.db.entity.ThrottlingEventEntity
import com.devicepulse.domain.model.ThrottlingEvent
import com.devicepulse.domain.repository.ThrottlingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThrottlingRepositoryImpl @Inject constructor(
    private val throttlingEventDao: ThrottlingEventDao
) : ThrottlingRepository {

    override fun getRecentEvents(limit: Int): Flow<List<ThrottlingEvent>> =
        throttlingEventDao.getRecentEvents(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insert(event: ThrottlingEvent) {
        throttlingEventDao.insert(event.toEntity())
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        throttlingEventDao.deleteOlderThan(cutoff)
    }
}

private fun ThrottlingEventEntity.toDomain() = ThrottlingEvent(
    id = id,
    timestamp = timestamp,
    thermalStatus = thermalStatus,
    batteryTempC = batteryTempC,
    cpuTempC = cpuTempC,
    foregroundApp = foregroundApp,
    durationMs = durationMs
)

private fun ThrottlingEvent.toEntity() = ThrottlingEventEntity(
    id = id,
    timestamp = timestamp,
    thermalStatus = thermalStatus,
    batteryTempC = batteryTempC,
    cpuTempC = cpuTempC,
    foregroundApp = foregroundApp,
    durationMs = durationMs
)
