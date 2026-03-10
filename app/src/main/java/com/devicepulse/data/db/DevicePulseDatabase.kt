package com.devicepulse.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devicepulse.data.db.dao.AppBatteryUsageDao
import com.devicepulse.data.db.dao.BatteryReadingDao
import com.devicepulse.data.db.dao.ChargerDao
import com.devicepulse.data.db.dao.DeviceDao
import com.devicepulse.data.db.dao.NetworkReadingDao
import com.devicepulse.data.db.dao.SpeedTestResultDao
import com.devicepulse.data.db.dao.StorageReadingDao
import com.devicepulse.data.db.dao.ThermalReadingDao
import com.devicepulse.data.db.dao.ThrottlingEventDao
import com.devicepulse.data.db.entity.AppBatteryUsageEntity
import com.devicepulse.data.db.entity.BatteryReadingEntity
import com.devicepulse.data.db.entity.ChargerProfileEntity
import com.devicepulse.data.db.entity.ChargingSessionEntity
import com.devicepulse.data.db.entity.DeviceEntity
import com.devicepulse.data.db.entity.NetworkReadingEntity
import com.devicepulse.data.db.entity.SpeedTestResultEntity
import com.devicepulse.data.db.entity.StorageReadingEntity
import com.devicepulse.data.db.entity.ThermalReadingEntity
import com.devicepulse.data.db.entity.ThrottlingEventEntity

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
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DevicePulseDatabase : RoomDatabase() {
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
