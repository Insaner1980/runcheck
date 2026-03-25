package com.runcheck.data.thermal

import com.runcheck.data.db.dao.ThrottlingEventDao
import com.runcheck.data.db.entity.ThrottlingEventEntity
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.ThrottlingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThrottlingRepositoryImpl @Inject constructor(
    private val throttlingEventDao: ThrottlingEventDao
) : ThrottlingRepository {

    override fun getRecentEvents(limit: Int): Flow<List<ThrottlingEvent>> =
        throttlingEventDao.getRecentEvents(limit).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)

    override suspend fun insert(event: ThrottlingEvent): Long = withContext(Dispatchers.IO) {
        throttlingEventDao.insert(event.toEntity())
    }

    override suspend fun updateSnapshot(
        id: Long,
        thermalStatus: String,
        batteryTempC: Float,
        cpuTempC: Float?,
        foregroundApp: String?
    ) = withContext(Dispatchers.IO) {
        throttlingEventDao.updateSnapshot(
            id = id,
            thermalStatus = thermalStatus,
            batteryTempC = batteryTempC,
            cpuTempC = cpuTempC,
            foregroundApp = foregroundApp
        )
    }

    override suspend fun updateDuration(id: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        throttlingEventDao.updateDuration(id, durationMs)
    }

    override suspend fun deleteOlderThan(cutoff: Long) = withContext<Unit>(Dispatchers.IO) {
        throttlingEventDao.deleteOlderThan(cutoff)
    }

    override suspend fun deleteAll() = withContext<Unit>(Dispatchers.IO) {
        throttlingEventDao.deleteAll()
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
