package com.runcheck.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_readings",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["status", "timestamp"])
    ]
)
data class BatteryReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val level: Int,
    @ColumnInfo(name = "voltage_mv") val voltageMv: Int,
    @ColumnInfo(name = "temperature_c") val temperatureC: Float,
    @ColumnInfo(name = "current_ma") val currentMa: Int?,
    @ColumnInfo(name = "current_confidence") val currentConfidence: String,
    val status: String,
    @ColumnInfo(name = "plug_type") val plugType: String,
    val health: String,
    @ColumnInfo(name = "cycle_count") val cycleCount: Int?,
    @ColumnInfo(name = "health_pct") val healthPct: Int?
)
