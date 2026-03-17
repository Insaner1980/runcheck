package com.runcheck.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runcheck.pro.ProManager
import com.runcheck.service.monitor.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TrialNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val proManager: ProManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (proManager.isPro()) return Result.success()

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
        const val WORK_TAG_DAY5 = "trial_notification_day5"
        const val WORK_TAG_DAY7 = "trial_notification_day7"
    }
}
