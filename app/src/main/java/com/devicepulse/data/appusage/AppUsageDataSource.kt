package com.devicepulse.data.appusage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    suspend fun getUsageSince(startTimeMs: Long, endTimeMs: Long): List<AppUsageSnapshot> =
        withContext(Dispatchers.IO) {
            if (endTimeMs <= startTimeMs || !hasUsageStatsPermission()) {
                return@withContext emptyList()
            }

            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTimeMs,
                endTimeMs
            ).orEmpty()

            val packageManager = context.packageManager
            stats.asSequence()
                .filter { stat ->
                    stat.packageName.isNotBlank() && stat.totalTimeInForeground > 0L
                }
                .map { stat ->
                    AppUsageSnapshot(
                        packageName = stat.packageName,
                        appLabel = resolveAppLabel(packageManager, stat.packageName),
                        foregroundTimeMs = stat.totalTimeInForeground
                    )
                }
                .sortedByDescending { it.foregroundTimeMs }
                .toList()
        }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun resolveAppLabel(packageManager: PackageManager, packageName: String): String {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        } catch (_: RuntimeException) {
            packageName
        }
    }

    data class AppUsageSnapshot(
        val packageName: String,
        val appLabel: String,
        val foregroundTimeMs: Long
    )
}
