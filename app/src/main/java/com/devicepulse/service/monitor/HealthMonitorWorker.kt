package com.devicepulse.service.monitor

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devicepulse.domain.repository.AppBatteryUsageRepository
import com.devicepulse.domain.repository.BatteryRepository
import com.devicepulse.domain.repository.NetworkRepository
import com.devicepulse.domain.repository.StorageRepository
import com.devicepulse.domain.repository.ThermalRepository
import com.devicepulse.domain.usecase.CleanupOldReadingsUseCase
import com.devicepulse.util.ReleaseSafeLog
import com.devicepulse.widget.DevicePulseWidgets
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class HealthMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appBatteryUsageRepository: AppBatteryUsageRepository,
    private val batteryRepository: BatteryRepository,
    private val networkRepository: NetworkRepository,
    private val thermalRepository: ThermalRepository,
    private val storageRepository: StorageRepository,
    private val cleanupOldReadings: CleanupOldReadingsUseCase
    ) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        var hadFailure = false

        hadFailure = collectStep("battery") {
            val batteryState = batteryRepository.getBatteryState().first()
            batteryRepository.saveReading(batteryState)
        }

        hadFailure = collectStep("network") {
            val networkState = networkRepository.getNetworkState().first()
            networkRepository.saveReading(networkState)
        } || hadFailure

        hadFailure = collectStep("thermal") {
            val thermalState = thermalRepository.getThermalState().first()
            thermalRepository.saveReading(thermalState)
        } || hadFailure

        hadFailure = collectStep("storage") {
            val storageState = storageRepository.getStorageState().first()
            storageRepository.saveReading(storageState)
        } || hadFailure

        hadFailure = collectStep("app_usage") {
            appBatteryUsageRepository.collectUsageSnapshot()
        } || hadFailure

        hadFailure = collectStep("cleanup") {
            cleanupOldReadings()
        } || hadFailure

        hadFailure = collectStep("widgets") {
            DevicePulseWidgets.updateAll(applicationContext)
        } || hadFailure

        return if (hadFailure) Result.retry() else Result.success()
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

    companion object {
        const val WORK_NAME = "health_monitor"
        private const val TAG = "HealthMonitorWorker"
    }
}
