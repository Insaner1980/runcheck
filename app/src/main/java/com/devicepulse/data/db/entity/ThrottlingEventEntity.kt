package com.devicepulse.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "throttling_events",
    indices = [Index(value = ["timestamp"])]
)
data class ThrottlingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "thermal_status") val thermalStatus: String,
    @ColumnInfo(name = "battery_temp_c") val batteryTempC: Float,
    @ColumnInfo(name = "cpu_temp_c") val cpuTempC: Float?,
    @ColumnInfo(name = "foreground_app") val foregroundApp: String?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long?
)
