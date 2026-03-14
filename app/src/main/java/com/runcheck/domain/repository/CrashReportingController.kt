package com.runcheck.domain.repository

interface CrashReportingController {
    suspend fun initialize()
    suspend fun setCollectionEnabled(enabled: Boolean)
}
