package com.runcheck.domain.repository

import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.StorageState
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun getStorageState(): Flow<StorageState>

    suspend fun saveReading(state: StorageState)

    fun getReadingsSince(
        since: Long,
        limit: Int? = null,
    ): Flow<List<StorageReading>>

    suspend fun getReadingsSinceSync(since: Long): List<StorageReading>

    suspend fun getAllReadings(): List<StorageReading>

    suspend fun deleteOlderThan(cutoff: Long)

    suspend fun deleteAll()
}
