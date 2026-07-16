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
import android.os.storage.StorageVolume
import com.runcheck.domain.model.MediaBreakdown
import com.runcheck.domain.model.TrashInfo
import com.runcheck.util.AppDispatchers
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val DATA_MOUNT_POINT = "/data"
private val MOUNT_FIELD_SEPARATOR = Regex("\\s+")

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
        private val storageManager =
            context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
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
                val primarySpace =
                    queryPrimaryStorageSpace(
                        storageStatsQuery =
                            ssm?.let { manager ->
                                {
                                    StorageSpace(
                                        totalBytes = manager.getTotalBytes(uuid),
                                        availableBytes = manager.getFreeBytes(uuid),
                                    )
                                }
                            },
                        statFsQuery = {
                            val stat = StatFs(Environment.getDataDirectory().path)
                            StorageSpace(totalBytes = stat.totalBytes, availableBytes = stat.availableBytes)
                        },
                    )
                val totalBytes = primarySpace.totalBytes
                val freeBytes = primarySpace.availableBytes
                val usedBytes = totalBytes - freeBytes

                val hasUsageStats = hasUsageStatsPermission()
                val appStats = if (hasUsageStats) calculateAppStats() else null
                val appCount = countLaunchableApps()
                val removableStorage = getRemovableStorage()
                val deviceInfo = getDeviceStorageInfo()

                StorageInfo(
                    totalBytes = totalBytes,
                    availableBytes = freeBytes,
                    usedBytes = usedBytes,
                    appsBytes = appStats?.totalBytes,
                    totalCacheBytes = appStats?.cacheBytes,
                    appCount = appCount,
                    mediaBreakdown = mediaBreakdown,
                    trashInfo = trashInfo,
                    removableStorageAvailable = removableStorage != null,
                    removableStorageTotalBytes = removableStorage?.totalBytes,
                    removableStorageAvailableBytes = removableStorage?.availableBytes,
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
            val bytes =
                queryAggregateAppStats {
                    val uuid = StorageManager.UUID_DEFAULT
                    val user = android.os.Process.myUserHandle()
                    val stats = ssm.queryStatsForUser(uuid, user)
                    AppStorageByteBreakdown(
                        appBytes = stats.appBytes,
                        dataBytes = stats.dataBytes,
                        cacheBytes = stats.cacheBytes,
                    )
                } ?: return null
            return AppStats(
                totalBytes = bytes.totalBytes,
                cacheBytes = bytes.cacheBytes,
            )
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
        private fun getRemovableStorage(): RemovableStorageInfo? {
            val sm = storageManager ?: return null
            val directories =
                try {
                    selectMountedPortableStorageDirectories(
                        context.getExternalFilesDirs(null).mapNotNull { directory ->
                            directory ?: return@mapNotNull null
                            val volume = sm.getStorageVolume(directory) ?: return@mapNotNull null
                            volume.toCandidate(directory)
                        },
                    )
                } catch (_: RuntimeException) {
                    return null
                }
            if (directories.isEmpty()) return null

            val space =
                aggregateStorageSpaces(
                    directories.map { directory ->
                        try {
                            val stat = StatFs(directory.path)
                            StorageSpace(stat.totalBytes, stat.availableBytes)
                        } catch (_: RuntimeException) {
                            null
                        }
                    },
                )
            return RemovableStorageInfo(
                totalBytes = space?.totalBytes,
                availableBytes = space?.availableBytes,
            )
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
            val removableStorageAvailable: Boolean,
            val removableStorageTotalBytes: Long?,
            val removableStorageAvailableBytes: Long?,
            val fileSystemType: String?,
            val encryptionStatus: String?,
            val storageVolumes: Int,
        )

        private data class AppStats(
            val totalBytes: Long,
            val cacheBytes: Long,
        )

        private data class RemovableStorageInfo(
            val totalBytes: Long?,
            val availableBytes: Long?,
        )

        @Suppress("TooGenericExceptionCaught", "CyclomaticComplexMethod")
        private fun getDeviceStorageInfo(): DeviceStorageInfo {
            var volumes = 0

            // File system type from /proc/mounts
            val fsType =
                readDataFileSystemType(
                    openMounts = { File("/proc/mounts").bufferedReader() },
                    onFailure = { error ->
                        ReleaseSafeLog.error(TAG, "Failed to read /proc/mounts", error)
                    },
                )

            // Storage volumes via StorageManager
            try {
                volumes = storageManager?.storageVolumes?.count { it.state.isMountedStorageState() } ?: 0
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

internal fun parseDataFileSystemType(lines: Sequence<String>): String? {
    for (line in lines) {
        val fields = line.trim().split(MOUNT_FIELD_SEPARATOR, limit = 4)
        if (fields.size >= 3 && fields[1] == DATA_MOUNT_POINT) return fields[2]
    }
    return null
}

internal fun readDataFileSystemType(
    openMounts: () -> BufferedReader,
    onFailure: (Exception) -> Unit = {},
): String? =
    try {
        openMounts().useLines(::parseDataFileSystemType)
    } catch (error: IOException) {
        onFailure(error)
        null
    } catch (error: SecurityException) {
        onFailure(error)
        null
    }

@Suppress("DEPRECATION")
internal fun mapStorageEncryptionStatus(status: Int): String? =
    when (status) {
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> "FBE"
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE -> "Encrypted"
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> "Encrypted (default key)"
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING -> "Activating"
        DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> "Inactive"
        DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> "Unsupported"
        else -> null
    }

internal fun countDistinctPackageNames(packageNames: Iterable<String>): Int =
    packageNames
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()
        .size

internal data class StorageVolumeCandidate(
    val directory: File,
    val isPrimary: Boolean,
    val isRemovable: Boolean,
    val isEmulated: Boolean,
    val state: String,
)

internal data class StorageSpace(
    val totalBytes: Long,
    val availableBytes: Long,
)

internal fun selectMountedPortableStorageDirectories(candidates: Iterable<StorageVolumeCandidate>): List<File> =
    candidates
        .filter { candidate ->
            !candidate.isPrimary &&
                candidate.isRemovable &&
                !candidate.isEmulated &&
                candidate.state.isMountedStorageState()
        }.distinctBy { it.directory.absolutePath }
        .map(StorageVolumeCandidate::directory)

internal fun aggregateStorageSpaces(spaces: Iterable<StorageSpace?>): StorageSpace? {
    var totalBytes = 0L
    var availableBytes = 0L
    for (space in spaces) {
        space ?: return null
        if (!space.isValid()) return null
        try {
            totalBytes = Math.addExact(totalBytes, space.totalBytes)
            availableBytes = Math.addExact(availableBytes, space.availableBytes)
        } catch (_: ArithmeticException) {
            return null
        }
    }
    return StorageSpace(totalBytes = totalBytes, availableBytes = availableBytes)
}

internal fun queryPrimaryStorageSpace(
    storageStatsQuery: (() -> StorageSpace)?,
    statFsQuery: () -> StorageSpace,
): StorageSpace {
    val storageStatsSpace =
        try {
            storageStatsQuery?.invoke()
        } catch (_: Exception) {
            null
        }
    return storageStatsSpace?.takeIf(StorageSpace::isValid) ?: statFsQuery()
}

private fun StorageSpace.isValid(): Boolean = totalBytes >= 0L && availableBytes in 0L..totalBytes

private fun String.isMountedStorageState(): Boolean =
    this == Environment.MEDIA_MOUNTED || this == Environment.MEDIA_MOUNTED_READ_ONLY

private fun StorageVolume.toCandidate(directory: File) =
    StorageVolumeCandidate(
        directory = directory,
        isPrimary = isPrimary,
        isRemovable = isRemovable,
        isEmulated = isEmulated,
        state = state,
    )

internal data class AggregateAppStats(
    val totalBytes: Long,
    val cacheBytes: Long,
)

internal data class AppStorageByteBreakdown(
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
)

internal fun queryAggregateAppStats(query: () -> AppStorageByteBreakdown): AggregateAppStats? =
    try {
        val bytes = query()
        AggregateAppStats(
            totalBytes = bytes.appBytes + bytes.dataBytes,
            cacheBytes = bytes.cacheBytes,
        )
    } catch (_: Exception) {
        null
    }
