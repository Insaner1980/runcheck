package com.runcheck.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "network_readings",
    indices = [Index(value = ["timestamp"])],
)
data class NetworkReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
    @ColumnInfo(name = "signal_dbm") val signalDbm: Int?,
    @ColumnInfo(name = "wifi_speed_mbps") val wifiSpeedMbps: Int?,
    @ColumnInfo(name = "wifi_frequency") val wifiFrequency: Int?,
    val carrier: String?,
    @ColumnInfo(name = "network_subtype") val networkSubtype: String?,
    @ColumnInfo(name = "latency_ms") val latencyMs: Int?,
)
