package com.runcheck.domain.repository

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import kotlinx.coroutines.flow.Flow

interface BatteryRepository {
    fun getBatteryState(): Flow<BatteryState>
    fun getReadingsSince(since: Long, limit: Int? = null): Flow<List<BatteryReading>>
    suspend fun saveReading(state: BatteryState)
    suspend fun getAllReadings(): List<BatteryReading>
    suspend fun getReadingsSinceSync(since: Long): List<BatteryReading>
    suspend fun deleteOlderThan(cutoff: Long)
    suspend fun deleteAll()
    suspend fun getLastChargingTimestamp(): Long?
}
