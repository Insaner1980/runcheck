package com.runcheck.worker

import android.content.Context
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
        override suspend fun doWork(): Result =
            try {
                insightEngine.generateInsights()
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReleaseSafeLog.error(TAG, "Insight generation failed", e)
                Result.retry()
            }

        companion object {
            const val WORK_NAME = "insight_generation"
            private const val TAG = "InsightGeneration"
        }
    }
