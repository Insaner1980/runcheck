package com.runcheck.data.network

import com.runcheck.data.db.dao.NetworkReadingDao
import com.runcheck.data.db.entity.NetworkReadingEntity
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.repository.NetworkReadingData
import com.runcheck.domain.repository.NetworkRepository as NetworkRepositoryContract
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val networkDataSource: NetworkDataSource,
    private val latencyMeasurer: LatencyMeasurer,
    private val networkReadingDao: NetworkReadingDao
) : NetworkRepositoryContract {

    @OptIn(FlowPreview::class)
    override fun getNetworkState(): Flow<NetworkState> =
        networkDataSource.getNetworkInfo()
            .sample(DISPLAY_UPDATE_INTERVAL_MS)
            .map { info ->
                NetworkState(
                    connectionType = info.connectionType,
                    signalDbm = info.signalDbm,
                    signalQuality = info.signalQuality,
                    wifiSsid = info.wifiSsid,
                    wifiSpeedMbps = info.wifiSpeedMbps,
                    wifiFrequencyMhz = info.wifiFrequencyMhz,
                    carrier = info.carrier,
                    networkSubtype = info.networkSubtype,
                    latencyMs = null
                )
            }
            .conflate()

    override suspend fun measureLatency(): Int? {
        if (!networkDataSource.hasValidatedConnection()) {
            return null
        }
        return latencyMeasurer.measureLatency()
    }

    override suspend fun saveReading(state: NetworkState) {
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

    override suspend fun getAllReadings(): List<NetworkReadingData> {
        return networkReadingDao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        networkReadingDao.deleteOlderThan(cutoff)
    }

    companion object {
        private const val DISPLAY_UPDATE_INTERVAL_MS = 333L
    }
}

private fun NetworkReadingEntity.toDomain() = NetworkReadingData(
    timestamp = timestamp,
    type = type,
    signalDbm = signalDbm,
    wifiSpeedMbps = wifiSpeedMbps,
    wifiFrequency = wifiFrequency,
    carrier = carrier,
    networkSubtype = networkSubtype,
    latencyMs = latencyMs
)
