package com.runcheck.domain.repository

import com.runcheck.domain.model.ChargerProfile
import com.runcheck.domain.model.ChargerSummary
import com.runcheck.domain.model.ChargingSession
import kotlinx.coroutines.flow.Flow

interface ChargerRepository {
    fun getChargerProfiles(): Flow<List<ChargerProfile>>

    fun getAllSessions(): Flow<List<ChargingSession>>

    suspend fun insertCharger(name: String): Long

    suspend fun deleteChargerById(id: Long)

    suspend fun insertSession(session: ChargingSession): Long

    suspend fun completeSession(
        id: Long,
        endTime: Long,
        endLevel: Int,
        avgCurrentMa: Int?,
        maxCurrentMa: Int?,
        avgVoltageMv: Int?,
        avgPowerMw: Int?,
    )

    suspend fun getActiveSession(): ChargingSession?

    suspend fun deleteSessionsOlderThan(cutoff: Long)
}
