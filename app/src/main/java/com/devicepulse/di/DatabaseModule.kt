package com.devicepulse.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devicepulse.data.db.DevicePulseDatabase
import com.devicepulse.data.db.dao.AppBatteryUsageDao
import com.devicepulse.data.db.dao.BatteryReadingDao
import com.devicepulse.data.db.dao.ChargerDao
import com.devicepulse.data.db.dao.DeviceDao
import com.devicepulse.data.db.dao.NetworkReadingDao
import com.devicepulse.data.db.dao.StorageReadingDao
import com.devicepulse.data.db.dao.ThermalReadingDao
import com.devicepulse.data.db.dao.ThrottlingEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `throttling_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `thermal_status` TEXT NOT NULL,
                    `battery_temp_c` REAL NOT NULL,
                    `cpu_temp_c` REAL,
                    `foreground_app` TEXT,
                    `duration_ms` INTEGER
                )"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_throttling_events_timestamp` ON `throttling_events` (`timestamp`)"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `charger_profiles` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `created` INTEGER NOT NULL
                )"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `charging_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `charger_id` INTEGER NOT NULL,
                    `start_time` INTEGER NOT NULL,
                    `end_time` INTEGER,
                    `start_level` INTEGER NOT NULL,
                    `end_level` INTEGER,
                    `avg_current_ma` INTEGER,
                    `max_current_ma` INTEGER,
                    `avg_voltage_mv` INTEGER,
                    `avg_power_mw` INTEGER,
                    `plug_type` TEXT NOT NULL,
                    FOREIGN KEY(`charger_id`) REFERENCES `charger_profiles`(`id`) ON DELETE CASCADE
                )"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_charging_sessions_charger_id` ON `charging_sessions` (`charger_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_charging_sessions_start_time` ON `charging_sessions` (`start_time`)"
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `app_battery_usage` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `package_name` TEXT NOT NULL,
                    `app_label` TEXT NOT NULL,
                    `foreground_time_ms` INTEGER NOT NULL,
                    `estimated_drain_mah` REAL
                )"""
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_app_battery_usage_timestamp` ON `app_battery_usage` (`timestamp`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_app_battery_usage_package_name` ON `app_battery_usage` (`package_name`)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DevicePulseDatabase {
        return Room.databaseBuilder(
            context,
            DevicePulseDatabase::class.java,
            "devicepulse.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideBatteryReadingDao(db: DevicePulseDatabase): BatteryReadingDao =
        db.batteryReadingDao()

    @Provides
    fun provideNetworkReadingDao(db: DevicePulseDatabase): NetworkReadingDao =
        db.networkReadingDao()

    @Provides
    fun provideThermalReadingDao(db: DevicePulseDatabase): ThermalReadingDao =
        db.thermalReadingDao()

    @Provides
    fun provideStorageReadingDao(db: DevicePulseDatabase): StorageReadingDao =
        db.storageReadingDao()

    @Provides
    fun provideDeviceDao(db: DevicePulseDatabase): DeviceDao =
        db.deviceDao()

    @Provides
    fun provideThrottlingEventDao(db: DevicePulseDatabase): ThrottlingEventDao =
        db.throttlingEventDao()

    @Provides
    fun provideChargerDao(db: DevicePulseDatabase): ChargerDao =
        db.chargerDao()

    @Provides
    fun provideAppBatteryUsageDao(db: DevicePulseDatabase): AppBatteryUsageDao =
        db.appBatteryUsageDao()
}
