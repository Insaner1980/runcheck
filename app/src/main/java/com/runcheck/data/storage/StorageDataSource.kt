package com.runcheck.data.storage

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.runcheck.domain.model.MediaBreakdown
import com.runcheck.domain.model.TrashInfo
import com.runcheck.util.AppDispatchers
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val mediaStoreScanner: MediaStoreScanner,
        private val dispatchers: AppDispatchers,
    ) {
        private val storageStatsManager =
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        private val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

        suspend fun getStorageInfo(): StorageInfo {
            val mediaBreakdown =
                try {
                    mediaStoreScanner.getMediaBreakdown()
                } catch (_: Exception) {
                    null
                }
            val trashInfo =
                try {
                    mediaStoreScanner.getTrashInfo()
                } catch (_: Exception) {
                    null
                }

            return withContext(dispatchers.io) {
                val ssm = storageStatsManager
                val uuid = StorageManager.UUID_DEFAULT
                val totalBytes =
                    try {
                        ssm?.getTotalBytes(uuid) ?: StatFs(Environment.getDataDirectory().path).totalBytes
                    } catch (_: Exception) {
                        StatFs(Environment.getDataDirectory().path).totalBytes
                    }
                val freeBytes =
                    try {
                        ssm?.getFreeBytes(uuid) ?: StatFs(Environment.getDataDirectory().path).availableBytes
                    } catch (_: Exception) {
                        StatFs(Environment.getDataDirectory().path).availableBytes
                    }
                val usedBytes = totalBytes - freeBytes

                val hasUsageStats = hasUsageStatsPermission()
                val appStats = if (hasUsageStats) calculateAppStats() else null
                val sdCard = getExternalSdCard()
                val deviceInfo = getDeviceStorageInfo()

                StorageInfo(
                    totalBytes = totalBytes,
                    availableBytes = freeBytes,
                    usedBytes = usedBytes,
                    appsBytes = appStats?.totalBytes,
                    totalCacheBytes = appStats?.cacheBytes,
                    appCount = appStats?.appCount,
                    mediaBreakdown = mediaBreakdown,
                    trashInfo = trashInfo,
                    sdCardAvailable = sdCard != null,
                    sdCardTotalBytes = sdCard?.totalBytes,
                    sdCardAvailableBytes = sdCard?.availableBytes,
                    fileSystemType = deviceInfo.fileSystemType,
                    encryptionStatus = deviceInfo.encryptionStatus,
                    storageVolumes = deviceInfo.storageVolumes,
                )
            }
        }

        fun hasUsageStatsPermission(): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode =
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            return mode == AppOpsManager.MODE_ALLOWED
        }

        private fun calculateAppStats(): AppStats? {
            val ssm = storageStatsManager ?: return null
            return try {
                val uuid = StorageManager.UUID_DEFAULT
                val user = android.os.Process.myUserHandle()
                val stats = ssm.queryStatsForUser(uuid, user)
                AppStats(
                    totalBytes = stats.appBytes + stats.dataBytes + stats.cacheBytes,
                    cacheBytes = stats.cacheBytes,
                    appCount = countLaunchableApps(),
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun countLaunchableApps(): Int? =
            try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val packageNames =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.queryIntentActivities(
                            intent,
                            PackageManager.ResolveInfoFlags.of(0L),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.queryIntentActivities(intent, 0)
                    }.mapNotNull { resolveInfo -> resolveInfo.activityInfo?.packageName }
                countDistinctPackageNames(packageNames)
            } catch (_: RuntimeException) {
                null
            }

        @Suppress("kotlin:S5324")
        private fun getExternalSdCard(): SdCardInfo? {
            val externalDirs = context.getExternalFilesDirs(null)
            if (externalDirs.size < 2) return null
            val sdCardDir = externalDirs[1] ?: return null
            return try {
                val stat = StatFs(sdCardDir.path)
                SdCardInfo(totalBytes = stat.totalBytes, availableBytes = stat.availableBytes)
            } catch (_: Exception) {
                null
            }
        }

        data class StorageInfo(
            val totalBytes: Long,
            val availableBytes: Long,
            val usedBytes: Long,
            val appsBytes: Long?,
            val totalCacheBytes: Long?,
            val appCount: Int?,
            val mediaBreakdown: MediaBreakdown?,
            val trashInfo: TrashInfo?,
            val sdCardAvailable: Boolean,
            val sdCardTotalBytes: Long?,
            val sdCardAvailableBytes: Long?,
            val fileSystemType: String?,
            val encryptionStatus: String?,
            val storageVolumes: Int,
        )

        private data class AppStats(
            val totalBytes: Long,
            val cacheBytes: Long,
            val appCount: Int?,
        )

        private data class SdCardInfo(
            val totalBytes: Long,
            val availableBytes: Long,
        )

        @Suppress("TooGenericExceptionCaught", "CyclomaticComplexMethod")
        private fun getDeviceStorageInfo(): DeviceStorageInfo {
            var fsType: String? = null
            var volumes = 1

            // File system type from /proc/mounts
            try {
                File("/proc/mounts").useLines { lines ->
                    for (line in lines) {
                        val parts = line.split(" ")
                        if (parts.size >= 3 && parts[1] == "/data") {
                            fsType = parts[2]
                            break
                        }
                    }
                }
            } catch (e: java.io.IOException) {
                ReleaseSafeLog.error(TAG, "Failed to read /proc/mounts", e)
            }

            // Storage volumes via StorageManager
            try {
                val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                volumes = sm.storageVolumes.size
            } catch (e: RuntimeException) {
                ReleaseSafeLog.error(TAG, "Failed to count storage volumes", e)
            }

            return DeviceStorageInfo(
                fileSystemType = fsType,
                encryptionStatus = getStorageEncryptionStatus(),
                storageVolumes = volumes,
            )
        }

        private fun getStorageEncryptionStatus(): String? =
            try {
                devicePolicyManager
                    ?.storageEncryptionStatus
                    ?.let(::mapStorageEncryptionStatus)
            } catch (_: RuntimeException) {
                null
            }

        private data class DeviceStorageInfo(
            val fileSystemType: String?,
            val encryptionStatus: String?,
            val storageVolumes: Int,
        )

        private companion object {
            private const val TAG = "StorageDataSource"
        }
    }

internal fun mapStorageEncryptionStatus(status: Int): String? =
    when (status) {
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> "FBE"
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE -> "Encrypted"
        DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> "Inactive"
        DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> "Unsupported"
        else -> null
    }

internal fun countDistinctPackageNames(packageNames: Iterable<String>): Int? {
    val count =
        packageNames
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
            .size
    return count.takeIf { it > 0 }
}
