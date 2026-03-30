package com.runcheck.data.network

import com.runcheck.data.db.dao.NetworkReadingDao
import com.runcheck.data.db.entity.NetworkReadingEntity
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.runcheck.domain.repository.NetworkRepository as NetworkRepositoryContract

@Singleton
class NetworkRepositoryImpl
    @Inject
    constructor(
        private val networkDataSource: NetworkDataSource,
        private val latencyMeasurer: LatencyMeasurer,
        private val networkReadingDao: NetworkReadingDao,
    ) : NetworkRepositoryContract {
        @OptIn(FlowPreview::class)
        override fun getNetworkState(): Flow<NetworkState> =
            networkDataSource
                .getNetworkInfo()
                .sample(DISPLAY_UPDATE_INTERVAL_MS)
                .map { info ->
                    NetworkState(
                        connectionType = info.connectionType,
                        signalDbm = info.signalDbm,
                        signalAsu = info.signalAsu,
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
                        wifiStandard = info.wifiStandard,
                    )
                }.conflate()

        override suspend fun measureLatency(): Int? {
            if (!networkDataSource.hasValidatedConnection()) {
                return null
            }
            return latencyMeasurer.measureLatency()?.pingMs
        }

        override suspend fun saveReading(state: NetworkState) =
            withContext(Dispatchers.IO) {
                try {
                    val entity =
                        NetworkReadingEntity(
                            timestamp = System.currentTimeMillis(),
                            type = state.connectionType.name,
                            signalDbm = state.signalDbm,
                            wifiSpeedMbps = state.wifiSpeedMbps,
                            wifiFrequency = state.wifiFrequencyMhz,
                            carrier = state.carrier,
                            networkSubtype = state.networkSubtype,
                            latencyMs = state.latencyMs,
                        )
                    networkReadingDao.insert(entity)
                } catch (e: android.database.sqlite.SQLiteException) {
                    ReleaseSafeLog.error(TAG, "Failed to save network reading", e)
                }
            }

        override suspend fun getAllReadings(): List<NetworkReading> =
            withContext(Dispatchers.IO) {
                networkReadingDao.getAll().map { it.toDomain() }
            }

        override fun getReadingsSince(
            since: Long,
            limit: Int?,
        ): Flow<List<NetworkReading>> {
            val readingsFlow =
                if (limit != null) {
                    networkReadingDao.getReadingsSinceLimited(since, limit)
                } else {
                    networkReadingDao.getReadingsSince(since)
                }
            return readingsFlow
                .map { entities ->
                    entities.map { it.toDomain() }
                }.flowOn(Dispatchers.IO)
        }

        override suspend fun getReadingsSinceSync(since: Long): List<NetworkReading> =
            withContext(Dispatchers.IO) {
                networkReadingDao.getReadingsSinceSync(since).map { it.toDomain() }
            }

        override suspend fun deleteOlderThan(cutoff: Long) =
            withContext(Dispatchers.IO) {
                try {
                    networkReadingDao.deleteOlderThan(cutoff)
                } catch (e: android.database.sqlite.SQLiteException) {
                    ReleaseSafeLog.error(TAG, "Failed to delete old network readings", e)
                }
            }

        override suspend fun deleteAll() =
            withContext<Unit>(Dispatchers.IO) {
                networkReadingDao.deleteAll()
            }

        companion object {
            private const val DISPLAY_UPDATE_INTERVAL_MS = 333L
            private const val TAG = "NetworkRepository"
        }
    }

private fun NetworkReadingEntity.toDomain() =
    NetworkReading(
        timestamp = timestamp,
        type = type,
        signalDbm = signalDbm,
        wifiSpeedMbps = wifiSpeedMbps,
        wifiFrequency = wifiFrequency,
        carrier = carrier,
        networkSubtype = networkSubtype,
        latencyMs = latencyMs,
    )
