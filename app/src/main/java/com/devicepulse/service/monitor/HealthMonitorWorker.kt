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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
        return try {
            // Collect one reading from each source
            val batteryState = batteryRepository.getBatteryState().first()
            batteryRepository.saveReading(batteryState)

            val networkState = networkRepository.getNetworkState().first()
            networkRepository.saveReading(networkState)

            val thermalState = thermalRepository.getThermalState().first()
            thermalRepository.saveReading(thermalState)

            val storageState = storageRepository.getStorageState().first()
            storageRepository.saveReading(storageState)

            appBatteryUsageRepository.collectUsageSnapshot()

            // Clean up old readings based on retention policy
            cleanupOldReadings()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "health_monitor"
    }
}
