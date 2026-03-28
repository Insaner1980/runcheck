package com.runcheck.data.appusage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.runcheck.domain.usecase.TrackThrottlingEventsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageDataSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : TrackThrottlingEventsUseCase.ForegroundAppProvider {
        private val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

        suspend fun getUsageSince(
            startTimeMs: Long,
            endTimeMs: Long,
        ): List<AppUsageSnapshot> =
            withContext(Dispatchers.IO) {
                if (endTimeMs <= startTimeMs || !hasUsageStatsPermission()) {
                    return@withContext emptyList()
                }

                val stats =
                    usageStatsManager
                        ?.queryUsageStats(
                            UsageStatsManager.INTERVAL_DAILY,
                            startTimeMs,
                            endTimeMs,
                        ).orEmpty()

                val packageManager = context.packageManager
                stats
                    .asSequence()
                    .filter { stat ->
                        stat.packageName.isNotBlank() && stat.totalTimeInForeground > 0L
                    }.map { stat ->
                        AppUsageSnapshot(
                            packageName = stat.packageName,
                            appLabel = resolveAppLabel(packageManager, stat.packageName),
                            foregroundTimeMs = stat.totalTimeInForeground,
                        )
                    }.sortedByDescending { it.foregroundTimeMs }
                    .toList()
            }

        override suspend fun getCurrentForegroundApp(): String? = getCurrentForegroundApp(RECENT_USAGE_LOOKBACK_MS)

        suspend fun getCurrentForegroundApp(lookbackWindowMs: Long): String? =
            withContext(Dispatchers.IO) {
                if (!hasUsageStatsPermission()) {
                    return@withContext null
                }

                val endTimeMs = System.currentTimeMillis()
                val startTimeMs = endTimeMs - lookbackWindowMs
                val packageManager = context.packageManager

                usageStatsManager
                    ?.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        startTimeMs,
                        endTimeMs,
                    ).orEmpty()
                    .asSequence()
                    .filter { stat -> stat.packageName.isNotBlank() && stat.lastTimeUsed > 0L }
                    .maxByOrNull { stat -> stat.lastTimeUsed }
                    ?.packageName
                    ?.let { packageName -> resolveAppLabel(packageManager, packageName) }
            }

        fun hasUsageStatsPermission(): Boolean {
            val appOps =
                context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                    ?: return false
            val mode =
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            return mode == AppOpsManager.MODE_ALLOWED
        }

        private fun resolveAppLabel(
            packageManager: PackageManager,
            packageName: String,
        ): String =
            try {
                val applicationInfo =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getApplicationInfo(
                            packageName,
                            PackageManager.ApplicationInfoFlags.of(
                                PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong(),
                            ),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getApplicationInfo(
                            packageName,
                            PackageManager.MATCH_UNINSTALLED_PACKAGES,
                        )
                    }
                packageManager.getApplicationLabel(applicationInfo).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                packageName
            } catch (_: RuntimeException) {
                packageName
            }

        data class AppUsageSnapshot(
            val packageName: String,
            val appLabel: String,
            val foregroundTimeMs: Long,
        )

        companion object {
            private const val RECENT_USAGE_LOOKBACK_MS = 2 * 60 * 1000L
        }
    }
