package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStorageHistoryUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(period: HistoryPeriod = HistoryPeriod.WEEK): Flow<List<StorageReading>> {
        val since = when (period) {
            HistoryPeriod.ALL, HistoryPeriod.SINCE_UNPLUG -> 0L
            else -> System.currentTimeMillis() - period.durationMs
        }
        val limit = if (period == HistoryPeriod.ALL) 5_000 else null
        return storageRepository.getReadingsSince(since, limit)
    }
}
