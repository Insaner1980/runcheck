package com.runcheck.service.monitor

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.usecase.CleanupOldReadingsUseCase
import com.runcheck.util.ReleaseSafeLog
import com.runcheck.widget.RuncheckWidgets
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
            RuncheckWidgets.updateAll(applicationContext)
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
