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
        collectStep("battery") {
            val batteryState = batteryRepository.getBatteryState().first()
            batteryRepository.saveReading(batteryState)
        }

        collectStep("network") {
            val networkState = networkRepository.getNetworkState().first()
            networkRepository.saveReading(networkState)
        }

        collectStep("thermal") {
            val thermalState = thermalRepository.getThermalState().first()
            thermalRepository.saveReading(thermalState)
        }

        collectStep("storage") {
            val storageState = storageRepository.getStorageState().first()
            storageRepository.saveReading(storageState)
        }

        collectStep("app_usage") {
            appBatteryUsageRepository.collectUsageSnapshot()
        }

        collectStep("cleanup") {
            cleanupOldReadings()
        }

        return Result.success()
    }

    private suspend fun collectStep(stepName: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ReleaseSafeLog.error(TAG, "Health monitor step failed: $stepName", e)
        }
    }

    companion object {
        const val WORK_NAME = "health_monitor"
        private const val TAG = "HealthMonitorWorker"
    }
}
