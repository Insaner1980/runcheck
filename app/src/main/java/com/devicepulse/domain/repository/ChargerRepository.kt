package com.devicepulse.domain.repository

import com.devicepulse.domain.model.ChargerProfile
import com.devicepulse.domain.model.ChargerSummary
import com.devicepulse.domain.model.ChargingSession
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
        avgPowerMw: Int?
    )
    suspend fun getActiveSession(): ChargingSession?
}
