package com.runcheck.domain.repository

import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.StorageState
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun getStorageState(): Flow<StorageState>
    suspend fun saveReading(state: StorageState)
    suspend fun getAllReadings(): List<StorageReading>
    suspend fun deleteOlderThan(cutoff: Long)
    suspend fun deleteAll()
}
