package com.runcheck.service.monitor

import android.content.Context
import android.os.SystemClock
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.runcheck.data.device.DeviceProfileProvider
import com.runcheck.data.thermal.ThermalDataSource
import com.runcheck.data.thermal.ThermalRepositoryImpl
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
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.MonitoringStatusRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.ThrottlingRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.EvaluateMonitoringAlertsUseCase
import com.runcheck.domain.usecase.TrackThrottlingEventsUseCase
import com.runcheck.util.TestAppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `doWork rethrows cancellation from latency measurement`() =
        runTest {
            val worker = createWorker()
            coEvery { networkRepository.measureLatency(sampleNetworkState.defaultNetworkHandle) } throws
                CancellationException("stopped")

            val thrown = runCatching { worker.doWork() }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            coVerify(exactly = 0) { networkRepository.saveReading(any()) }
        }

    private fun createWorker(
        runAttemptCount: Int = 0,
        preferencesFlow: Flow<UserPreferences> = flowOf(UserPreferences()),
        batteryStateFlow: Flow<BatteryState> = flowOf(sampleBatteryState),
        networkStateFlow: Flow<NetworkState> = flowOf(sampleNetworkState),
        thermalStateFlow: Flow<ThermalState> = flowOf(sampleThermalState),
        storageStateFlow: Flow<StorageState> = flowOf(sampleStorageState),
        chargerSessionTracker: ChargerSessionTracker = this.chargerSessionTracker,
    ): HealthMonitorWorker {
        every { workerParameters.runAttemptCount } returns runAttemptCount
        every { userPreferencesRepository.getPreferences() } returns preferencesFlow
        every { batteryRepository.getBatteryState() } returns batteryStateFlow
        every { networkRepository.getNetworkState() } returns networkStateFlow
        every { thermalRepository.getThermalState() } returns thermalStateFlow
        every { storageRepository.getStorageState() } returns storageStateFlow
        coEvery { networkRepository.measureLatency(sampleNetworkState.defaultNetworkHandle) } returns 23
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

    @Test
    fun `session persistence failure remains retryable`() =
        runTest {
            val sessions = mockk<ChargerRepository>()
            val preferences = mockk<UserPreferencesRepository>()
            coEvery { preferences.getSelectedChargerId() } returns 7L
            coEvery { sessions.getActiveSession() } returns null
            coEvery { sessions.insertSession(any()) } throws IllegalStateException("session database full")
            val tracker = ChargerSessionTracker(sessions, batteryRepository, preferences, mockk())
            val worker = createWorker(
                batteryStateFlow = flowOf(sampleBatteryState.copy(chargingStatus = ChargingStatus.CHARGING)),
                chargerSessionTracker = tracker,
            )

            assertEquals(ListenableWorker.Result.retry(), worker.doWork())
            coVerify(exactly = 0) { monitoringStatusRepository.setLastWorkerHeartbeat(any()) }
        }

    @Test
    fun `real thermal event failure remains retryable`() =
        runTest {
            every { SystemClock.elapsedRealtime() } returns 1_000L
            val events = mockk<ThrottlingRepository>()
            coEvery { events.getOpenEvent() } throws IllegalStateException("event database full")
            val source = mockk<ThermalDataSource>()
            every { source.getBatteryTemperature() } returns flowOf(42f)
            every { source.getCpuTemperature(emptyList()) } returns flowOf(null)
            every { source.getThermalStatus() } returns flowOf(ThermalStatus.SEVERE)
            every { source.getThermalHeadroom() } returns flowOf(null)
            val profile = mockk<DeviceProfileProvider>()
            coEvery { profile.getDeviceProfile() } returns mockk {
                every { thermalZonesAvailable } returns emptyList()
            }
            val repository = ThermalRepositoryImpl(
                source, profile, mockk(),
                TrackThrottlingEventsUseCase(events, mockk()),
                TestAppDispatchers(),
            )
            val worker = createWorker(thermalStateFlow = repository.getThermalState())

            assertEquals(ListenableWorker.Result.retry(), worker.doWork())
            coVerify(exactly = 1) { events.getOpenEvent() }
            coVerify(exactly = 0) { monitoringStatusRepository.setLastWorkerHeartbeat(any()) }
        }

    @Test
    fun `heartbeat persistence remains best effort`() =
        runTest {
            val worker = createWorker()
            coEvery { monitoringStatusRepository.setLastWorkerHeartbeat(any()) } throws
                IllegalStateException("heartbeat database full")

            assertEquals(ListenableWorker.Result.success(), worker.doWork())
        }

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
                defaultNetworkHandle = 7L,
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
