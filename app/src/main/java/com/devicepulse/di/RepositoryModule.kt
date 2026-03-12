package com.devicepulse.di

import com.devicepulse.billing.ProPurchaseManager
import com.devicepulse.data.appusage.AppBatteryUsageRepositoryImpl
import com.devicepulse.data.battery.BatteryRepositoryImpl
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.data.charger.ChargerRepositoryImpl
import com.devicepulse.data.device.DeviceProfileRepositoryImpl
import com.devicepulse.data.export.FileExportRepositoryImpl
import com.devicepulse.data.network.NetworkRepositoryImpl
import com.devicepulse.data.network.SpeedTestRepositoryImpl
import com.devicepulse.data.preferences.UserPreferencesRepositoryImpl
import com.devicepulse.data.storage.StorageRepositoryImpl
import com.devicepulse.data.thermal.ThermalRepositoryImpl
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
    abstract fun bindBatteryRepository(impl: BatteryRepositoryImpl): BatteryRepositoryContract

    @Binds
    @Singleton
    abstract fun bindNetworkRepository(impl: NetworkRepositoryImpl): NetworkRepositoryContract

    @Binds
    @Singleton
    abstract fun bindThermalRepository(impl: ThermalRepositoryImpl): ThermalRepositoryContract

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepositoryContract

    @Binds
    @Singleton
    abstract fun bindSpeedTestRepository(impl: SpeedTestRepositoryImpl): SpeedTestRepositoryContract

    @Binds
    @Singleton
    abstract fun bindProStatusProvider(impl: ProStatusRepository): ProStatusProvider

    @Binds
    @Singleton
    abstract fun bindProPurchaseManager(impl: ProStatusRepository): ProPurchaseManager

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
    abstract fun bindDeviceProfileRepository(impl: DeviceProfileRepositoryImpl): DeviceProfileRepositoryContract

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepositoryContract

    @Binds
    @Singleton
    abstract fun bindFileExportRepository(impl: FileExportRepositoryImpl): FileExportRepository
}
