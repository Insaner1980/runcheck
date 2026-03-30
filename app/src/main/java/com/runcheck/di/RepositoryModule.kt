package com.runcheck.di

import com.runcheck.data.appusage.AppBatteryUsageRepositoryImpl
import com.runcheck.data.battery.BatteryRepositoryImpl
import com.runcheck.data.charger.ChargerRepositoryImpl
import com.runcheck.data.device.DeviceProfileRepositoryImpl
import com.runcheck.data.export.FileExportRepositoryImpl
import com.runcheck.data.insights.InsightRepositoryImpl
import com.runcheck.data.network.NetworkRepositoryImpl
import com.runcheck.data.network.SpeedTestRepositoryImpl
import com.runcheck.data.preferences.MonitoringStatusRepositoryImpl
import com.runcheck.data.preferences.UserPreferencesRepositoryImpl
import com.runcheck.data.storage.StorageCleanupRepositoryImpl
import com.runcheck.data.storage.StorageRepositoryImpl
import com.runcheck.data.thermal.ThermalRepositoryImpl
import com.runcheck.data.thermal.ThrottlingRepositoryImpl
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.FileExportRepository
import com.runcheck.domain.repository.InfoCardDismissalRepository
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.domain.repository.MonitoringStatusRepository
import com.runcheck.domain.repository.StorageCleanupRepository
import com.runcheck.domain.repository.ThrottlingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.runcheck.domain.repository.BatteryRepository as BatteryRepositoryContract
import com.runcheck.domain.repository.DeviceProfileRepository as DeviceProfileRepositoryContract
import com.runcheck.domain.repository.NetworkRepository as NetworkRepositoryContract
import com.runcheck.domain.repository.SpeedTestRepository as SpeedTestRepositoryContract
import com.runcheck.domain.repository.StorageRepository as StorageRepositoryContract
import com.runcheck.domain.repository.ThermalRepository as ThermalRepositoryContract
import com.runcheck.domain.repository.UserPreferencesRepository as UserPreferencesRepositoryContract

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
    abstract fun bindStorageCleanupRepository(impl: StorageCleanupRepositoryImpl): StorageCleanupRepository

    @Binds
    @Singleton
    abstract fun bindSpeedTestRepository(impl: SpeedTestRepositoryImpl): SpeedTestRepositoryContract

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
    abstract fun bindInsightRepository(impl: InsightRepositoryImpl): InsightRepository

    @Binds
    @Singleton
    abstract fun bindDeviceProfileRepository(impl: DeviceProfileRepositoryImpl): DeviceProfileRepositoryContract

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepositoryContract

    @Binds
    @Singleton
    abstract fun bindMonitoringStatusRepository(impl: MonitoringStatusRepositoryImpl): MonitoringStatusRepository

    @Binds
    @Singleton
    abstract fun bindInfoCardDismissalRepository(impl: UserPreferencesRepositoryImpl): InfoCardDismissalRepository

    @Binds
    @Singleton
    abstract fun bindFileExportRepository(impl: FileExportRepositoryImpl): FileExportRepository
}
