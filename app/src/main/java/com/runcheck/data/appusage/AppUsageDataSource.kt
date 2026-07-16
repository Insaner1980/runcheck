package com.runcheck.data.appusage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.runcheck.domain.usecase.TrackThrottlingEventsUseCase
import com.runcheck.util.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageDataSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val dispatchers: AppDispatchers,
    ) : TrackThrottlingEventsUseCase.ForegroundAppProvider {
        private val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

        suspend fun getUsageSince(
            startTimeMs: Long,
            endTimeMs: Long,
        ): List<AppUsageSnapshot>? =
            withContext(dispatchers.io) {
                if (endTimeMs <= startTimeMs || !hasUsageStatsPermission()) {
                    return@withContext emptyList()
                }

                val manager = usageStatsManager ?: return@withContext null
                val eventQueryStart = (startTimeMs - EVENT_STATE_LOOKBACK_MS).coerceAtLeast(0L)
                val usageEvents = manager.queryEvents(eventQueryStart, endTimeMs) ?: return@withContext null
                val foregroundUsage =
                    aggregateForegroundUsage(
                        events = usageEvents.toActivityEvents(),
                        startTimeMs = startTimeMs,
                        endTimeMs = endTimeMs,
                    )

                val packageManager = context.packageManager
                foregroundUsage
                    .asSequence()
                    .map { (packageName, foregroundTimeMs) ->
                        AppUsageSnapshot(
                            packageName = packageName,
                            appLabel = resolveAppLabel(packageManager, packageName),
                            foregroundTimeMs = foregroundTimeMs,
                        )
                    }.sortedByDescending { it.foregroundTimeMs }
                    .toList()
            }

        override suspend fun getCurrentForegroundApp(): String? = getCurrentForegroundApp(RECENT_USAGE_LOOKBACK_MS)

        suspend fun getCurrentForegroundApp(lookbackWindowMs: Long): String? =
            withContext(dispatchers.io) {
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
            private const val EVENT_STATE_LOOKBACK_MS = 24L * 60L * 60L * 1000L
            private const val RECENT_USAGE_LOOKBACK_MS = 2 * 60 * 1000L
        }
    }

internal data class UsageActivityEvent(
    val timestamp: Long,
    val packageName: String?,
    val className: String?,
    val type: UsageActivityEventType,
)

internal enum class UsageActivityEventType {
    RESUMED,
    PAUSED,
    STOPPED,
    DEVICE_SHUTDOWN,
    DEVICE_STARTUP,
}

internal fun aggregateForegroundUsage(
    events: List<UsageActivityEvent>,
    startTimeMs: Long,
    endTimeMs: Long,
): Map<String, Long> {
    if (endTimeMs <= startTimeMs) return emptyMap()

    val packageStates = mutableMapOf<String, PackageForegroundState>()
    val foregroundUsage = mutableMapOf<String, Long>()
    var lastEventAt = events.minOfOrNull(UsageActivityEvent::timestamp) ?: startTimeMs
    var resumeOrder = 0L
    events.sortedBy(UsageActivityEvent::timestamp).forEach { event ->
        if (event.timestamp >= endTimeMs) return@forEach
        accrueForegroundInterval(
            packageStates = packageStates,
            foregroundUsage = foregroundUsage,
            intervalStartMs = lastEventAt,
            intervalEndMs = event.timestamp,
            windowStartMs = startTimeMs,
            windowEndMs = endTimeMs,
        )
        lastEventAt = event.timestamp
        if (event.type == UsageActivityEventType.DEVICE_SHUTDOWN ||
            event.type == UsageActivityEventType.DEVICE_STARTUP
        ) {
            packageStates.clear()
            return@forEach
        }

        // Keep every named usage-event package, including removed, system, and launcher packages.
        val packageName = event.packageName?.takeIf(String::isNotBlank) ?: return@forEach
        val state = packageStates.getOrPut(packageName) { PackageForegroundState() }
        val activityKey = event.className ?: packageName
        when (event.type) {
            UsageActivityEventType.RESUMED -> {
                state.activeActivities += activityKey
                state.lastResumeOrder = ++resumeOrder
            }

            UsageActivityEventType.PAUSED,
            UsageActivityEventType.STOPPED,
            -> {
                state.activeActivities -= activityKey
                if (state.activeActivities.isEmpty()) {
                    packageStates -= packageName
                }
            }

            UsageActivityEventType.DEVICE_SHUTDOWN,
            UsageActivityEventType.DEVICE_STARTUP,
            -> {
                Unit
            }
        }
    }

    accrueForegroundInterval(
        packageStates = packageStates,
        foregroundUsage = foregroundUsage,
        intervalStartMs = lastEventAt,
        intervalEndMs = endTimeMs,
        windowStartMs = startTimeMs,
        windowEndMs = endTimeMs,
    )
    return foregroundUsage
}

private data class PackageForegroundState(
    val activeActivities: MutableSet<String> = mutableSetOf(),
    var lastResumeOrder: Long = 0L,
)

private fun accrueForegroundInterval(
    packageStates: Map<String, PackageForegroundState>,
    foregroundUsage: MutableMap<String, Long>,
    intervalStartMs: Long,
    intervalEndMs: Long,
    windowStartMs: Long,
    windowEndMs: Long,
) {
    val foregroundPackage = packageStates.maxByOrNull { it.value.lastResumeOrder }?.key ?: return
    val clippedStart = intervalStartMs.coerceAtLeast(windowStartMs)
    val clippedEnd = intervalEndMs.coerceAtMost(windowEndMs)
    val durationMs = (clippedEnd - clippedStart).coerceAtLeast(0L)
    if (durationMs > 0L) {
        foregroundUsage[foregroundPackage] = foregroundUsage.getOrDefault(foregroundPackage, 0L) + durationMs
    }
}

private fun UsageEvents.toActivityEvents(): List<UsageActivityEvent> =
    buildList {
        val event = UsageEvents.Event()
        while (hasNextEvent() && getNextEvent(event)) {
            val type =
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> UsageActivityEventType.RESUMED
                    UsageEvents.Event.ACTIVITY_PAUSED -> UsageActivityEventType.PAUSED
                    UsageEvents.Event.ACTIVITY_STOPPED -> UsageActivityEventType.STOPPED
                    UsageEvents.Event.DEVICE_SHUTDOWN -> UsageActivityEventType.DEVICE_SHUTDOWN
                    UsageEvents.Event.DEVICE_STARTUP -> UsageActivityEventType.DEVICE_STARTUP
                    else -> null
                }
            if (type != null) {
                add(
                    UsageActivityEvent(
                        timestamp = event.timeStamp,
                        packageName = event.packageName,
                        className = event.className,
                        type = type,
                    ),
                )
            }
        }
    }
