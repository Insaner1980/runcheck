package com.devicepulse.di

import android.content.Context
import androidx.room.Room
import com.devicepulse.data.db.DevicePulseDatabase
import com.devicepulse.data.db.dao.BatteryReadingDao
import com.devicepulse.data.db.dao.DeviceDao
import com.devicepulse.data.db.dao.NetworkReadingDao
import com.devicepulse.data.db.dao.StorageReadingDao
import com.devicepulse.data.db.dao.ThermalReadingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DevicePulseDatabase {
        return Room.databaseBuilder(
            context,
            DevicePulseDatabase::class.java,
            "devicepulse.db"
        ).build()
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
}
