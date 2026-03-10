package com.devicepulse.data.network

import com.devicepulse.data.db.dao.NetworkReadingDao
import com.devicepulse.data.db.entity.NetworkReadingEntity
import com.devicepulse.domain.model.NetworkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository @Inject constructor(
    private val networkDataSource: NetworkDataSource,
    private val latencyMeasurer: LatencyMeasurer,
    private val networkReadingDao: NetworkReadingDao
) {

    fun getNetworkState(): Flow<NetworkState> = combine(
        networkDataSource.getNetworkInfo(),
        latencyFlow()
    ) { info, latency ->
        NetworkState(
            connectionType = info.connectionType,
            signalDbm = info.signalDbm,
            signalQuality = info.signalQuality,
            wifiSsid = info.wifiSsid,
            wifiSpeedMbps = info.wifiSpeedMbps,
            wifiFrequencyMhz = info.wifiFrequencyMhz,
            carrier = info.carrier,
            networkSubtype = info.networkSubtype,
            latencyMs = latency
        )
    }

    private fun latencyFlow(): Flow<Int?> = flow {
        emit(latencyMeasurer.measureLatency())
    }

    fun getReadingsSince(since: Long): Flow<List<NetworkReadingEntity>> {
        return networkReadingDao.getReadingsSince(since)
    }

    suspend fun saveReading(state: NetworkState) {
        val entity = NetworkReadingEntity(
            timestamp = System.currentTimeMillis(),
            type = state.connectionType.name,
            signalDbm = state.signalDbm,
            wifiSpeedMbps = state.wifiSpeedMbps,
            wifiFrequency = state.wifiFrequencyMhz,
            carrier = state.carrier,
            networkSubtype = state.networkSubtype,
            latencyMs = state.latencyMs
        )
        networkReadingDao.insert(entity)
    }
}
