package com.runcheck.domain.repository

import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import kotlinx.coroutines.flow.Flow

interface ThermalRepository {
    fun getThermalState(): Flow<ThermalState>
    suspend fun saveReading(state: ThermalState)
    suspend fun getAllReadings(): List<ThermalReading>
    suspend fun deleteOlderThan(cutoff: Long)
    suspend fun deleteAll()
}
