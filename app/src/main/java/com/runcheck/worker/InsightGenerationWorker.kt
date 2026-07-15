package com.runcheck.worker

import android.content.Context
import android.database.SQLException
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.insights.engine.InsightEngine
import com.runcheck.util.ReleaseSafeLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class InsightGenerationWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val insightEngine: InsightEngine,
    ) : CoroutineWorker(appContext, workerParams) {
        // WorkManager needs an explicit terminal result for unexpected non-cancellation failures.
        @Suppress("TooGenericExceptionCaught")
        override suspend fun doWork(): Result =
            try {
                insightEngine.generateInsights()
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SQLException) {
                ReleaseSafeLog.error(TAG, "Insight generation failed", e)
                Result.retry()
            } catch (e: Exception) {
                ReleaseSafeLog.error(TAG, "Insight generation failed", e)
                Result.failure()
            }

        companion object {
            const val WORK_NAME = "insight_generation"
            private const val TAG = "InsightGeneration"
        }
    }
