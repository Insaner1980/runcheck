package com.devicepulse.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_battery_usage",
    indices = [Index(value = ["timestamp"]), Index(value = ["package_name"])]
)
data class AppBatteryUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_label") val appLabel: String,
    @ColumnInfo(name = "foreground_time_ms") val foregroundTimeMs: Long,
    @ColumnInfo(name = "estimated_drain_mah") val estimatedDrainMah: Float?
)
