package com.devicepulse.domain.repository

import com.devicepulse.domain.model.BatteryReading
import com.devicepulse.domain.model.BatteryState
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    fun getBatteryState(): Flow<BatteryState>
    fun getReadingsSince(since: Long): Flow<List<BatteryReading>>
    suspend fun saveReading(state: BatteryState)
    suspend fun getAllReadings(): List<BatteryReading>
    suspend fun deleteOlderThan(cutoff: Long)
}
