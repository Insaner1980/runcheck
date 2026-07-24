package com.runcheck.service.report

import com.runcheck.domain.model.WeeklyReport
import com.runcheck.service.monitor.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton

fun interface WeeklyReportNotificationGate {
    fun canNotify(): Boolean
}

interface WeeklyReportNotifier {
    suspend fun show(report: WeeklyReport): Boolean
}

@Singleton
class WeeklyReportNotificationGateImpl
    @Inject
    constructor(
        private val notificationHelper: NotificationHelper,
    ) : WeeklyReportNotificationGate {
        override fun canNotify(): Boolean = notificationHelper.canPostReports()
    }

@Singleton
class WeeklyReportNotifierImpl
    @Inject
    constructor(
        private val notificationHelper: NotificationHelper,
    ) : WeeklyReportNotifier {
        override suspend fun show(report: WeeklyReport): Boolean =
            notificationHelper.showWeeklyReport(report.coverage.monitoredDays)
    }
