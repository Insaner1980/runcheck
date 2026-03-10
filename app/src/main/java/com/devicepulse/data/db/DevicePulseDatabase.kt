package com.devicepulse.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devicepulse.data.db.dao.BatteryReadingDao
import com.devicepulse.data.db.dao.DeviceDao
import com.devicepulse.data.db.dao.NetworkReadingDao
import com.devicepulse.data.db.dao.StorageReadingDao
import com.devicepulse.data.db.dao.ThermalReadingDao
import com.devicepulse.data.db.entity.BatteryReadingEntity
import com.devicepulse.data.db.entity.DeviceEntity
import com.devicepulse.data.db.entity.NetworkReadingEntity
import com.devicepulse.data.db.entity.StorageReadingEntity
import com.devicepulse.data.db.entity.ThermalReadingEntity

@Database(
    entities = [
        BatteryReadingEntity::class,
        NetworkReadingEntity::class,
        ThermalReadingEntity::class,
        StorageReadingEntity::class,
        DeviceEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DevicePulseDatabase : RoomDatabase() {
    abstract fun batteryReadingDao(): BatteryReadingDao
    abstract fun networkReadingDao(): NetworkReadingDao
    abstract fun thermalReadingDao(): ThermalReadingDao
    abstract fun storageReadingDao(): StorageReadingDao
    abstract fun deviceDao(): DeviceDao
}
