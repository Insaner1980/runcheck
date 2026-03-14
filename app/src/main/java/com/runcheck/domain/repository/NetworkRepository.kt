package com.runcheck.domain.repository

import com.runcheck.domain.model.NetworkState
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun getNetworkState(): Flow<NetworkState>
    suspend fun measureLatency(): Int?
    suspend fun saveReading(state: NetworkState)
    suspend fun getAllReadings(): List<NetworkReadingData>
    suspend fun deleteOlderThan(cutoff: Long)
}

data class NetworkReadingData(
    val timestamp: Long,
    val type: String,
    val signalDbm: Int?,
    val wifiSpeedMbps: Int?,
    val wifiFrequency: Int?,
    val carrier: String?,
    val networkSubtype: String?,
    val latencyMs: Int?
)
