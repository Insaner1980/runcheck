package com.devicepulse.di

import com.devicepulse.data.appusage.AppBatteryUsageRepositoryImpl
import com.devicepulse.data.battery.BatteryRepository
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.data.charger.ChargerRepositoryImpl
import com.devicepulse.data.device.DeviceProfileRepository
import com.devicepulse.data.export.FileExportRepositoryImpl
import com.devicepulse.data.network.NetworkRepository
import com.devicepulse.data.network.SpeedTestRepository
import com.devicepulse.data.preferences.UserPreferencesRepository
import com.devicepulse.data.storage.StorageRepository
import com.devicepulse.data.thermal.ThermalRepository
import com.devicepulse.data.thermal.ThrottlingRepositoryImpl
import com.devicepulse.domain.repository.AppBatteryUsageRepository
import com.devicepulse.domain.repository.BatteryRepository as BatteryRepositoryContract
import com.devicepulse.domain.repository.ChargerRepository
import com.devicepulse.domain.repository.DeviceProfileRepository as DeviceProfileRepositoryContract
import com.devicepulse.domain.repository.FileExportRepository
import com.devicepulse.domain.repository.NetworkRepository as NetworkRepositoryContract
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.repository.SpeedTestRepository as SpeedTestRepositoryContract
import com.devicepulse.domain.repository.StorageRepository as StorageRepositoryContract
import com.devicepulse.domain.repository.ThermalRepository as ThermalRepositoryContract
import com.devicepulse.domain.repository.ThrottlingRepository
import com.devicepulse.domain.repository.UserPreferencesRepository as UserPreferencesRepositoryContract
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBatteryRepository(impl: BatteryRepository): BatteryRepositoryContract

    @Binds
    @Singleton
    abstract fun bindNetworkRepository(impl: NetworkRepository): NetworkRepositoryContract

    @Binds
    @Singleton
    abstract fun bindThermalRepository(impl: ThermalRepository): ThermalRepositoryContract

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepository): StorageRepositoryContract

    @Binds
    @Singleton
    abstract fun bindSpeedTestRepository(impl: SpeedTestRepository): SpeedTestRepositoryContract

    @Binds
    @Singleton
    abstract fun bindProStatusProvider(impl: ProStatusRepository): ProStatusProvider

    @Binds
    @Singleton
    abstract fun bindThrottlingRepository(impl: ThrottlingRepositoryImpl): ThrottlingRepository

    @Binds
    @Singleton
    abstract fun bindChargerRepository(impl: ChargerRepositoryImpl): ChargerRepository

    @Binds
    @Singleton
    abstract fun bindAppBatteryUsageRepository(impl: AppBatteryUsageRepositoryImpl): AppBatteryUsageRepository

    @Binds
    @Singleton
    abstract fun bindDeviceProfileRepository(impl: DeviceProfileRepository): DeviceProfileRepositoryContract

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepository): UserPreferencesRepositoryContract

    @Binds
    @Singleton
    abstract fun bindFileExportRepository(impl: FileExportRepositoryImpl): FileExportRepository
}
