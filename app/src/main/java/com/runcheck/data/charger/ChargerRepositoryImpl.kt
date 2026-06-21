package com.runcheck.data.charger

import com.runcheck.data.db.dao.ChargerDao
import com.runcheck.data.db.entity.ChargerProfileEntity
import com.runcheck.data.db.entity.ChargingSessionEntity
import com.runcheck.domain.model.ChargerProfile
import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.util.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChargerRepositoryImpl
    @Inject
    constructor(
        private val chargerDao: ChargerDao,
        private val dispatchers: AppDispatchers,
    ) : ChargerRepository {
        override fun getChargerProfiles(): Flow<List<ChargerProfile>> =
            chargerDao
                .getChargerProfiles()
                .map { entities ->
                    entities.map { it.toDomain() }
                }.flowOn(dispatchers.io)

        override fun getAllSessions(): Flow<List<ChargingSession>> =
            chargerDao
                .getAllSessions()
                .map { entities ->
                    entities.map { it.toDomain() }
                }.flowOn(dispatchers.io)

        override suspend fun getChargerProfilesSync(): List<ChargerProfile> =
            chargerDao.getChargerProfilesSync().map { it.toDomain() }

        override suspend fun getAllSessionsSync(): List<ChargingSession> =
            chargerDao.getAllSessionsSync().map { it.toDomain() }

        override suspend fun insertCharger(name: String): Long =
            chargerDao.insertCharger(
                ChargerProfileEntity(
                    name = name.trim(),
                    created = System.currentTimeMillis(),
                ),
            )

        override suspend fun deleteChargerById(id: Long) =
            chargerDao.deleteChargerById(id)

        override suspend fun insertSession(session: ChargingSession): Long =
            chargerDao.insertSession(session.toEntity())

        override suspend fun completeSession(
            id: Long,
            endTime: Long,
            endLevel: Int,
            avgCurrentMa: Int?,
            maxCurrentMa: Int?,
            avgVoltageMv: Int?,
            avgPowerMw: Int?,
        ) {
            chargerDao.completeSession(id, endTime, endLevel, avgCurrentMa, maxCurrentMa, avgVoltageMv, avgPowerMw)
        }

        override suspend fun getActiveSession(): ChargingSession? =
            chargerDao.getActiveSession()?.toDomain()

        override suspend fun deleteSessionsOlderThan(cutoff: Long) =
            chargerDao.deleteSessionsOlderThan(cutoff)
    }

private fun ChargerProfileEntity.toDomain() =
    ChargerProfile(
        id = id,
        name = name,
        created = created,
    )

private fun ChargingSessionEntity.toDomain() =
    ChargingSession(
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
        plugType = plugType,
    )

private fun ChargingSession.toEntity() =
    ChargingSessionEntity(
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
        plugType = plugType,
    )
