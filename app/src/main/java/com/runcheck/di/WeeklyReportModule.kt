package com.runcheck.di

import com.runcheck.service.report.WeeklyReportNotificationGate
import com.runcheck.service.report.WeeklyReportNotificationGateImpl
import com.runcheck.service.report.WeeklyReportNotifier
import com.runcheck.service.report.WeeklyReportNotifierImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WeeklyReportModule {
    @Binds
    @Singleton
    abstract fun bindWeeklyReportNotifier(impl: WeeklyReportNotifierImpl): WeeklyReportNotifier

    @Binds
    @Singleton
    abstract fun bindWeeklyReportNotificationGate(
        impl: WeeklyReportNotificationGateImpl,
    ): WeeklyReportNotificationGate
}
