package com.runcheck.domain.model

import java.time.Instant

enum class UnusedAppsPeriod(
    val days: Int,
) {
    DAYS_30(30),
    DAYS_60(60),
    DAYS_90(90),
}

enum class UsageAccess {
    GRANTED,
    REQUIRED,
}

enum class UnusedAppError {
    PACKAGE_LABEL,
    STORAGE_PERMISSION,
    STORAGE_IO,
}

data class UnusedAppCandidate(
    val packageName: String,
    val appLabel: String?,
    val firstInstallTime: Instant,
    val lastRecordedUse: Instant?,
    val storageBytes: Long?,
    val errors: Set<UnusedAppError> = emptySet(),
)

data class UnusedAppsResult(
    val usageAccess: UsageAccess,
    val period: UnusedAppsPeriod,
    val observedAt: Instant,
    val candidates: List<UnusedAppCandidate> = emptyList(),
    val partialErrors: Set<UnusedAppError> = emptySet(),
)
