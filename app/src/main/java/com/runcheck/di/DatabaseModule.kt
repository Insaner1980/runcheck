package com.runcheck.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.runcheck.BuildConfig
import com.runcheck.data.db.RuncheckDatabase
import com.runcheck.data.db.dao.AppBatteryUsageDao
import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.dao.ChargerDao
import com.runcheck.data.db.dao.DeviceDao
import com.runcheck.data.db.dao.NetworkReadingDao
import com.runcheck.data.db.dao.SpeedTestResultDao
import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.dao.ThrottlingEventDao
import com.runcheck.util.ReleaseSafeLog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
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
                )""",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_throttling_events_timestamp` ON `throttling_events` (`timestamp`)",
                )
            }
        }

    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `charger_profiles` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `created` INTEGER NOT NULL
                )""",
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
                )""",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_charging_sessions_charger_id` ON `charging_sessions` (`charger_id`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_charging_sessions_start_time` ON `charging_sessions` (`start_time`)",
                )
            }
        }

    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `app_battery_usage` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `package_name` TEXT NOT NULL,
                    `app_label` TEXT NOT NULL,
                    `foreground_time_ms` INTEGER NOT NULL,
                    `estimated_drain_mah` REAL
                )""",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_app_battery_usage_timestamp` ON `app_battery_usage` (`timestamp`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_app_battery_usage_package_name` ON `app_battery_usage` (`package_name`)",
                )
            }
        }

    private val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `speed_test_results` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `download_mbps` REAL NOT NULL,
                    `upload_mbps` REAL NOT NULL,
                    `ping_ms` INTEGER NOT NULL,
                    `jitter_ms` INTEGER,
                    `server_name` TEXT,
                    `server_location` TEXT,
                    `connection_type` TEXT NOT NULL,
                    `network_subtype` TEXT,
                    `signal_dbm` INTEGER
                )""",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_speed_test_results_timestamp` ON `speed_test_results` (`timestamp`)",
                )
            }
        }

    private val MIGRATION_6_7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_battery_readings_status_timestamp` ON `battery_readings` (`status`, `timestamp`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_charging_sessions_end_time` ON `charging_sessions` (`end_time`)",
                )
            }
        }

    private val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate network_readings with nullable signal_dbm (was NOT NULL)
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `network_readings_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `signal_dbm` INTEGER,
                    `wifi_speed_mbps` INTEGER,
                    `wifi_frequency` INTEGER,
                    `carrier` TEXT,
                    `network_subtype` TEXT,
                    `latency_ms` INTEGER
                )""",
                )
                db.execSQL(
                    "INSERT INTO `network_readings_new` SELECT * FROM `network_readings`",
                )
                db.execSQL("DROP TABLE `network_readings`")
                db.execSQL("ALTER TABLE `network_readings_new` RENAME TO `network_readings`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_network_readings_timestamp` ON `network_readings` (`timestamp`)",
                )
            }
        }

    private val MIGRATION_7_8 =
        object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_app_battery_usage_package_name_timestamp` ON `app_battery_usage` (`package_name`, `timestamp`)",
                )
            }
        }

    private val MIGRATION_8_9 =
        object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop redundant single-column index — the composite
                // (package_name, timestamp) index covers all the same lookups.
                db.execSQL(
                    "DROP INDEX IF EXISTS `index_app_battery_usage_package_name`",
                )
            }
        }

    val ALL_MIGRATIONS =
        arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
        )

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): RuncheckDatabase {
        val database =
            Room
                .databaseBuilder(
                    context,
                    RuncheckDatabase::class.java,
                    BuildConfig.ROOM_DB_NAME,
                ).addMigrations(*ALL_MIGRATIONS)
                .addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            ReleaseSafeLog.error(
                                TAG,
                                "Database destructive migration triggered — historical data lost",
                            )
                            context
                                .getSharedPreferences(DB_EVENT_PREFS, Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean(KEY_DB_RESET, true)
                                .apply()
                        }
                    },
                ).build()

        return database
    }

    private const val TAG = "DatabaseModule"
    const val DB_EVENT_PREFS = "runcheck_db_events"
    const val KEY_DB_RESET = "destructive_migration_occurred"

    @Provides
    @Singleton
    fun provideBatteryReadingDao(db: RuncheckDatabase): BatteryReadingDao = db.batteryReadingDao()

    @Provides
    @Singleton
    fun provideNetworkReadingDao(db: RuncheckDatabase): NetworkReadingDao = db.networkReadingDao()

    @Provides
    @Singleton
    fun provideThermalReadingDao(db: RuncheckDatabase): ThermalReadingDao = db.thermalReadingDao()

    @Provides
    @Singleton
    fun provideStorageReadingDao(db: RuncheckDatabase): StorageReadingDao = db.storageReadingDao()

    @Provides
    @Singleton
    fun provideDeviceDao(db: RuncheckDatabase): DeviceDao = db.deviceDao()

    @Provides
    @Singleton
    fun provideThrottlingEventDao(db: RuncheckDatabase): ThrottlingEventDao = db.throttlingEventDao()

    @Provides
    @Singleton
    fun provideChargerDao(db: RuncheckDatabase): ChargerDao = db.chargerDao()

    @Provides
    @Singleton
    fun provideAppBatteryUsageDao(db: RuncheckDatabase): AppBatteryUsageDao = db.appBatteryUsageDao()

    @Provides
    @Singleton
    fun provideSpeedTestResultDao(db: RuncheckDatabase): SpeedTestResultDao = db.speedTestResultDao()
}
