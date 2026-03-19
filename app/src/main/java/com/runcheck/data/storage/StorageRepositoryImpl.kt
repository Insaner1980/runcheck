package com.runcheck.data.storage

import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.StorageReadingData
import com.runcheck.domain.repository.StorageRepository as StorageRepositoryContract
import com.runcheck.domain.usecase.CalculateFillRateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
                    fillRateBytesPerDay = fillRate,
                    fillRateEstimate = fillRate?.let { rate ->
                        calculateFillRate.formatEstimate(info.availableBytes, rate)
                    }
                )
            )
            delay(STORAGE_REFRESH_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun saveReading(state: StorageState) {
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
    }

    override suspend fun getAllReadings(): List<StorageReadingData> {
        return storageReadingDao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        storageReadingDao.deleteOlderThan(cutoff)
    }
}

private fun StorageReadingEntity.toDomain() = StorageReadingData(
    timestamp = timestamp,
    totalBytes = totalBytes,
    availableBytes = availableBytes,
    appsBytes = appsBytes,
    mediaBytes = mediaBytes
)
