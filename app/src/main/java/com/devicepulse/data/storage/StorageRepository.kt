package com.devicepulse.data.storage

import com.devicepulse.data.db.dao.StorageReadingDao
import com.devicepulse.data.db.entity.StorageReadingEntity
import com.devicepulse.domain.model.StorageState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storageDataSource: StorageDataSource,
    private val storageReadingDao: StorageReadingDao
) {

    fun getStorageState(): Flow<StorageState> = flow {
        while (true) {
            val info = storageDataSource.getStorageInfo()
            val usagePercent = if (info.totalBytes > 0) {
                (info.usedBytes.toFloat() / info.totalBytes.toFloat()) * 100f
            } else 0f

            val fillRateEstimate = calculateFillRate(info)

            emit(
                StorageState(
                    totalBytes = info.totalBytes,
                    availableBytes = info.availableBytes,
                    usedBytes = info.usedBytes,
                    usagePercent = usagePercent,
                    appsBytes = info.appsBytes,
                    mediaBytes = info.mediaBytes,
                    sdCardAvailable = info.sdCardAvailable,
                    sdCardTotalBytes = info.sdCardTotalBytes,
                    sdCardAvailableBytes = info.sdCardAvailableBytes,
                    fillRateEstimate = fillRateEstimate
                )
            )
            delay(REFRESH_INTERVAL_MS)
        }
    }

    private suspend fun calculateFillRate(currentInfo: StorageDataSource.StorageInfo): String? {
        // Compare with oldest reading to estimate fill rate
        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        // This is a simplified version; a production version would use historical data
        return null
    }

    fun getReadingsSince(since: Long): Flow<List<StorageReadingEntity>> {
        return storageReadingDao.getReadingsSince(since)
    }

    suspend fun saveReading(state: StorageState) {
        val entity = StorageReadingEntity(
            timestamp = System.currentTimeMillis(),
            totalBytes = state.totalBytes,
            availableBytes = state.availableBytes,
            appsBytes = state.appsBytes,
            mediaBytes = state.mediaBytes
        )
        storageReadingDao.insert(entity)
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}
