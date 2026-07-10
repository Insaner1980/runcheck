package com.runcheck.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runcheck.billing.ProPurchaseRefreshResult
import com.runcheck.billing.ProPurchaseStatusRefresher
import com.runcheck.service.monitor.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TrialNotificationWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val proPurchaseStatusRefresher: ProPurchaseStatusRefresher,
        private val notificationHelper: NotificationHelper,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            when (proPurchaseStatusRefresher.refreshPurchaseStatusAfterInitialization()) {
                ProPurchaseRefreshResult.ACTIVE -> return Result.success()
                ProPurchaseRefreshResult.UNAVAILABLE -> return Result.retry()
                ProPurchaseRefreshResult.NOT_ACTIVE -> Unit
            }

            val notificationType = inputData.getString(KEY_NOTIFICATION_TYPE) ?: return Result.failure()

            when (notificationType) {
                TYPE_DAY5 -> notificationHelper.showTrialDay5Notification()
                TYPE_DAY7 -> notificationHelper.showTrialDay7Notification()
            }

            return Result.success()
        }

        companion object {
            const val KEY_NOTIFICATION_TYPE = "notification_type"
            const val TYPE_DAY5 = "trial_day5"
            const val TYPE_DAY7 = "trial_day7"
            const val UNIQUE_WORK_DAY5 = "trial_notification_day5"
            const val UNIQUE_WORK_DAY7 = "trial_notification_day7"
            const val TAG_DAY5 = "trial_notification_day5"
            const val TAG_DAY7 = "trial_notification_day7"
        }
    }
