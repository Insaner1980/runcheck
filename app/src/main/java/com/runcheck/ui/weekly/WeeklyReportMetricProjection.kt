package com.runcheck.ui.weekly

import com.runcheck.domain.model.WeeklyBatterySummary
import com.runcheck.domain.model.WeeklyReportAvailability
import com.runcheck.domain.model.WeeklyThermalSummary

internal sealed interface WeeklyReportMetricProjection {
    data object Available : WeeklyReportMetricProjection

    data object Unavailable : WeeklyReportMetricProjection
}

internal fun batteryProjection(summary: WeeklyBatterySummary): WeeklyReportMetricProjection =
    if (summary.availability == WeeklyReportAvailability.UNAVAILABLE) {
        WeeklyReportMetricProjection.Unavailable
    } else {
        WeeklyReportMetricProjection.Available
    }

internal fun thermalProjection(summary: WeeklyThermalSummary): WeeklyReportMetricProjection =
    if (summary.availability == WeeklyReportAvailability.UNAVAILABLE) {
        WeeklyReportMetricProjection.Unavailable
    } else {
        WeeklyReportMetricProjection.Available
    }
