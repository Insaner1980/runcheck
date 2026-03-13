package com.devicepulse.data.charger

import com.devicepulse.data.db.dao.ChargerDao
import com.devicepulse.data.db.entity.ChargerProfileEntity
import com.devicepulse.data.db.entity.ChargingSessionEntity
import com.devicepulse.domain.model.ChargerProfile
import com.devicepulse.domain.model.ChargingSession
import com.devicepulse.domain.repository.ChargerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargerRepositoryImpl @Inject constructor(
    private val chargerDao: ChargerDao
) : ChargerRepository {

    override fun getChargerProfiles(): Flow<List<ChargerProfile>> =
        chargerDao.getChargerProfiles().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getAllSessions(): Flow<List<ChargingSession>> =
        chargerDao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertCharger(name: String): Long {
        return chargerDao.insertCharger(
            ChargerProfileEntity(
                name = name.trim(),
                created = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteChargerById(id: Long) {
        chargerDao.deleteChargerById(id)
    }

    override suspend fun insertSession(session: ChargingSession): Long {
        return chargerDao.insertSession(session.toEntity())
    }

    override suspend fun completeSession(
        id: Long,
        endTime: Long,
        endLevel: Int,
        avgCurrentMa: Int?,
        maxCurrentMa: Int?,
        avgVoltageMv: Int?,
        avgPowerMw: Int?
    ) {
        chargerDao.completeSession(id, endTime, endLevel, avgCurrentMa, maxCurrentMa, avgVoltageMv, avgPowerMw)
    }

    override suspend fun getActiveSession(): ChargingSession? {
        return chargerDao.getActiveSession()?.toDomain()
    }
}

private fun ChargerProfileEntity.toDomain() = ChargerProfile(
    id = id,
    name = name,
    created = created
)

private fun ChargingSessionEntity.toDomain() = ChargingSession(
    id = id,
    chargerId = chargerId,
    startTime = startTime,
    endTime = endTime,
    startLevel = startLevel,
    endLevel = endLevel,
    avgCurrentMa = avgCurrentMa,
    maxCurrentMa = maxCurrentMa,
    avgVoltageMv = avgVoltageMv,
    avgPowerMw = avgPowerMw,
    plugType = plugType
)

private fun ChargingSession.toEntity() = ChargingSessionEntity(
    id = id,
    chargerId = chargerId,
    startTime = startTime,
    endTime = endTime,
    startLevel = startLevel,
    endLevel = endLevel,
    avgCurrentMa = avgCurrentMa,
    maxCurrentMa = maxCurrentMa,
    avgVoltageMv = avgVoltageMv,
    avgPowerMw = avgPowerMw,
    plugType = plugType
)
