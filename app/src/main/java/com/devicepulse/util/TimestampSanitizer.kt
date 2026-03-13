package com.devicepulse.util

object TimestampSanitizer {
    fun clampToNow(timestamp: Long, now: Long = System.currentTimeMillis()): Long =
        timestamp.coerceAtMost(now)

    fun isUsable(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean =
        timestamp in 0..now
}
