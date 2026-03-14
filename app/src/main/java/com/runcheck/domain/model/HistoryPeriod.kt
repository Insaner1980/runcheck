package com.runcheck.domain.model

enum class HistoryPeriod(val durationMs: Long) {
    DAY(24 * 60 * 60 * 1000L),
    WEEK(7 * 24 * 60 * 60 * 1000L),
    MONTH(30L * 24 * 60 * 60 * 1000L),
    ALL(0L)
}
