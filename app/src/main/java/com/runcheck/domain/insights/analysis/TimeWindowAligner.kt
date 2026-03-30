package com.runcheck.domain.insights.analysis

import javax.inject.Inject

class TimeWindowAligner
    @Inject
    constructor() {
        fun <T> alignLatestContext(
            intervals: List<TimeInterval>,
            contexts: List<T>,
            contextTimestamp: (T) -> Long,
        ): List<AlignedInterval<T>> {
            if (intervals.isEmpty()) return emptyList()

            val sortedContexts = contexts.sortedBy(contextTimestamp)
            var contextIndex = 0
            var latestContext: T? = null

            return intervals.map { interval ->
                while (contextIndex < sortedContexts.size &&
                    contextTimestamp(sortedContexts[contextIndex]) <= interval.endTime
                ) {
                    val candidate = sortedContexts[contextIndex]
                    if (contextTimestamp(candidate) >= interval.startTime) {
                        latestContext = candidate
                    }
                    contextIndex++
                }
                AlignedInterval(
                    startTime = interval.startTime,
                    endTime = interval.endTime,
                    context =
                        latestContext?.takeIf { timestamp ->
                            contextTimestamp(timestamp) in interval.startTime..interval.endTime
                        },
                ).also {
                    if (it.context == null) {
                        latestContext = null
                    }
                }
            }
        }
    }

data class TimeInterval(
    val startTime: Long,
    val endTime: Long,
)

data class AlignedInterval<T>(
    val startTime: Long,
    val endTime: Long,
    val context: T?,
)
