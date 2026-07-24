package com.runcheck.data.weekly

import com.runcheck.data.db.dao.AppBatteryUsageDao
import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.dao.SpeedTestResultDao
import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.dao.ThrottlingEventDao
import com.runcheck.data.db.entity.AppBatteryUsageEntity
import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.data.db.entity.SpeedTestResultEntity
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.data.db.entity.ThermalReadingEntity
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.WeeklyReportPeriod
import com.runcheck.domain.model.WeeklyReportSourceData
import com.runcheck.domain.repository.WeeklyReportRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeeklyReportRepositoryImpl
    @Inject
    constructor(
        private val batteryDao: BatteryReadingDao,
        private val storageDao: StorageReadingDao,
        private val thermalDao: ThermalReadingDao,
        private val throttlingDao: ThrottlingEventDao,
        private val speedDao: SpeedTestResultDao,
        private val appUsageDao: AppBatteryUsageDao,
    ) : WeeklyReportRepository {
        override suspend fun loadPeriod(period: WeeklyReportPeriod): WeeklyReportSourceData {
            val start = period.startInclusive.toEpochMilli()
            val end = period.endExclusive.toEpochMilli()
            return WeeklyReportSourceData(
                batteryReadings = batteryDao.getReadingsInPeriod(start, end).map { it.toDomain() },
                storageReadings = storageDao.getReadingsInPeriod(start, end).map { it.toDomain() },
                thermalReadings = thermalDao.getReadingsInPeriod(start, end).map { it.toDomain() },
                throttlingEventTimestamps = throttlingDao.getEventsInPeriod(start, end).map { it.timestamp },
                speedTests = speedDao.getResultsInPeriod(start, end).map { it.toDomain() },
                appUsage = appUsageDao.getUsageInPeriod(start, end).map { it.toDomain() },
            )
        }
    }

private fun BatteryReadingEntity.toDomain() =
    BatteryReading(
        id = id,
        timestamp = timestamp,
        level = level,
        voltageMv = voltageMv,
        temperatureC = temperatureC,
        currentMa = currentMa,
        currentConfidence = currentConfidence,
        status = status,
        plugType = plugType,
        health = health,
        cycleCount = cycleCount,
        healthPct = healthPct,
    )

private fun StorageReadingEntity.toDomain() =
    StorageReading(
        timestamp = timestamp,
        totalBytes = totalBytes,
        availableBytes = availableBytes,
        appsBytes = appsBytes,
        mediaBytes = mediaBytes,
    )

private fun ThermalReadingEntity.toDomain() =
    ThermalReading(
        timestamp = timestamp,
        batteryTempC = batteryTempC,
        cpuTempC = cpuTempC,
        thermalStatus = thermalStatus,
        throttling = throttling,
    )

private fun SpeedTestResultEntity.toDomain() =
    SpeedTestResult(
        id = id,
        timestamp = timestamp,
        downloadMbps = downloadMbps,
        uploadMbps = uploadMbps,
        pingMs = pingMs,
        jitterMs = jitterMs,
        serverName = serverName,
        serverLocation = serverLocation,
        connectionType = enumValueOrNull(connectionType) ?: ConnectionType.NONE,
        networkSubtype = networkSubtype,
        signalDbm = signalDbm,
    )

private fun AppBatteryUsageEntity.toDomain() =
    AppBatteryUsage(
        id = id,
        timestamp = timestamp,
        packageName = packageName,
        appLabel = appLabel,
        foregroundTimeMs = foregroundTimeMs,
        estimatedDrainMah = null,
    )

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
    runCatching { enumValueOf<T>(value) }.getOrNull()
