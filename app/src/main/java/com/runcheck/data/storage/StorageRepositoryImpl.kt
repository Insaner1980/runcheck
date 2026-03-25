package com.runcheck.data.storage

import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.StorageRepository as StorageRepositoryContract
import com.runcheck.domain.usecase.CalculateFillRateUseCase
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val STORAGE_REFRESH_INTERVAL_MS = 30_000L
private const val FILL_RATE_LOOKBACK_MS = 7L * 24 * 60 * 60 * 1000

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storageDataSource: StorageDataSource,
    private val storageReadingDao: StorageReadingDao,
    private val calculateFillRate: CalculateFillRateUseCase
) : StorageRepositoryContract {

    override fun getStorageState(): Flow<StorageState> = flow {
        while (true) {
            val info = storageDataSource.getStorageInfo()
            val usagePercent = if (info.totalBytes > 0) {
                (info.usedBytes.toFloat() / info.totalBytes.toFloat()) * 100f
            } else 0f

            val since = System.currentTimeMillis() - FILL_RATE_LOOKBACK_MS
            val readings = storageReadingDao.getReadingsSinceSync(since)
                .map { it.toDomain() }
            val fillRate = calculateFillRate(readings)

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
                    sdCardAvailable = info.sdCardAvailable,
                    sdCardTotalBytes = info.sdCardTotalBytes,
                    sdCardAvailableBytes = info.sdCardAvailableBytes,
                    fileSystemType = info.fileSystemType,
                    encryptionStatus = info.encryptionStatus,
                    storageVolumes = info.storageVolumes,
                    fillRateBytesPerDay = fillRate,
                    fillRateEstimate = fillRate?.let { rate ->
                        calculateFillRate.formatEstimate(info.availableBytes, rate)
                    }
                )
            )
            delay(STORAGE_REFRESH_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveReading(state: StorageState) = withContext(Dispatchers.IO) {
        try {
            val entity = StorageReadingEntity(
                timestamp = System.currentTimeMillis(),
                totalBytes = state.totalBytes,
                availableBytes = state.availableBytes,
                appsBytes = state.appsBytes ?: 0L,
                mediaBytes = state.mediaBreakdown?.let {
                    it.imagesBytes + it.videosBytes + it.audioBytes + it.documentsBytes + it.downloadsBytes
                } ?: 0L
            )
            storageReadingDao.insert(entity)
        } catch (e: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to save storage reading", e)
        }
    }

    override fun getReadingsSince(since: Long, limit: Int?): Flow<List<StorageReading>> {
        val source = if (limit != null) {
            storageReadingDao.getReadingsSinceLimited(since, limit)
        } else {
            storageReadingDao.getReadingsSince(since)
        }
        return source.map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getAllReadings(): List<StorageReading> = withContext(Dispatchers.IO) {
        storageReadingDao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteOlderThan(cutoff: Long) = withContext(Dispatchers.IO) {
        try {
            storageReadingDao.deleteOlderThan(cutoff)
        } catch (e: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to delete old storage readings", e)
        }
    }

    override suspend fun deleteAll() = withContext<Unit>(Dispatchers.IO) {
        storageReadingDao.deleteAll()
    }

    private companion object {
        const val TAG = "StorageRepository"
    }
}

private fun StorageReadingEntity.toDomain() = StorageReading(
    timestamp = timestamp,
    totalBytes = totalBytes,
    availableBytes = availableBytes,
    appsBytes = appsBytes,
    mediaBytes = mediaBytes
)
