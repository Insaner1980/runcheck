package com.runcheck.domain.repository

import com.runcheck.domain.model.WeeklyReportPeriod
import com.runcheck.domain.model.WeeklyReportSourceData

interface WeeklyReportRepository {
    suspend fun loadPeriod(period: WeeklyReportPeriod): WeeklyReportSourceData
}
