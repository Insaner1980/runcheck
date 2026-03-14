package com.runcheck.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "storage_readings",
    indices = [Index(value = ["timestamp"])]
)
data class StorageReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "total_bytes") val totalBytes: Long,
    @ColumnInfo(name = "available_bytes") val availableBytes: Long,
    @ColumnInfo(name = "apps_bytes") val appsBytes: Long,
    @ColumnInfo(name = "media_bytes") val mediaBytes: Long
)
