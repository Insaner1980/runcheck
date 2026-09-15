package com.runcheck.domain.model

object SpeedTestHistoryPolicy {
    fun resultLimit(isPro: Boolean): Int = if (isPro) 100 else 5
}
