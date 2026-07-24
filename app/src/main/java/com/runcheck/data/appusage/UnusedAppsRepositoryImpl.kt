package com.runcheck.data.appusage

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.runcheck.domain.model.UnusedAppCandidate
import com.runcheck.domain.model.UnusedAppError
import com.runcheck.domain.model.UnusedAppsPeriod
import com.runcheck.domain.model.UnusedAppsResult
import com.runcheck.domain.model.UsageAccess
import com.runcheck.domain.repository.UnusedAppsRepository
import com.runcheck.util.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnusedAppsRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val dispatchers: AppDispatchers,
    ) : UnusedAppsRepository {
        private val cacheMutex = Mutex()
        private val cache = mutableMapOf<UnusedAppsPeriod, UnusedAppsResult>()

        override suspend fun getUnusedApps(
            period: UnusedAppsPeriod,
            observedAt: Instant,
            forceRefresh: Boolean,
        ): UnusedAppsResult =
            cacheMutex.withLock {
                if (!forceRefresh) {
                    cache[period]?.let { return@withLock it }
                }
                val result = load(period, observedAt)
                cache[period] = result
                result
            }

        private suspend fun load(
            period: UnusedAppsPeriod,
            observedAt: Instant,
        ): UnusedAppsResult =
            withContext(dispatchers.io) {
                if (!hasUsageAccess()) {
                    return@withContext UnusedAppsResult(
                        usageAccess = UsageAccess.REQUIRED,
                        period = period,
                        observedAt = observedAt,
                    )
                }
                val apps = queryLauncherApps()
                val start = observedAt.minusSeconds(period.days.toLong() * SECONDS_PER_DAY)
                val lastUsedAt = queryLastUsed(start, observedAt)
                val candidates =
                    filterUnusedApps(
                        apps = apps,
                        lastUsedAt = lastUsedAt,
                        selfPackageName = context.packageName,
                        period = period,
                        observedAt = observedAt,
                    )
                val storageSemaphore = Semaphore(STORAGE_QUERY_PARALLELISM)
                val withStorage =
                    coroutineScope {
                        candidates
                            .map { candidate ->
                                async(dispatchers.io) {
                                    storageSemaphore.withPermit {
                                        candidate.withStorage(queryStorage(candidate.packageName))
                                    }
                                }
                            }.awaitAll()
                    }.sortedWith(
                        compareByDescending<UnusedAppCandidate> { it.storageBytes ?: -1L }
                            .thenBy { it.appLabel ?: it.packageName },
                    )
                UnusedAppsResult(
                    usageAccess = UsageAccess.GRANTED,
                    period = period,
                    observedAt = observedAt,
                    candidates = withStorage,
                    partialErrors = withStorage.flatMapTo(mutableSetOf()) { it.errors },
                )
            }

        private fun hasUsageAccess(): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            return appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            ) == AppOpsManager.MODE_ALLOWED
        }

        private fun queryLauncherApps(): List<InstalledLauncherApp> {
            val packageManager = context.packageManager
            val launcherIntent =
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
            val resolved =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.queryIntentActivities(
                        launcherIntent,
                        PackageManager.ResolveInfoFlags.of(0L),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(launcherIntent, 0)
                }
            return resolved
                .asSequence()
                .mapNotNull { it.activityInfo?.applicationInfo }
                .distinctBy { it.packageName }
                .mapNotNull { applicationInfo ->
                    val packageInfo =
                        runCatching {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                packageManager.getPackageInfo(
                                    applicationInfo.packageName,
                                    PackageManager.PackageInfoFlags.of(0L),
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                packageManager.getPackageInfo(applicationInfo.packageName, 0)
                            }
                        }.getOrNull() ?: return@mapNotNull null
                    val label =
                        runCatching {
                            packageManager.getApplicationLabel(applicationInfo).toString().takeIf(String::isNotBlank)
                        }.getOrNull()
                    InstalledLauncherApp(
                        packageName = applicationInfo.packageName,
                        appLabel = label,
                        firstInstallTime = Instant.ofEpochMilli(packageInfo.firstInstallTime),
                        isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        isUpdatedSystemApp =
                            applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
                    )
                }.toList()
        }

        private fun queryLastUsed(
            startInclusive: Instant,
            endExclusive: Instant,
        ): Map<String, Instant> {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()
            return manager
                .queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    startInclusive.toEpochMilli(),
                    endExclusive.toEpochMilli(),
                ).orEmpty()
                .asSequence()
                .filter { it.packageName.isNotBlank() && it.lastTimeUsed > 0L }
                .groupBy { it.packageName }
                .mapValues { (_, rows) -> Instant.ofEpochMilli(rows.maxOf { it.lastTimeUsed }) }
        }

        private fun queryStorage(packageName: String): StorageQueryResult {
            val packageManager = context.packageManager
            val storageManager =
                context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                    ?: return StorageQueryResult(null, UnusedAppError.STORAGE_IO)
            return try {
                val applicationInfo =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getApplicationInfo(
                            packageName,
                            PackageManager.ApplicationInfoFlags.of(0L),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getApplicationInfo(packageName, 0)
                    }
                val stats =
                    storageManager.queryStatsForPackage(
                        applicationInfo.storageUuid,
                        packageName,
                        Process.myUserHandle(),
                    )
                StorageQueryResult(stats.appBytes + stats.dataBytes, null)
            } catch (error: Exception) {
                StorageQueryResult(null, storageErrorFor(error))
            }
        }
    }

internal fun storageErrorFor(error: Exception): UnusedAppError =
    if (error is SecurityException) {
        UnusedAppError.STORAGE_PERMISSION
    } else {
        UnusedAppError.STORAGE_IO
    }

internal data class InstalledLauncherApp(
    val packageName: String,
    val appLabel: String?,
    val firstInstallTime: Instant,
    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean,
)

internal fun filterUnusedApps(
    apps: List<InstalledLauncherApp>,
    lastUsedAt: Map<String, Instant>,
    selfPackageName: String,
    period: UnusedAppsPeriod,
    observedAt: Instant,
): List<UnusedAppCandidate> {
    val cutoff = observedAt.minusSeconds(period.days.toLong() * SECONDS_PER_DAY)
    return apps
        .asSequence()
        .filterNot { it.isSystemApp || it.isUpdatedSystemApp }
        .filterNot { it.packageName == selfPackageName }
        .filter { it.firstInstallTime <= cutoff }
        .filter { lastUsedAt[it.packageName]?.isBefore(cutoff) != false }
        .map { app ->
            UnusedAppCandidate(
                packageName = app.packageName,
                appLabel = app.appLabel,
                firstInstallTime = app.firstInstallTime,
                lastRecordedUse = lastUsedAt[app.packageName],
                storageBytes = null,
                errors =
                    if (app.appLabel == null) {
                        setOf(UnusedAppError.PACKAGE_LABEL)
                    } else {
                        emptySet()
                    },
            )
        }.sortedBy { it.appLabel ?: it.packageName }
        .toList()
}

private fun UnusedAppCandidate.withStorage(result: StorageQueryResult): UnusedAppCandidate =
    copy(
        storageBytes = result.bytes,
        errors = result.error?.let { errors + it } ?: errors,
    )

private data class StorageQueryResult(
    val bytes: Long?,
    val error: UnusedAppError?,
)

private const val STORAGE_QUERY_PARALLELISM = 4
private const val SECONDS_PER_DAY = 86_400L
