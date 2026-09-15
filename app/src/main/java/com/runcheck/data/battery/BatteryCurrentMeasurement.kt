package com.runcheck.data.battery

private const val MAX_PLAUSIBLE_CURRENT_MA = 10_000

// Integer division intentionally truncates sub-milliamp readings toward zero.
internal fun batteryCurrentMicroampsToMilliamps(rawMicroamps: Int): Int = rawMicroamps / 1000

internal fun isPlausibleBatteryCurrent(milliamps: Int): Boolean =
    milliamps in -MAX_PLAUSIBLE_CURRENT_MA..MAX_PLAUSIBLE_CURRENT_MA
