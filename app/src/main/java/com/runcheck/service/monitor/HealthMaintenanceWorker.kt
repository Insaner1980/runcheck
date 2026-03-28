package com.runcheck.service.monitor

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.usecase.CleanupOldReadingsUseCase
import com.runcheck.util.ReleaseSafeLog
import com.runcheck.widget.RuncheckWidgets
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class HealthMaintenanceWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val appBatteryUsageRepository: AppBatteryUsageRepository,
        private val cleanupOldReadings: CleanupOldReadingsUseCase,
    ) : CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result {
            var maintenanceFailure =
                collectStep("app_usage") {
                    appBatteryUsageRepository.collectUsageSnapshot()
                }

            maintenanceFailure = collectStep("cleanup") { cleanupOldReadings() } || maintenanceFailure

            // Widget refresh is best-effort and should not force periodic retries.
            collectStep("widgets") {
                RuncheckWidgets.updateAll(applicationContext)
            }

            return if (maintenanceFailure) Result.retry() else Result.success()
        }

        private suspend fun collectStep(
            stepName: String,
            block: suspend () -> Unit,
        ): Boolean =
            try {
                block()
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReleaseSafeLog.error(TAG, "Health maintenance step failed: $stepName", e)
                true
            }

        companion object {
            const val WORK_NAME = "health_maintenance"
            private const val TAG = "HealthMaintenance"
        }
    }
