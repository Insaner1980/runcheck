package com.devicepulse.domain.usecase

import com.devicepulse.data.db.dao.ChargerDao
import com.devicepulse.data.db.entity.ChargingSessionEntity
import javax.inject.Inject

class ManageChargingSessionUseCase @Inject constructor(
    private val chargerDao: ChargerDao
) {

    /**
     * Start a new charging session for the given charger.
     * Returns the ID of the newly created session.
     */
    suspend fun startSession(chargerId: Long, level: Int, plugType: String): Long {
        val session = ChargingSessionEntity(
            chargerId = chargerId,
            startTime = System.currentTimeMillis(),
            endTime = null,
            startLevel = level,
            endLevel = null,
            avgCurrentMa = null,
            maxCurrentMa = null,
            avgVoltageMv = null,
            avgPowerMw = null,
            plugType = plugType
        )
        return chargerDao.insertSession(session)
    }

    /**
     * Complete an active charging session with the final measurements.
     */
    suspend fun completeSession(
        sessionId: Long,
        endLevel: Int,
        avgCurrentMa: Int?,
        maxCurrentMa: Int?,
        avgVoltageMv: Int?,
        avgPowerMw: Int?
    ) {
        chargerDao.completeSession(
            id = sessionId,
            endTime = System.currentTimeMillis(),
            endLevel = endLevel,
            avgCurrentMa = avgCurrentMa,
            maxCurrentMa = maxCurrentMa,
            avgVoltageMv = avgVoltageMv,
            avgPowerMw = avgPowerMw
        )
    }

    /**
     * Get the currently active (incomplete) charging session, if any.
     */
    suspend fun getActiveSession(): ChargingSessionEntity? {
        return chargerDao.getActiveSession()
    }
}
