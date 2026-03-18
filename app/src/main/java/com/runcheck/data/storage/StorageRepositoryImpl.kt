package com.runcheck.data.storage

import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.StorageReadingData
import com.runcheck.domain.repository.StorageRepository as StorageRepositoryContract
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

private const val STORAGE_REFRESH_INTERVAL_MS = 30_000L
private const val FILL_RATE_MIN_READINGS = 3
private const val FILL_RATE_LOOKBACK_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
private const val DAY_MS = 24L * 60 * 60 * 1000

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storageDataSource: StorageDataSource,
    private val storageReadingDao: StorageReadingDao
) : StorageRepositoryContract {

    override fun getStorageState(): Flow<StorageState> = flow {
        while (true) {
            val info = storageDataSource.getStorageInfo()
            val usagePercent = if (info.totalBytes > 0) {
                (info.usedBytes.toFloat() / info.totalBytes.toFloat()) * 100f
            } else 0f

            val fillRate = calculateFillRate(info)

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
                        if (rate > 0 && info.availableBytes > 0) {
                            formatFillEstimate(info.availableBytes, rate)
                        } else null
                    }
                )
            )
            delay(STORAGE_REFRESH_INTERVAL_MS)
        }
    }

    /**
     * Calculate fill rate (bytes/day) using linear regression on historical readings.
     */
    private suspend fun calculateFillRate(currentInfo: StorageDataSource.StorageInfo): Long? {
        val since = System.currentTimeMillis() - FILL_RATE_LOOKBACK_MS
        val readings = storageReadingDao.getReadingsSinceSync(since)

        if (readings.size < FILL_RATE_MIN_READINGS) return null

        // Linear regression: usedBytes = a * timestamp + b
        val n = readings.size
        val usedValues = readings.map { it.totalBytes - it.availableBytes }
        val times = readings.map { it.timestamp.toDouble() }

        val sumX = times.sum()
        val sumY = usedValues.sumOf { it.toDouble() }
        val sumXY = times.zip(usedValues).sumOf { (x, y) -> x * y }
        val sumX2 = times.sumOf { it * it }

        val denominator = n * sumX2 - sumX * sumX
        if (denominator == 0.0) return null

        val slope = (n * sumXY - sumX * sumY) / denominator // bytes per ms

        val bytesPerDay = (slope * DAY_MS).toLong()
        return bytesPerDay
    }

    private fun formatFillEstimate(availableBytes: Long, bytesPerDay: Long): String {
        val days = availableBytes / bytesPerDay
        return when {
            days < 7 -> "${days}d"
            days < 60 -> "${days / 7}w"
            days < 730 -> "${days / 30}mo"
            else -> "${days / 365}y"
        }
    }

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
