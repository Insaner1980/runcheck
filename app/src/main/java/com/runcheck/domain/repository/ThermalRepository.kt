package com.devicepulse.domain.repository

import com.devicepulse.domain.model.ThermalState
import kotlinx.coroutines.flow.Flow

interface ThermalRepository {
    fun getThermalState(): Flow<ThermalState>
    suspend fun saveReading(state: ThermalState)
    suspend fun getAllReadings(): List<ThermalReadingData>
    suspend fun deleteOlderThan(cutoff: Long)
}

data class ThermalReadingData(
    val timestamp: Long,
    val batteryTempC: Float,
    val cpuTempC: Float?,
    val thermalStatus: Int,
    val throttling: Boolean
)
