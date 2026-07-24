package com.runcheck.service.report

import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

open class WeeklyReportTimeProvider
    @Inject
    constructor() {
        internal var overrideClock: Clock? = null
        internal var overrideZoneProvider: (() -> ZoneId)? = null

        val clock: Clock
            get() = overrideClock ?: Clock.systemUTC()

        fun zoneId(): ZoneId = overrideZoneProvider?.invoke() ?: ZoneId.systemDefault()
    }
