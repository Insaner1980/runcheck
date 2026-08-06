package com.runcheck.service.monitor

import android.content.Context
import android.os.SystemClock
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.MonitoringStatusRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.EvaluateMonitoringAlertsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HealthMonitorWorkerTest {
    private val context: Context = mockk(relaxed = true)
    private val workerParameters: WorkerParameters = mockk(relaxed = true)
    private val batteryRepository: BatteryRepository = mockk(relaxed = true)
    private val networkRepository: NetworkRepository = mockk(relaxed = true)
    private val thermalRepository: ThermalRepository = mockk(relaxed = true)
    private val storageRepository: StorageRepository = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val monitoringStatusRepository: MonitoringStatusRepository = mockk(relaxed = true)
    private val chargerSessionTracker: ChargerSessionTracker = mockk(relaxed = true)
    private val monitoringAlertStateStore: MonitoringAlertStateStore = mockk(relaxed = true)
    private val notificationHelper: NotificationHelper = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.uptimeMillis() } returns 1_000L
    }

    @After
    fun tearDown() {
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `doWork records heartbeat only after successful collection`() =
        runTest {
            val worker = createWorker()

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 1) {
                monitoringStatusRepository.setLastWorkerHeartbeat(
                    match {
                        it.intervalMinutes == UserPreferences().monitoringInterval.minutes &&
                            it.recordedAtUptimeMillis == 1_000L
                    },
                )
            }
            coVerify(exactly = 1) { batteryRepository.saveReading(sampleBatteryState) }
            coVerify(exactly = 1) {
                networkRepository.saveReading(match { it.latencyMs == 23 && it.connectionType == ConnectionType.WIFI })
            }
            coVerify(exactly = 1) { thermalRepository.saveReading(sampleThermalState) }
            coVerify(exactly = 1) { storageRepository.saveReading(sampleStorageState) }
        }

    @Test
    fun `doWork retries and skips heartbeat when core collection fails`() =
        runTest {
            val worker =
                createWorker(
                    batteryStateFlow = failingFlow("battery failed"),
                )

            assertRetryWithoutCoreCollection(worker)
        }

    @Test
    fun `doWork retries before collection when preferences fail to load`() =
        runTest {
            val worker =
                createWorker(
                    preferencesFlow = failingFlow("preferences failed"),
                )

            assertRetryWithoutCoreCollection(worker)
            coVerify(exactly = 0) { networkRepository.saveReading(any()) }
            coVerify(exactly = 0) { thermalRepository.saveReading(any()) }
            coVerify(exactly = 0) { storageRepository.saveReading(any()) }
        }

    @Test
    fun `doWork persists successful readings when thermal collection fails`() =
        runTest {
            val worker =
                createWorker(
                    thermalStateFlow = failingFlow("thermal failed"),
                )

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            coVerify(exactly = 1) { batteryRepository.saveReading(sampleBatteryState) }
            coVerify(exactly = 1) { networkRepository.saveReading(any()) }
            coVerify(exactly = 0) { thermalRepository.saveReading(any()) }
            coVerify(exactly = 1) { storageRepository.saveReading(sampleStorageState) }
            coVerify(exactly = 0) { monitoringAlertStateStore.update(any(), any()) }
        }

    @Test
    fun `doWork stops retrying a persistently broken sensor`() =
        runTest {
            val worker =
                createWorker(
                    runAttemptCount = 3,
                    thermalStateFlow = failingFlow("thermal failed"),
                )

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 1) { batteryRepository.saveReading(sampleBatteryState) }
            coVerify(exactly = 1) { storageRepository.saveReading(sampleStorageState) }
            coVerify(exactly = 0) { monitoringStatusRepository.setLastWorkerHeartbeat(any()) }
        }

    private fun createWorker(
        runAttemptCount: Int = 0,
        preferencesFlow: Flow<UserPreferences> = flowOf(UserPreferences()),
        batteryStateFlow: Flow<BatteryState> = flowOf(sampleBatteryState),
        networkStateFlow: Flow<NetworkState> = flowOf(sampleNetworkState),
        thermalStateFlow: Flow<ThermalState> = flowOf(sampleThermalState),
        storageStateFlow: Flow<StorageState> = flowOf(sampleStorageState),
    ): HealthMonitorWorker {
        every { workerParameters.runAttemptCount } returns runAttemptCount
        every { userPreferencesRepository.getPreferences() } returns preferencesFlow
        every { batteryRepository.getBatteryState() } returns batteryStateFlow
        every { networkRepository.getNetworkState() } returns networkStateFlow
        every { thermalRepository.getThermalState() } returns thermalStateFlow
        every { storageRepository.getStorageState() } returns storageStateFlow
        coEvery { networkRepository.measureLatency() } returns 23
        coEvery { monitoringAlertStateStore.getLastSnapshot() } returns null
        coEvery { monitoringAlertStateStore.wasChargeCompleteFired() } returns false

        return HealthMonitorWorker(
            context = context,
            workerParams = workerParameters,
            batteryRepository = batteryRepository,
            networkRepository = networkRepository,
            thermalRepository = thermalRepository,
            storageRepository = storageRepository,
            userPreferencesRepository = userPreferencesRepository,
            monitoringStatusRepository = monitoringStatusRepository,
            chargerSessionTracker = chargerSessionTracker,
            evaluateMonitoringAlerts = EvaluateMonitoringAlertsUseCase(),
            monitoringAlertStateStore = monitoringAlertStateStore,
            notificationHelper = notificationHelper,
        )
    }

    private fun <T> failingFlow(message: String): Flow<T> = flow { error(message) }

    private suspend fun assertRetryWithoutCoreCollection(worker: HealthMonitorWorker) {
        assertEquals(ListenableWorker.Result.retry(), worker.doWork())
        coVerify(exactly = 0) { monitoringStatusRepository.setLastWorkerHeartbeat(any()) }
        coVerify(exactly = 0) { batteryRepository.saveReading(any()) }
        coVerify(exactly = 0) { monitoringAlertStateStore.update(any(), any()) }
    }

    private companion object {
        val sampleBatteryState =
            BatteryState(
                level = 82,
                voltageMv = 4110,
                temperatureC = 31f,
                currentMa = MeasuredValue(-420, Confidence.HIGH),
                chargingStatus = ChargingStatus.DISCHARGING,
                plugType = PlugType.NONE,
                health = BatteryHealth.GOOD,
                technology = "Li-ion",
            )

        val sampleNetworkState =
            NetworkState(
                connectionType = ConnectionType.WIFI,
                signalDbm = -54,
                signalQuality = SignalQuality.EXCELLENT,
                wifiSsid = "TestWiFi",
            )

        val sampleThermalState =
            ThermalState(
                batteryTempC = 31f,
                cpuTempC = 44f,
                thermalStatus = ThermalStatus.NONE,
                isThrottling = false,
            )

        val sampleStorageState =
            StorageState(
                totalBytes = 128_000_000_000L,
                availableBytes = 64_000_000_000L,
                usedBytes = 64_000_000_000L,
                usagePercent = 50f,
            )
    }
}
