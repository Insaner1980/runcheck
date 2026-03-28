package com.runcheck.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "charging_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ChargerProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["charger_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["charger_id"]),
        Index(value = ["start_time"]),
        Index(value = ["end_time"]),
    ],
)
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "charger_id") val chargerId: Long,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long?,
    @ColumnInfo(name = "start_level") val startLevel: Int,
    @ColumnInfo(name = "end_level") val endLevel: Int?,
    @ColumnInfo(name = "avg_current_ma") val avgCurrentMa: Int?,
    @ColumnInfo(name = "max_current_ma") val maxCurrentMa: Int?,
    @ColumnInfo(name = "avg_voltage_mv") val avgVoltageMv: Int?,
    @ColumnInfo(name = "avg_power_mw") val avgPowerMw: Int?,
    @ColumnInfo(name = "plug_type") val plugType: String,
)
