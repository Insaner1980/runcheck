package com.runcheck.data.storage

import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.model.MediaBreakdown
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.StorageState
import com.runcheck.util.AppDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.runcheck.domain.repository.StorageRepository as StorageRepositoryContract

private const val STORAGE_REFRESH_INTERVAL_MS = 30_000L
private const val FILL_RATE_LOOKBACK_MS = 7L * 24 * 60 * 60 * 1000
private const val UNAVAILABLE_STORAGE_BYTES = -1L

@Singleton
class StorageRepositoryImpl
    @Inject
    constructor(
        private val storageDataSource: StorageDataSource,
        private val storageReadingDao: StorageReadingDao,
        private val storageGrowthAnalyzer: StorageGrowthAnalyzer,
        private val dispatchers: AppDispatchers,
    ) : StorageRepositoryContract {
        override fun getStorageState(): Flow<StorageState> =
            flow {
                while (true) {
                    val info = storageDataSource.getStorageInfo()
                    val usagePercent =
                        if (info.totalBytes > 0) {
                            (info.usedBytes.toFloat() / info.totalBytes.toFloat()) * 100f
                        } else {
                            0f
                        }

                    val since = System.currentTimeMillis() - FILL_RATE_LOOKBACK_MS
                    val readings =
                        storageReadingDao
                            .getReadingsSinceSync(since)
                            .map { it.toDomain() }
                    val fillRate = storageGrowthAnalyzer.calculateFillRateBytesPerDay(readings)

                    emit(
                        StorageState(
                            totalBytes = info.totalBytes,
                            availableBytes = info.availableBytes,
                            usedBytes = info.usedBytes,
                            usagePercent = usagePercent,
                            appsBytes = info.appsBytes,
                            totalCacheBytes = info.totalCacheBytes,
                            appCount = info.appCount,
                            mediaBreakdown = info.mediaBreakdown,
                            trashInfo = info.trashInfo,
                            removableStorageAvailable = info.removableStorageAvailable,
                            removableStorageTotalBytes = info.removableStorageTotalBytes,
                            removableStorageAvailableBytes = info.removableStorageAvailableBytes,
                            fileSystemType = info.fileSystemType,
                            encryptionStatus = info.encryptionStatus,
                            storageVolumes = info.storageVolumes,
                            fillRateBytesPerDay = fillRate,
                            fillRateEstimate =
                                fillRate?.let { rate ->
                                    storageGrowthAnalyzer.formatEstimate(info.availableBytes, rate)
                                },
                        ),
                    )
                    delay(STORAGE_REFRESH_INTERVAL_MS)
                }
            }.flowOn(dispatchers.io)

        override suspend fun saveReading(state: StorageState) {
            val entity =
                StorageReadingEntity(
                    timestamp = System.currentTimeMillis(),
                    totalBytes = state.totalBytes,
                    availableBytes = state.availableBytes,
                    appsBytes = state.appsBytes ?: UNAVAILABLE_STORAGE_BYTES,
                    mediaBytes = state.mediaBreakdown?.persistedTotalBytes() ?: UNAVAILABLE_STORAGE_BYTES,
                )
            storageReadingDao.insert(entity)
        }

        override fun getReadingsSince(
            since: Long,
            limit: Int?,
        ): Flow<List<StorageReading>> {
            val source =
                if (limit != null) {
                    storageReadingDao.getReadingsSinceLimited(since, limit)
                } else {
                    storageReadingDao.getReadingsSince(since)
                }
            return source
                .map { entities -> entities.map { it.toDomain() } }
                .flowOn(dispatchers.io)
        }

        override suspend fun getReadingsSinceSync(since: Long): List<StorageReading> =
            storageReadingDao.getReadingsSinceSync(since).map { it.toDomain() }

        override suspend fun getAllReadings(): List<StorageReading> = storageReadingDao.getAll().map { it.toDomain() }

        override suspend fun deleteOlderThan(cutoff: Long) = storageReadingDao.deleteOlderThan(cutoff)

        override suspend fun deleteAll() = storageReadingDao.deleteAll()
    }

private fun MediaBreakdown.persistedTotalBytes(): Long {
    var total = 0L
    for (bytes in longArrayOf(imagesBytes, videosBytes, audioBytes, documentsBytes, downloadsBytes)) {
        if (bytes < 0L || bytes > Long.MAX_VALUE - total) return UNAVAILABLE_STORAGE_BYTES
        total += bytes
    }
    return total
}

private fun StorageReadingEntity.toDomain() =
    StorageReading(
        timestamp = timestamp,
        totalBytes = totalBytes,
        availableBytes = availableBytes,
        appsBytes = decodePersistedStorageBytes(appsBytes),
        mediaBytes = decodePersistedStorageBytes(mediaBytes),
    )
