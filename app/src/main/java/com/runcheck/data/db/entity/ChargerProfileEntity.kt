package com.runcheck.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charger_profiles")
data class ChargerProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val created: Long,
)
