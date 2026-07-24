package com.runcheck.service.report

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.runcheck.domain.model.WeeklyReportPeriod
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.domain.usecase.GenerateWeeklyReportUseCase
import com.runcheck.domain.usecase.WeeklyReportGenerationResult
import com.runcheck.util.ReleaseSafeLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.ZoneId

@HiltWorker
class WeeklyReportWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val preferencesRepository: UserPreferencesRepository,
        private val generateWeeklyReport: GenerateWeeklyReportUseCase,
        private val proStatusProvider: ProStatusProvider,
        private val notifier: WeeklyReportNotifier,
        private val scheduler: WeeklyReportScheduler,
        private val notificationGate: WeeklyReportNotificationGate,
        private val timeProvider: WeeklyReportTimeProvider,
    ) : CoroutineWorker(context, params) {
        internal constructor(
            context: Context,
            params: WorkerParameters,
            preferencesRepository: UserPreferencesRepository,
            generateWeeklyReport: GenerateWeeklyReportUseCase,
            proStatusProvider: ProStatusProvider,
            notifier: WeeklyReportNotifier,
            scheduler: WeeklyReportScheduler,
            notificationGate: WeeklyReportNotificationGate,
            clock: Clock,
            zoneProvider: () -> ZoneId,
        ) : this(
            context,
            params,
            preferencesRepository,
            generateWeeklyReport,
            proStatusProvider,
            notifier,
            scheduler,
            notificationGate,
            WeeklyReportTimeProvider().apply {
                overrideClock = clock
                overrideZoneProvider = zoneProvider
            },
        )

        @Suppress("TooGenericExceptionCaught")
        override suspend fun doWork(): Result =
            try {
                val period =
                    WeeklyReportPeriod.previousCompleted(
                        timeProvider.clock.instant(),
                        timeProvider.zoneId(),
                    )
                val preferences = preferencesRepository.getPreferences().first()
                val processed = preferencesRepository.getWeeklyReportLastProcessedPeriod()
                val notificationAllowed = notificationGate.canNotify()
                when (
                    decideWeeklyReportDelivery(
                        period = period,
                        lastProcessedStart = processed?.first,
                        lastProcessedEnd = processed?.second,
                        weeklyEnabled = preferences.weeklyReportEnabled,
                        hasPro = proStatusProvider.isPro(),
                        notificationsEnabled = preferences.notificationsEnabled,
                        canPostNotifications = notificationAllowed,
                        reportsChannelEnabled = notificationAllowed,
                    )
                ) {
                    WeeklyReportDeliveryDecision.HANDLE_WITHOUT_NOTIFICATION -> markProcessed(period)

                    WeeklyReportDeliveryDecision.DELIVER -> {
                        when (val generated = generateWeeklyReport(period)) {
                            is WeeklyReportGenerationResult.Available -> {
                                notifier.show(generated.report)
                                markProcessed(period)
                            }

                            WeeklyReportGenerationResult.Locked -> Unit
                        }
                    }

                    WeeklyReportDeliveryDecision.DISABLED,
                    WeeklyReportDeliveryDecision.PRO_INACTIVE,
                    WeeklyReportDeliveryDecision.ALREADY_PROCESSED,
                    -> Unit
                }
                scheduler.scheduleNextAfterCurrent()
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReleaseSafeLog.error(TAG, "Weekly report generation failed", e)
                Result.retry()
            }

        private suspend fun markProcessed(period: WeeklyReportPeriod) {
            preferencesRepository.setWeeklyReportLastProcessedPeriod(
                period.startInclusive.toEpochMilli(),
                period.endExclusive.toEpochMilli(),
            )
        }

        companion object {
            const val WORK_NAME = "weekly_report"
            private const val TAG = "WeeklyReportWorker"
        }
    }
