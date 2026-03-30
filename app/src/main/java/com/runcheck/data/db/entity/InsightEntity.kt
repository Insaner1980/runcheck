package com.runcheck.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "insights",
    indices = [
        Index(value = ["generated_at"]),
        Index(value = ["dismissed", "expires_at", "priority"]),
        Index(value = ["rule_id", "dedupe_key"], unique = true),
    ],
)
data class InsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "rule_id") val ruleId: String,
    @ColumnInfo(name = "dedupe_key") val dedupeKey: String,
    val type: String,
    val priority: Int,
    val confidence: Float,
    @ColumnInfo(name = "title_key") val titleKey: String,
    @ColumnInfo(name = "body_key") val bodyKey: String,
    @ColumnInfo(name = "body_args_json") val bodyArgsJson: String,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    @ColumnInfo(name = "data_window_start") val dataWindowStart: Long,
    @ColumnInfo(name = "data_window_end") val dataWindowEnd: Long,
    val target: String,
    val dismissed: Boolean = false,
    val seen: Boolean = false,
)
