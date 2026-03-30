package com.runcheck.service.monitor

import android.content.Context
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    @Test
    fun `doWork records heartbeat only after successful collection`() =
        runTest {
            val worker = createWorker()

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 1) { monitoringStatusRepository.setLastWorkerHeartbeatAt(any()) }
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
                    batteryStateFlow =
                        flow {
                            error("battery failed")
                        },
                )

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            coVerify(exactly = 0) { monitoringStatusRepository.setLastWorkerHeartbeatAt(any()) }
            coVerify(exactly = 0) { batteryRepository.saveReading(any()) }
            coVerify(exactly = 0) { monitoringAlertStateStore.update(any(), any()) }
        }

    @Test
    fun `doWork retries before collection when preferences fail to load`() =
        runTest {
            val worker =
                createWorker(
                    preferencesFlow =
                        flow {
                            error("preferences failed")
                        },
                )

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            coVerify(exactly = 0) { monitoringStatusRepository.setLastWorkerHeartbeatAt(any()) }
            coVerify(exactly = 0) { batteryRepository.saveReading(any()) }
            coVerify(exactly = 0) { networkRepository.saveReading(any()) }
            coVerify(exactly = 0) { thermalRepository.saveReading(any()) }
            coVerify(exactly = 0) { storageRepository.saveReading(any()) }
        }

    private fun createWorker(
        preferencesFlow: Flow<UserPreferences> = flowOf(UserPreferences()),
        batteryStateFlow: Flow<BatteryState> = flowOf(sampleBatteryState),
        networkStateFlow: Flow<NetworkState> = flowOf(sampleNetworkState),
        thermalStateFlow: Flow<ThermalState> = flowOf(sampleThermalState),
        storageStateFlow: Flow<StorageState> = flowOf(sampleStorageState),
    ): HealthMonitorWorker {
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
