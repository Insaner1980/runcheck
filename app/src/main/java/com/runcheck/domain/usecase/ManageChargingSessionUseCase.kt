package com.runcheck.domain.usecase

import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.repository.ChargerRepository
import javax.inject.Inject

class ManageChargingSessionUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository
) {

    suspend fun startSession(chargerId: Long, level: Int, plugType: String): Long {
        val session = ChargingSession(
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
        return chargerRepository.insertSession(session)
    }

    suspend fun completeSession(
        sessionId: Long,
        endLevel: Int,
        avgCurrentMa: Int?,
        maxCurrentMa: Int?,
        avgVoltageMv: Int?,
        avgPowerMw: Int?
    ) {
        chargerRepository.completeSession(
            id = sessionId,
            endTime = System.currentTimeMillis(),
            endLevel = endLevel,
            avgCurrentMa = avgCurrentMa,
            maxCurrentMa = maxCurrentMa,
            avgVoltageMv = avgVoltageMv,
            avgPowerMw = avgPowerMw
        )
    }

    suspend fun getActiveSession(): ChargingSession? {
        return chargerRepository.getActiveSession()
    }
}
