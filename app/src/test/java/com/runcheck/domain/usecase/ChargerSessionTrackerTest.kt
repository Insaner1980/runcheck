package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargerProfile
import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargerSessionTrackerTest {

    @Test
    fun `starts a new session when selected charger begins charging`() = runTest {
        val chargerRepository = FakeChargerRepository()
        val batteryRepository = FakeBatteryRepository()
        val preferencesRepository = FakeUserPreferencesRepository(selectedChargerId = 7L)
        val tracker = ChargerSessionTracker(chargerRepository, batteryRepository, preferencesRepository)

        tracker.onBatteryState(chargingBatteryState(level = 42), timestamp = 1_000L)

        val session = chargerRepository.activeSession
        requireNotNull(session)
        assertEquals(7L, session.chargerId)
        assertEquals(42, session.startLevel)
        assertNull(session.endTime)
    }

    @Test
    fun `completes an active session using saved battery readings`() = runTest {
        val chargerRepository = FakeChargerRepository().apply {
            activeSession = ChargingSession(
                id = 11L,
                chargerId = 7L,
                startTime = 1_000L,
                endTime = null,
                startLevel = 20,
                endLevel = null,
                avgCurrentMa = null,
                maxCurrentMa = null,
                avgVoltageMv = null,
                avgPowerMw = null,
                plugType = PlugType.USB.name
            )
        }
        val batteryRepository = FakeBatteryRepository(
            readings = listOf(
                reading(timestamp = 2_000L, currentMa = 2000, voltageMv = 5000),
                reading(timestamp = 3_000L, currentMa = 2400, voltageMv = 5100),
                reading(timestamp = 4_000L, currentMa = null, voltageMv = 5000)
            )
        )
        val preferencesRepository = FakeUserPreferencesRepository(selectedChargerId = 7L)
        val tracker = ChargerSessionTracker(chargerRepository, batteryRepository, preferencesRepository)

        tracker.onBatteryState(
            state = chargingBatteryState(level = 78, status = ChargingStatus.NOT_CHARGING),
            timestamp = 5_000L
        )

        assertNull(chargerRepository.activeSession)
        val completed = requireNotNull(chargerRepository.completedSession)
        assertEquals(11L, completed.id)
        assertEquals(5_000L, completed.endTime)
        assertEquals(78, completed.endLevel)
        assertEquals(2200, completed.avgCurrentMa)
        assertEquals(2400, completed.maxCurrentMa)
        assertEquals(5033, completed.avgVoltageMv)
        assertEquals(11_120, completed.avgPowerMw)
    }

    private fun chargingBatteryState(
        level: Int,
        status: ChargingStatus = ChargingStatus.CHARGING
    ) = BatteryState(
        level = level,
        voltageMv = 5000,
        temperatureC = 30f,
        currentMa = MeasuredValue(2200, Confidence.HIGH),
        chargingStatus = status,
        plugType = PlugType.USB,
        health = BatteryHealth.GOOD,
        technology = "Li-ion"
    )

    private fun reading(
        timestamp: Long,
        currentMa: Int?,
        voltageMv: Int
    ) = BatteryReading(
        id = timestamp,
        timestamp = timestamp,
        level = 50,
        voltageMv = voltageMv,
        temperatureC = 30f,
        currentMa = currentMa,
        currentConfidence = Confidence.HIGH.name,
        status = ChargingStatus.CHARGING.name,
        plugType = PlugType.USB.name,
        health = BatteryHealth.GOOD.name,
        cycleCount = null,
        healthPct = null
    )
}

private class FakeChargerRepository : ChargerRepository {
    var activeSession: ChargingSession? = null
    var completedSession: ChargingSession? = null

    override fun getChargerProfiles(): Flow<List<ChargerProfile>> = flowOf(emptyList())

    override fun getAllSessions(): Flow<List<ChargingSession>> = flowOf(emptyList())

    override suspend fun insertCharger(name: String): Long = 0L

    override suspend fun deleteChargerById(id: Long) = Unit

    override suspend fun insertSession(session: ChargingSession): Long {
        activeSession = session.copy(id = 99L)
        return 99L
    }

    override suspend fun completeSession(
        id: Long,
        endTime: Long,
        endLevel: Int,
        avgCurrentMa: Int?,
        maxCurrentMa: Int?,
        avgVoltageMv: Int?,
        avgPowerMw: Int?
    ) {
        completedSession = requireNotNull(activeSession).copy(
            id = id,
            endTime = endTime,
            endLevel = endLevel,
            avgCurrentMa = avgCurrentMa,
            maxCurrentMa = maxCurrentMa,
            avgVoltageMv = avgVoltageMv,
            avgPowerMw = avgPowerMw
        )
        activeSession = null
    }

    override suspend fun getActiveSession(): ChargingSession? = activeSession

    override suspend fun deleteSessionsOlderThan(cutoff: Long) = Unit
}

private class FakeBatteryRepository(
    private val readings: List<BatteryReading> = emptyList()
) : BatteryRepository {
    override fun getBatteryState(): Flow<BatteryState> = emptyFlow()

    override fun getReadingsSince(since: Long, limit: Int?): Flow<List<BatteryReading>> = flowOf(emptyList())

    override suspend fun saveReading(state: BatteryState) = Unit

    override suspend fun getAllReadings(): List<BatteryReading> = readings

    override suspend fun getReadingsSinceSync(since: Long): List<BatteryReading> =
        readings.filter { it.timestamp >= since }

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit

    override suspend fun getLastChargingTimestamp(): Long? = null
}

private class FakeUserPreferencesRepository(
    private var selectedChargerId: Long?
) : UserPreferencesRepository {
    override fun getPreferences() = emptyFlow<com.runcheck.domain.model.UserPreferences>()

    override suspend fun setMonitoringInterval(interval: com.runcheck.domain.model.MonitoringInterval) = Unit

    override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit

    override suspend fun setDataRetention(retention: com.runcheck.domain.model.DataRetention) = Unit

    override suspend fun setCrashReportingEnabled(enabled: Boolean) = Unit

    override fun getPermissionEducationSeen(): Flow<Boolean> = flowOf(false)

    override suspend fun setPermissionEducationSeen(seen: Boolean) = Unit

    override suspend fun getAppUsageLastCollectedAt(): Long? = null

    override suspend fun setAppUsageLastCollectedAt(timestamp: Long) = Unit

    override fun observeSelectedChargerId(): Flow<Long?> = flowOf(selectedChargerId)

    override suspend fun getSelectedChargerId(): Long? = selectedChargerId

    override suspend fun setSelectedChargerId(chargerId: Long?) {
        selectedChargerId = chargerId
    }

    override suspend fun setNotifLowBattery(enabled: Boolean) = Unit

    override suspend fun setNotifHighTemp(enabled: Boolean) = Unit

    override suspend fun setNotifLowStorage(enabled: Boolean) = Unit

    override suspend fun setNotifChargeComplete(enabled: Boolean) = Unit

    override suspend fun setAlertBatteryThreshold(value: Int) = Unit

    override suspend fun setAlertTempThreshold(value: Int) = Unit

    override suspend fun setAlertStorageThreshold(value: Int) = Unit

    override suspend fun setTemperatureUnit(unit: com.runcheck.domain.model.TemperatureUnit) = Unit
}
