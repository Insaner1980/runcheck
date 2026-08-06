package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

private const val MAX_PRO_HISTORY_POINTS = 5_000

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> ProStatusProvider.proHistoryFlow(
    period: HistoryPeriod,
    loadHistory: (since: Long, limit: Int?) -> Flow<List<T>>,
): Flow<List<T>> =
    isProUser
        .distinctUntilChanged()
        .flatMapLatest { isPro ->
            if (!isPro) {
                flowOf(emptyList())
            } else {
                val since =
                    when (period) {
                        HistoryPeriod.ALL, HistoryPeriod.SINCE_UNPLUG -> 0L
                        else -> System.currentTimeMillis() - period.durationMs
                    }
                val limit = if (period == HistoryPeriod.ALL) MAX_PRO_HISTORY_POINTS else null
                loadHistory(since, limit)
            }
        }
