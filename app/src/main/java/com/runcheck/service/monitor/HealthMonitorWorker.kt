package com.runcheck.service.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.EvaluateMonitoringAlertsUseCase
import com.runcheck.domain.usecase.MonitoringAlertSnapshot
import com.runcheck.util.ReleaseSafeLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class HealthMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val batteryRepository: BatteryRepository,
    private val networkRepository: NetworkRepository,
    private val thermalRepository: ThermalRepository,
    private val storageRepository: StorageRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val chargerSessionTracker: ChargerSessionTracker,
    private val evaluateMonitoringAlerts: EvaluateMonitoringAlertsUseCase,
    private val monitoringAlertStateStore: MonitoringAlertStateStore,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        var coreFailure = false
        var batteryState: BatteryState? = null
        var thermalState: ThermalState? = null
        var storageState: StorageState? = null

        val preferences = try {
            userPreferencesRepository.getPreferences().first()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to load notification preferences", e)
            return Result.retry()
        }

        coreFailure = collectStep("battery") {
            batteryState = batteryRepository.getBatteryState().first()
            batteryRepository.saveReading(requireNotNull(batteryState))
            chargerSessionTracker.onBatteryState(requireNotNull(batteryState))
        }

        coreFailure = collectStep("network") {
            val networkState = networkRepository.getNetworkState().first()
            networkRepository.saveReading(networkState)
        } || coreFailure

        coreFailure = collectStep("thermal") {
            thermalState = thermalRepository.getThermalState().first()
            thermalRepository.saveReading(requireNotNull(thermalState))
        } || coreFailure

        coreFailure = collectStep("storage") {
            storageState = storageRepository.getStorageState().first()
            storageRepository.saveReading(requireNotNull(storageState))
        } || coreFailure

        coreFailure = collectStep("alerts") {
            val snapshot = buildSnapshot(
                batteryState = requireNotNull(batteryState),
                thermalState = requireNotNull(thermalState),
                storageState = requireNotNull(storageState)
            )
            val previousSnapshot = monitoringAlertStateStore.getLastSnapshot()
            val chargeCompleteFired = monitoringAlertStateStore.wasChargeCompleteFired()
            val alertDecision = evaluateMonitoringAlerts(
                previousSnapshot, snapshot, preferences, chargeCompleteFired
            )

            // Reset charge-complete flag when phone is unplugged
            val isUnplugged = snapshot.chargingStatus == ChargingStatus.DISCHARGING ||
                snapshot.chargingStatus == ChargingStatus.NOT_CHARGING
            val newChargeCompleteFired = when {
                isUnplugged -> false
                alertDecision.chargeComplete -> true
                else -> chargeCompleteFired
            }

            // Persist state before posting notifications so a retry
            // after a crash here won't re-evaluate the same transition.
            monitoringAlertStateStore.update(snapshot, newChargeCompleteFired)

            if (alertDecision.lowBattery) {
                notificationHelper.showLowBatteryAlert(snapshot.batteryLevel)
            }
            if (alertDecision.highTemp) {
                notificationHelper.showHighTempAlert(
                    snapshot.batteryTempC,
                    preferences.temperatureUnit
                )
            }
            if (alertDecision.lowStorage) {
                notificationHelper.showLowStorageAlert(snapshot.storageUsagePercent)
            }
            if (alertDecision.chargeComplete) {
                notificationHelper.showChargeCompleteNotification(snapshot.batteryLevel)
            }
        } || coreFailure

        restartLiveNotificationIfNeeded(preferences.liveNotificationEnabled)

        return if (coreFailure) Result.retry() else Result.success()
    }

    /**
     * Safety net: if the user has live notification enabled but the
     * foreground service died (e.g. process death with START_NOT_STICKY),
     * restart it on the next periodic worker run.
     */
    private fun restartLiveNotificationIfNeeded(liveNotificationEnabled: Boolean) {
        if (!liveNotificationEnabled) return
        try {
            val am = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val running = am.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == RealTimeMonitorService::class.java.name }
            if (!running) {
                val serviceIntent = Intent(applicationContext, RealTimeMonitorService::class.java)
                applicationContext.startForegroundService(serviceIntent)
            }
        } catch (t: Throwable) {
            ReleaseSafeLog.error(TAG, "Failed to restart live notification service", t)
        }
    }

    private suspend fun collectStep(stepName: String, block: suspend () -> Unit): Boolean {
        return try {
            block()
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ReleaseSafeLog.error(TAG, "Health monitor step failed: $stepName", e)
            true
        }
    }

    private fun buildSnapshot(
        batteryState: BatteryState,
        thermalState: ThermalState,
        storageState: StorageState
    ): MonitoringAlertSnapshot {
        return MonitoringAlertSnapshot(
            batteryLevel = batteryState.level,
            batteryTempC = thermalState.batteryTempC,
            storageUsagePercent = storageState.usagePercent,
            chargingStatus = batteryState.chargingStatus
        )
    }

    companion object {
        const val WORK_NAME = "health_monitor"
        private const val TAG = "HealthMonitorWorker"
    }
}
