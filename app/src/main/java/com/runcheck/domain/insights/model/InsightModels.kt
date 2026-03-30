package com.runcheck.domain.insights.model

enum class InsightType {
    BATTERY,
    THERMAL,
    NETWORK,
    STORAGE,
    CHARGER,
    APP_USAGE,
    CROSS_CATEGORY,
}

enum class InsightPriority(
    val sortOrder: Int,
) {
    HIGH(0),
    MEDIUM(1),
    LOW(2),
}

enum class InsightTarget {
    NONE,
    BATTERY,
    THERMAL,
    NETWORK,
    STORAGE,
    CHARGER,
    APP_USAGE,
}

data class Insight(
    val id: Long,
    val ruleId: String,
    val type: InsightType,
    val priority: InsightPriority,
    val confidence: Float,
    val titleKey: String,
    val bodyKey: String,
    val bodyArgs: List<String>,
    val generatedAt: Long,
    val expiresAt: Long,
    val target: InsightTarget,
    val seen: Boolean,
    val dismissed: Boolean,
)

data class InsightCandidate(
    val ruleId: String,
    val dedupeKey: String,
    val type: InsightType,
    val priority: InsightPriority,
    val confidence: Float,
    val titleKey: String,
    val bodyKey: String,
    val bodyArgs: List<String>,
    val generatedAt: Long,
    val expiresAt: Long,
    val dataWindowStart: Long,
    val dataWindowEnd: Long,
    val target: InsightTarget,
)
