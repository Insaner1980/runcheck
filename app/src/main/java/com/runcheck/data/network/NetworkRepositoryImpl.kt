package com.runcheck.data.network

import com.runcheck.data.db.dao.NetworkReadingDao
import com.runcheck.data.db.entity.NetworkReadingEntity
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.repository.NetworkRepository as NetworkRepositoryContract
import com.runcheck.util.ReleaseSafeLog
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
                    latencyMs = null,
                    estimatedDownstreamKbps = info.estimatedDownstreamKbps,
                    estimatedUpstreamKbps = info.estimatedUpstreamKbps,
                    isMetered = info.isMetered,
                    isRoaming = info.isRoaming,
                    isVpn = info.isVpn,
                    ipAddresses = info.ipAddresses,
                    dnsServers = info.dnsServers,
                    mtuBytes = info.mtuBytes,
                    wifiBssid = info.wifiBssid,
                    wifiStandard = info.wifiStandard
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
        try {
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
        } catch (e: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to save network reading", e)
        }
    }

    override suspend fun getAllReadings(): List<NetworkReading> {
        return networkReadingDao.getAll().map { it.toDomain() }
    }

    override fun getReadingsSince(since: Long, limit: Int?): Flow<List<NetworkReading>> {
        val readingsFlow = if (limit != null) {
            networkReadingDao.getReadingsSinceLimited(since, limit)
        } else {
            networkReadingDao.getReadingsSince(since)
        }
        return readingsFlow.map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        try {
            networkReadingDao.deleteOlderThan(cutoff)
        } catch (e: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to delete old network readings", e)
        }
    }

    override suspend fun deleteAll() {
        networkReadingDao.deleteAll()
    }

    companion object {
        private const val DISPLAY_UPDATE_INTERVAL_MS = 333L
        private const val TAG = "NetworkRepository"
    }
}

private fun NetworkReadingEntity.toDomain() = NetworkReading(
    timestamp = timestamp,
    type = type,
    signalDbm = signalDbm,
    wifiSpeedMbps = wifiSpeedMbps,
    wifiFrequency = wifiFrequency,
    carrier = carrier,
    networkSubtype = networkSubtype,
    latencyMs = latencyMs
)
