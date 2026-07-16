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
import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.insights.analysis.TimeWindowAligner
import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.rules.BaselineAnomalyRule
import com.runcheck.domain.insights.rules.BatteryDegradationTrendRule
import com.runcheck.domain.insights.rules.ChargerPerformanceRule
import com.runcheck.domain.insights.rules.HeatAcceleratedBatteryWearRule
import com.runcheck.domain.insights.rules.HeavyAppUsageRule
import com.runcheck.domain.insights.rules.NetworkDrivenBatteryDrainRule
import com.runcheck.domain.insights.rules.NetworkSignalPatternRule
import com.runcheck.domain.insights.rules.RecurringThermalThrottlingRule
import com.runcheck.domain.insights.rules.StoragePressureImpactRule
import com.runcheck.domain.insights.rules.StoragePressureProjectionRule
import com.runcheck.domain.insights.rules.ThermalPatternDetectionRule
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ChargerProfile
import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.ThrottlingRepository
import com.runcheck.domain.scoring.HealthScoreCalculator
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightTestDataSeederTest {
    @Test
    fun seedIsDeterministicAndTriggersEveryProductionRule() =
        runTest {
            val first = captureSeed()
            val second = captureSeed()

            assertEquals(first, second)

            val candidatesByRule = first.rules().associate { rule -> rule.ruleId to rule.evaluate(NOW) }
            val missingRules = candidatesByRule.filterValues(List<*>::isEmpty).keys
            val belowEngineThreshold =
                candidatesByRule
                    .filterValues { candidates -> candidates.any { it.confidence < MINIMUM_ENGINE_CONFIDENCE } }
                    .keys

            assertTrue("Seeder did not trigger rules: $missingRules", missingRules.isEmpty())
            assertTrue(
                "Seeder produced candidates below the engine confidence threshold: $belowEngineThreshold",
                belowEngineThreshold.isEmpty(),
            )
        }

    private suspend fun captureSeed(): SeededData {
        val batteryDao = mockk<BatteryReadingDao>(relaxed = true)
        val chargerDao = mockk<ChargerDao>(relaxed = true)
        val networkDao = mockk<NetworkReadingDao>(relaxed = true)
        val storageDao = mockk<StorageReadingDao>(relaxed = true)
        val thermalDao = mockk<ThermalReadingDao>(relaxed = true)
        val throttlingDao = mockk<ThrottlingEventDao>(relaxed = true)
        val appUsageDao = mockk<AppBatteryUsageDao>(relaxed = true)
        val insightDao = mockk<InsightDao>(relaxed = true)
        val batteryReadings = mutableListOf<BatteryReadingEntity>()
        val chargers = mutableListOf<ChargerProfileEntity>()
        val chargingSessions = mutableListOf<ChargingSessionEntity>()
        val networkReadings = mutableListOf<NetworkReadingEntity>()
        val storageReadings = mutableListOf<StorageReadingEntity>()
        val thermalReadings = mutableListOf<ThermalReadingEntity>()
        val throttlingEvents = mutableListOf<ThrottlingEventEntity>()
        val appUsageBatches = mutableListOf<List<AppBatteryUsageEntity>>()

        coEvery { batteryDao.insert(capture(batteryReadings)) } just runs
        coEvery { chargerDao.insertCharger(capture(chargers)) } returnsMany listOf(FAST_CHARGER_ID, DESK_CHARGER_ID)
        coEvery { chargerDao.insertSession(capture(chargingSessions)) } returns 1L
        coEvery { networkDao.insert(capture(networkReadings)) } just runs
        coEvery { storageDao.insert(capture(storageReadings)) } just runs
        coEvery { thermalDao.insert(capture(thermalReadings)) } just runs
        coEvery { throttlingDao.insert(capture(throttlingEvents)) } returns 1L
        coEvery { appUsageDao.insertAll(capture(appUsageBatches)) } just runs

        InsightTestDataSeeder(
            batteryReadingDao = batteryDao,
            chargerDao = chargerDao,
            networkReadingDao = networkDao,
            storageReadingDao = storageDao,
            thermalReadingDao = thermalDao,
            throttlingEventDao = throttlingDao,
            appBatteryUsageDao = appUsageDao,
            insightDao = insightDao,
            transactionRunner = DatabaseTransactionRunner { block -> block() },
        ).seed(NOW)

        return SeededData(
            batteryReadings = batteryReadings,
            chargers = chargers,
            chargingSessions = chargingSessions,
            networkReadings = networkReadings,
            storageReadings = storageReadings,
            thermalReadings = thermalReadings,
            throttlingEvents = throttlingEvents,
            appUsage = appUsageBatches.flatten(),
        )
    }

    private fun SeededData.rules(): List<InsightRule> {
        val batteryRepository = mockk<BatteryRepository>()
        val chargerRepository = mockk<ChargerRepository>()
        val networkRepository = mockk<NetworkRepository>()
        val storageRepository = mockk<StorageRepository>()
        val thermalRepository = mockk<ThermalRepository>()
        val throttlingRepository = mockk<ThrottlingRepository>()
        val appUsageRepository = mockk<AppBatteryUsageRepository>()

        coEvery { batteryRepository.getReadingsSinceSync(any()) } answers {
            batteryReadings.filter { it.timestamp >= firstArg<Long>() }.map { it.toDomain() }
        }
        coEvery { chargerRepository.getChargerProfilesSync() } returns
            chargers.mapIndexed { index, charger -> charger.toDomain(index + 1L) }
        coEvery { chargerRepository.getAllSessionsSync() } returns chargingSessions.map { it.toDomain() }
        coEvery { networkRepository.getReadingsSinceSync(any()) } answers {
            networkReadings.filter { it.timestamp >= firstArg<Long>() }.map { it.toDomain() }
        }
        coEvery { storageRepository.getReadingsSinceSync(any()) } answers {
            storageReadings.filter { it.timestamp >= firstArg<Long>() }.map { it.toDomain() }
        }
        coEvery { thermalRepository.getReadingsSinceSync(any()) } answers {
            thermalReadings.filter { it.timestamp >= firstArg<Long>() }.map { it.toDomain() }
        }
        coEvery { throttlingRepository.getEventsSinceSync(any()) } answers {
            throttlingEvents.filter { it.timestamp >= firstArg<Long>() }.map { it.toDomain() }
        }
        coEvery { appUsageRepository.getUsageSinceSync(any()) } answers {
            appUsage.filter { it.timestamp >= firstArg<Long>() }.map { it.toDomain() }
        }

        val batteryDrainAnalyzer = BatteryDrainAnalyzer()
        val storageGrowthAnalyzer = StorageGrowthAnalyzer()
        val timeWindowAligner = TimeWindowAligner()
        return listOf(
            BatteryDegradationTrendRule(batteryRepository, batteryDrainAnalyzer),
            ChargerPerformanceRule(chargerRepository),
            StoragePressureProjectionRule(storageRepository, storageGrowthAnalyzer),
            RecurringThermalThrottlingRule(throttlingRepository),
            HeavyAppUsageRule(appUsageRepository),
            NetworkSignalPatternRule(networkRepository),
            NetworkDrivenBatteryDrainRule(
                batteryRepository,
                networkRepository,
                batteryDrainAnalyzer,
                timeWindowAligner,
            ),
            HeatAcceleratedBatteryWearRule(
                batteryRepository,
                thermalRepository,
                batteryDrainAnalyzer,
                timeWindowAligner,
            ),
            StoragePressureImpactRule(storageRepository, storageGrowthAnalyzer, HealthScoreCalculator()),
            ThermalPatternDetectionRule(thermalRepository),
            BaselineAnomalyRule(batteryRepository, batteryDrainAnalyzer),
        )
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

    private fun ChargerProfileEntity.toDomain(id: Long) = ChargerProfile(id = id, name = name, created = created)

    private fun ChargingSessionEntity.toDomain() =
        ChargingSession(
            id = id,
            chargerId = chargerId,
            startTime = startTime,
            endTime = endTime,
            startLevel = startLevel,
            endLevel = endLevel,
            avgCurrentMa = avgCurrentMa,
            maxCurrentMa = maxCurrentMa,
            avgVoltageMv = avgVoltageMv,
            avgPowerMw = avgPowerMw,
            plugType = plugType,
        )

    private fun NetworkReadingEntity.toDomain() =
        NetworkReading(timestamp, type, signalDbm, wifiSpeedMbps, wifiFrequency, carrier, networkSubtype, latencyMs)

    private fun StorageReadingEntity.toDomain() =
        StorageReading(timestamp, totalBytes, availableBytes, appsBytes, mediaBytes)

    private fun ThermalReadingEntity.toDomain() =
        ThermalReading(timestamp, batteryTempC, cpuTempC, thermalStatus, throttling)

    private fun ThrottlingEventEntity.toDomain() =
        ThrottlingEvent(id, timestamp, thermalStatus, batteryTempC, cpuTempC, foregroundApp, durationMs)

    private fun AppBatteryUsageEntity.toDomain() =
        AppBatteryUsage(id, timestamp, packageName, appLabel, foregroundTimeMs, estimatedDrainMah)

    private data class SeededData(
        val batteryReadings: List<BatteryReadingEntity>,
        val chargers: List<ChargerProfileEntity>,
        val chargingSessions: List<ChargingSessionEntity>,
        val networkReadings: List<NetworkReadingEntity>,
        val storageReadings: List<StorageReadingEntity>,
        val thermalReadings: List<ThermalReadingEntity>,
        val throttlingEvents: List<ThrottlingEventEntity>,
        val appUsage: List<AppBatteryUsageEntity>,
    )

    private companion object {
        const val NOW = 1_800_000_000_000L
        const val FAST_CHARGER_ID = 1L
        const val DESK_CHARGER_ID = 2L
        const val MINIMUM_ENGINE_CONFIDENCE = 0.6f
    }
}
