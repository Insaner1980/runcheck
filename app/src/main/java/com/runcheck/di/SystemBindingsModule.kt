package com.runcheck.di

import com.runcheck.billing.ProPurchaseManager
import com.runcheck.data.appusage.AppUsageDataSource
import com.runcheck.data.billing.BillingManager
import com.runcheck.data.crash.CrashReportingManager
import com.runcheck.data.db.RoomTransactionRunner
import com.runcheck.data.device.DeviceProfileProvider
import com.runcheck.data.device.DeviceProfileRepositoryImpl
import com.runcheck.domain.repository.CrashReportingController
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.ScreenStateRepository
import com.runcheck.domain.usecase.TrackThrottlingEventsUseCase
import com.runcheck.service.monitor.MonitorScheduler
import com.runcheck.service.monitor.ScreenStateTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemBindingsModule {

    @Binds
    @Singleton
    abstract fun bindProStatusProvider(impl: BillingManager): ProStatusProvider

    @Binds
    @Singleton
    abstract fun bindProPurchaseManager(impl: BillingManager): ProPurchaseManager

    @Binds
    @Singleton
    abstract fun bindDeviceProfileProvider(impl: DeviceProfileRepositoryImpl): DeviceProfileProvider

    @Binds
    @Singleton
    abstract fun bindCrashReportingController(impl: CrashReportingManager): CrashReportingController

    @Binds
    @Singleton
    abstract fun bindMonitoringScheduler(impl: MonitorScheduler): MonitoringScheduler

    @Binds
    @Singleton
    abstract fun bindScreenStateRepository(impl: ScreenStateTracker): ScreenStateRepository

    @Binds
    @Singleton
    abstract fun bindForegroundAppProvider(
        impl: AppUsageDataSource
    ): TrackThrottlingEventsUseCase.ForegroundAppProvider

    @Binds
    @Singleton
    abstract fun bindDatabaseTransactionRunner(
        impl: RoomTransactionRunner
    ): DatabaseTransactionRunner
}
