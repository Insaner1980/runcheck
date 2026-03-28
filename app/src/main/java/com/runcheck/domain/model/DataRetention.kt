package com.runcheck.domain.model

enum class DataRetention(
    val durationMillis: Long?,
) {
    THREE_MONTHS(90L * 24 * 60 * 60 * 1000),
    SIX_MONTHS(180L * 24 * 60 * 60 * 1000),
    ONE_YEAR(365L * 24 * 60 * 60 * 1000),
    FOREVER(null),
}
