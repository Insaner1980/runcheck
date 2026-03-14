package com.runcheck.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "thermal_readings",
    indices = [Index(value = ["timestamp"])]
)
data class ThermalReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "battery_temp_c") val batteryTempC: Float,
    @ColumnInfo(name = "cpu_temp_c") val cpuTempC: Float?,
    @ColumnInfo(name = "thermal_status") val thermalStatus: Int,
    val throttling: Boolean
)
