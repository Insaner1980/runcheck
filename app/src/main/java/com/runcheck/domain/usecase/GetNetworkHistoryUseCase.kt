package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.repository.NetworkReadingData
import com.runcheck.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNetworkHistoryUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<NetworkReadingData>> {
        val since = if (period == HistoryPeriod.ALL) 0L
            else System.currentTimeMillis() - period.durationMs
        val limit = if (period == HistoryPeriod.ALL) MAX_HISTORY_POINTS else null
        return networkRepository.getReadingsSince(since, limit)
    }

    companion object {
        private const val MAX_HISTORY_POINTS = 5_000
    }
}
