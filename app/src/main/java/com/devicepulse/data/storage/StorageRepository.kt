package com.devicepulse.data.storage

import com.devicepulse.data.db.dao.StorageReadingDao
import com.devicepulse.data.db.entity.StorageReadingEntity
import com.devicepulse.domain.model.StorageState
import com.devicepulse.domain.repository.StorageReadingData
import com.devicepulse.domain.repository.StorageRepository as StorageRepositoryContract
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storageDataSource: StorageDataSource,
    private val storageReadingDao: StorageReadingDao
) : StorageRepositoryContract {

    override fun getStorageState(): Flow<StorageState> = flow {
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
        return null
    }

    override suspend fun saveReading(state: StorageState) {
        val entity = StorageReadingEntity(
            timestamp = System.currentTimeMillis(),
            totalBytes = state.totalBytes,
            availableBytes = state.availableBytes,
            appsBytes = state.appsBytes ?: 0L,
            mediaBytes = state.mediaBytes ?: 0L
        )
        storageReadingDao.insert(entity)
    }

    override suspend fun getAllReadings(): List<StorageReadingData> {
        return storageReadingDao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        storageReadingDao.deleteOlderThan(cutoff)
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}

private fun StorageReadingEntity.toDomain() = StorageReadingData(
    timestamp = timestamp,
    totalBytes = totalBytes,
    availableBytes = availableBytes,
    appsBytes = appsBytes,
    mediaBytes = mediaBytes
)
