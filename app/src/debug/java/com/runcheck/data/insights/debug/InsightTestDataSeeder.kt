package com.runcheck.data.insights.debug

import com.runcheck.data.db.dao.AppBatteryUsageDao
import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.dao.ChargerDao
import com.runcheck.data.db.dao.InsightDao
import com.runcheck.data.db.dao.NetworkReadingDao
import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.dao.ThrottlingEventDao
import com.runcheck.data.db.entity.AppBatteryUsageEntity
import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.data.db.entity.ChargerProfileEntity
import com.runcheck.data.db.entity.ChargingSessionEntity
import com.runcheck.data.db.entity.NetworkReadingEntity
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.data.db.entity.ThermalReadingEntity
import com.runcheck.data.db.entity.ThrottlingEventEntity
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.repository.DatabaseTransactionRunner
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Singleton
class InsightTestDataSeeder
    @Inject
    constructor(
        private val batteryReadingDao: BatteryReadingDao,
        private val chargerDao: ChargerDao,
        private val networkReadingDao: NetworkReadingDao,
        private val storageReadingDao: StorageReadingDao,
        private val thermalReadingDao: ThermalReadingDao,
        private val throttlingEventDao: ThrottlingEventDao,
        private val appBatteryUsageDao: AppBatteryUsageDao,
        private val insightDao: InsightDao,
        private val transactionRunner: DatabaseTransactionRunner,
    ) {
        suspend fun seed(now: Long) {
            transactionRunner.runInTransaction {
                batteryReadingDao.deleteAll()
                chargerDao.deleteAllSessions()
                chargerDao.deleteAllChargers()
                networkReadingDao.deleteAll()
                storageReadingDao.deleteAll()
                thermalReadingDao.deleteAll()
                throttlingEventDao.deleteAll()
                appBatteryUsageDao.deleteAll()
                insightDao.deleteAll()

                seedBatteryReadings(now)
                seedChargerSessions(now)
                seedNetworkReadings(now)
                seedStorageReadings(now)
                seedThermalReadings(now)
                seedThermalEvents(now)
                seedAppUsage(now)
            }
        }

        private suspend fun seedBatteryReadings(now: Long) {
            val windowMs = 7L * DAY_MS
            val previousIntervalMs = 4L * HOUR_MS
            val currentIntervalMs = 6L * HOUR_MS
            val previousStart = now - (windowMs * 2) - DAY_MS
            val currentStart = now - windowMs

            insertBatteryWindow(
                start = previousStart,
                samples = 48,
                intervalMs = previousIntervalMs,
                startLevel = 96,
                endLevel = 82,
            )
            insertBatteryWindow(
                start = currentStart,
                levels =
                    List(20) { 100 } +
                        listOf(
                            100,
                            99,
                            98,
                            97,
                            97,
                            78,
                            59,
                            40,
                            21,
                        ),
                intervalMs = currentIntervalMs,
            )
        }

        private suspend fun seedChargerSessions(now: Long) {
            val fastChargerId =
                chargerDao.insertCharger(
                    ChargerProfileEntity(
                        name = "Fast Brick",
                        created = now - (30L * DAY_MS),
                    ),
                )
            val deskChargerId =
                chargerDao.insertCharger(
                    ChargerProfileEntity(
                        name = "Desk Charger",
                        created = now - (25L * DAY_MS),
                    ),
                )

            listOf(
                chargingSession(fastChargerId, now - (12L * DAY_MS), 31_000),
                chargingSession(fastChargerId, now - (8L * DAY_MS), 30_000),
                chargingSession(fastChargerId, now - (4L * DAY_MS), 32_000),
                chargingSession(deskChargerId, now - (6L * DAY_MS), 18_000),
                chargingSession(deskChargerId, now - (2L * DAY_MS), 17_000),
                chargingSession(deskChargerId, now - DAY_MS, 18_500),
            ).forEach { session ->
                chargerDao.insertSession(session)
            }
        }

        private suspend fun insertBatteryWindow(
            start: Long,
            samples: Int,
            intervalMs: Long,
            startLevel: Int,
            endLevel: Int,
        ) {
            val lastIndex = (samples - 1).coerceAtLeast(1)
            repeat(samples) { index ->
                val progress = index / lastIndex.toFloat()
                val level = interpolateInt(startLevel, endLevel, progress)
                batteryReadingDao.insert(
                    BatteryReadingEntity(
                        timestamp = start + (index * intervalMs),
                        level = level,
                        voltageMv = 4020 - (index * 3),
                        temperatureC = 29.5f + ((index % 5) * 0.2f),
                        currentMa = -420 - (index * 2),
                        currentConfidence = "HIGH",
                        status = "DISCHARGING",
                        plugType = "NONE",
                        health = "GOOD",
                        cycleCount = 312,
                        healthPct = 90,
                    ),
                )
            }
        }

        private suspend fun insertBatteryWindow(
            start: Long,
            levels: List<Int>,
            intervalMs: Long,
        ) {
            levels.forEachIndexed { index, level ->
                batteryReadingDao.insert(
                    BatteryReadingEntity(
                        timestamp = start + (index * intervalMs),
                        level = level,
                        voltageMv = 4020 - (index * 3),
                        temperatureC = 29.5f + ((index % 5) * 0.2f),
                        currentMa = -420 - (index * 3),
                        currentConfidence = "HIGH",
                        status = "DISCHARGING",
                        plugType = "NONE",
                        health = "GOOD",
                        cycleCount = 312,
                        healthPct = 90,
                    ),
                )
            }
        }

        private suspend fun seedNetworkReadings(now: Long) {
            val currentStart = now - (7L * DAY_MS)
            val intervalMs = 6L * HOUR_MS
            val cellularSignals =
                listOf(-112, -98, -97, -95, -113, -116, -118, -115)
            cellularSignals.forEachIndexed { index, signalDbm ->
                networkReadingDao.insert(
                    NetworkReadingEntity(
                        timestamp = currentStart + ((20 + index) * intervalMs),
                        type = "CELLULAR",
                        signalDbm = signalDbm,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier Demo",
                        networkSubtype = "LTE",
                        latencyMs = if (signalDbm <= -110) 145 else 68,
                    ),
                )
            }
        }

        private suspend fun seedStorageReadings(now: Long) {
            val totalBytes = 128L * GIB
            val availableSeries =
                listOf(
                    20L * GIB,
                    17L * GIB,
                    14L * GIB,
                    11L * GIB,
                    8L * GIB,
                    5L * GIB,
                )
            availableSeries.forEachIndexed { index, availableBytes ->
                val usedBytes = totalBytes - availableBytes
                storageReadingDao.insert(
                    StorageReadingEntity(
                        timestamp = now - ((availableSeries.lastIndex - index) * 2L * DAY_MS),
                        totalBytes = totalBytes,
                        availableBytes = availableBytes,
                        appsBytes = (usedBytes * 0.62).roundToLongSafe(),
                        mediaBytes = (usedBytes * 0.28).roundToLongSafe(),
                    ),
                )
            }
        }

        private suspend fun seedThermalReadings(now: Long) {
            val currentStart = now - (7L * DAY_MS)
            val intervalMs = 6L * HOUR_MS
            val profile =
                listOf(
                    Triple(40.5f, 68.0f, ThermalStatus.MODERATE),
                    Triple(33.7f, 57.0f, ThermalStatus.NONE),
                    Triple(34.4f, 59.0f, ThermalStatus.LIGHT),
                    Triple(34.2f, 58.5f, ThermalStatus.LIGHT),
                    Triple(41.0f, 69.0f, ThermalStatus.SEVERE),
                    Triple(42.5f, 72.0f, ThermalStatus.CRITICAL),
                    Triple(43.2f, 74.0f, ThermalStatus.CRITICAL),
                    Triple(42.8f, 71.0f, ThermalStatus.SEVERE),
                )
            profile.forEachIndexed { index, (batteryTempC, cpuTempC, thermalStatus) ->
                thermalReadingDao.insert(
                    ThermalReadingEntity(
                        timestamp = currentStart + ((20 + index) * intervalMs),
                        batteryTempC = batteryTempC,
                        cpuTempC = cpuTempC,
                        thermalStatus = thermalStatus.ordinal,
                        throttling = thermalStatus >= ThermalStatus.SEVERE,
                    ),
                )
            }
        }

        private suspend fun seedThermalEvents(now: Long) {
            val events =
                listOf(
                    ThrottlingEventEntity(
                        timestamp = now - (6L * DAY_MS),
                        thermalStatus = ThermalStatus.SEVERE.name,
                        batteryTempC = 41.8f,
                        cpuTempC = 71.4f,
                        foregroundApp = "StreamBox",
                        durationMs = 4L * 60L * 1000L,
                    ),
                    ThrottlingEventEntity(
                        timestamp = now - (4L * DAY_MS),
                        thermalStatus = ThermalStatus.CRITICAL.name,
                        batteryTempC = 43.1f,
                        cpuTempC = 75.6f,
                        foregroundApp = "StreamBox",
                        durationMs = 6L * 60L * 1000L,
                    ),
                    ThrottlingEventEntity(
                        timestamp = now - (2L * DAY_MS),
                        thermalStatus = ThermalStatus.SEVERE.name,
                        batteryTempC = 42.4f,
                        cpuTempC = 73.0f,
                        foregroundApp = "Open World Arena",
                        durationMs = 5L * 60L * 1000L,
                    ),
                    ThrottlingEventEntity(
                        timestamp = now - (18L * HOUR_MS),
                        thermalStatus = ThermalStatus.SEVERE.name,
                        batteryTempC = 44.0f,
                        cpuTempC = 74.2f,
                        foregroundApp = "Open World Arena",
                        durationMs = 7L * 60L * 1000L,
                    ),
                )
            for (event in events) {
                throttlingEventDao.insert(event)
            }
        }

        private suspend fun seedAppUsage(now: Long) {
            appBatteryUsageDao.insertAll(
                listOf(
                    AppBatteryUsageEntity(
                        timestamp = now - (20L * HOUR_MS),
                        packageName = "com.demo.streambox",
                        appLabel = "StreamBox",
                        foregroundTimeMs = 2L * HOUR_MS,
                        estimatedDrainMah = null,
                    ),
                    AppBatteryUsageEntity(
                        timestamp = now - (4L * HOUR_MS),
                        packageName = "com.demo.streambox",
                        appLabel = "StreamBox",
                        foregroundTimeMs = 2L * HOUR_MS + (30L * MINUTE_MS),
                        estimatedDrainMah = null,
                    ),
                    AppBatteryUsageEntity(
                        timestamp = now - (11L * HOUR_MS),
                        packageName = "com.demo.mailbox",
                        appLabel = "Mailbox",
                        foregroundTimeMs = 45L * MINUTE_MS,
                        estimatedDrainMah = null,
                    ),
                    AppBatteryUsageEntity(
                        timestamp = now - (7L * HOUR_MS),
                        packageName = "com.demo.maps",
                        appLabel = "City Maps",
                        foregroundTimeMs = 30L * MINUTE_MS,
                        estimatedDrainMah = null,
                    ),
                ),
            )
        }

        private fun interpolateInt(
            start: Int,
            end: Int,
            progress: Float,
        ): Int = (start + ((end - start) * progress)).roundToInt()

        private fun chargingSession(
            chargerId: Long,
            endTime: Long,
            avgPowerMw: Int,
        ) = ChargingSessionEntity(
            chargerId = chargerId,
            startTime = endTime - (50L * MINUTE_MS),
            endTime = endTime,
            startLevel = 25,
            endLevel = 80,
            avgCurrentMa = null,
            maxCurrentMa = null,
            avgVoltageMv = null,
            avgPowerMw = avgPowerMw,
            plugType = "AC",
        )

        private fun Double.roundToLongSafe(): Long = roundToLong()

        private companion object {
            const val MINUTE_MS = 60_000L
            const val HOUR_MS = 60L * MINUTE_MS
            const val DAY_MS = 24L * HOUR_MS
            const val GIB = 1024L * 1024L * 1024L
        }
    }
