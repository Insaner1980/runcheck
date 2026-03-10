package com.devicepulse.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "speed_test_results",
    indices = [Index(value = ["timestamp"])]
)
data class SpeedTestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "download_mbps") val downloadMbps: Double,
    @ColumnInfo(name = "upload_mbps") val uploadMbps: Double,
    @ColumnInfo(name = "ping_ms") val pingMs: Int,
    @ColumnInfo(name = "jitter_ms") val jitterMs: Int?,
    @ColumnInfo(name = "server_name") val serverName: String?,
    @ColumnInfo(name = "server_location") val serverLocation: String?,
    @ColumnInfo(name = "connection_type") val connectionType: String,
    @ColumnInfo(name = "network_subtype") val networkSubtype: String?,
    @ColumnInfo(name = "signal_dbm") val signalDbm: Int?
)
