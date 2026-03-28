package com.runcheck.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val manufacturer: String,
    val model: String,
    @ColumnInfo(name = "api_level") val apiLevel: Int,
    @ColumnInfo(name = "first_seen") val firstSeen: Long,
    @ColumnInfo(name = "profile_json") val profileJson: String,
)
