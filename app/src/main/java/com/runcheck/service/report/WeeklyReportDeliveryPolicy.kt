package com.runcheck.service.report

import com.runcheck.domain.model.WeeklyReportPeriod

enum class WeeklyReportDeliveryDecision {
    DISABLED,
    PRO_INACTIVE,
    ALREADY_PROCESSED,
    HANDLE_WITHOUT_NOTIFICATION,
    DELIVER,
}

internal fun decideWeeklyReportDelivery(
    period: WeeklyReportPeriod,
    lastProcessedStart: Long?,
    lastProcessedEnd: Long?,
    weeklyEnabled: Boolean,
    hasPro: Boolean,
    notificationsEnabled: Boolean,
    canPostNotifications: Boolean,
    reportsChannelEnabled: Boolean,
): WeeklyReportDeliveryDecision =
    when {
        lastProcessedStart == period.startInclusive.toEpochMilli() &&
            lastProcessedEnd == period.endExclusive.toEpochMilli() -> {
            WeeklyReportDeliveryDecision.ALREADY_PROCESSED
        }

        !weeklyEnabled -> WeeklyReportDeliveryDecision.DISABLED
        !hasPro -> WeeklyReportDeliveryDecision.PRO_INACTIVE
        !notificationsEnabled || !canPostNotifications || !reportsChannelEnabled -> {
            WeeklyReportDeliveryDecision.HANDLE_WITHOUT_NOTIFICATION
        }

        else -> WeeklyReportDeliveryDecision.DELIVER
    }
