package com.runcheck.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.runcheck.data.db.dao.AppBatteryUsageDao
import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.dao.ChargerDao
import com.runcheck.data.db.dao.DeviceDao
import com.runcheck.data.db.dao.NetworkReadingDao
import com.runcheck.data.db.dao.SpeedTestResultDao
import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.dao.ThrottlingEventDao
import com.runcheck.data.db.entity.AppBatteryUsageEntity
import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.data.db.entity.ChargerProfileEntity
import com.runcheck.data.db.entity.ChargingSessionEntity
import com.runcheck.data.db.entity.DeviceEntity
import com.runcheck.data.db.entity.NetworkReadingEntity
import com.runcheck.data.db.entity.SpeedTestResultEntity
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.data.db.entity.ThermalReadingEntity
import com.runcheck.data.db.entity.ThrottlingEventEntity

@Database(
    entities = [
        BatteryReadingEntity::class,
        NetworkReadingEntity::class,
        ThermalReadingEntity::class,
        StorageReadingEntity::class,
        DeviceEntity::class,
        ThrottlingEventEntity::class,
        ChargerProfileEntity::class,
        ChargingSessionEntity::class,
        AppBatteryUsageEntity::class,
        SpeedTestResultEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RuncheckDatabase : RoomDatabase() {
    abstract fun batteryReadingDao(): BatteryReadingDao
    abstract fun networkReadingDao(): NetworkReadingDao
    abstract fun thermalReadingDao(): ThermalReadingDao
    abstract fun storageReadingDao(): StorageReadingDao
    abstract fun deviceDao(): DeviceDao
    abstract fun throttlingEventDao(): ThrottlingEventDao
    abstract fun chargerDao(): ChargerDao
    abstract fun appBatteryUsageDao(): AppBatteryUsageDao
    abstract fun speedTestResultDao(): SpeedTestResultDao
}
