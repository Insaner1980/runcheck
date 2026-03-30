package com.runcheck.domain.repository

import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun getNetworkState(): Flow<NetworkState>

    suspend fun measureLatency(): Int?

    suspend fun saveReading(state: NetworkState)

    suspend fun getAllReadings(): List<NetworkReading>

    fun getReadingsSince(
        since: Long,
        limit: Int? = null,
    ): Flow<List<NetworkReading>>

    suspend fun getReadingsSinceSync(since: Long): List<NetworkReading>

    suspend fun deleteOlderThan(cutoff: Long)

    suspend fun deleteAll()
}
