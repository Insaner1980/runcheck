package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetNetworkHistoryUseCase
    @Inject
    constructor(
        private val networkRepository: NetworkRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<NetworkReading>> =
            proStatusProvider.isProUser
                .distinctUntilChanged()
                .flatMapLatest { isPro ->
                    val since =
                        when {
                            !isPro -> {
                                System.currentTimeMillis() - HistoryPeriod.DAY.durationMs
                            }

                            period == HistoryPeriod.ALL || period == HistoryPeriod.SINCE_UNPLUG -> {
                                0L
                            }

                            else -> {
                                System.currentTimeMillis() - period.durationMs
                            }
                        }
                    val limit = if (period == HistoryPeriod.ALL) MAX_HISTORY_POINTS else null
                    networkRepository.getReadingsSince(since, limit)
                }

        companion object {
            private const val MAX_HISTORY_POINTS = 5_000
        }
    }
