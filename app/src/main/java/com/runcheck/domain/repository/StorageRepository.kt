package com.runcheck.domain.repository

import com.runcheck.domain.model.StorageState
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun getStorageState(): Flow<StorageState>
    suspend fun saveReading(state: StorageState)
    suspend fun getAllReadings(): List<StorageReadingData>
    suspend fun deleteOlderThan(cutoff: Long)
}

data class StorageReadingData(
    val timestamp: Long,
    val totalBytes: Long,
    val availableBytes: Long,
    val appsBytes: Long,
    val mediaBytes: Long
)
