package com.devicepulse.data.storage

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val storageStatsManager =
        context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        // StorageStatsManager reports full disk (matches Settings), StatFs only /data
        val ssm = storageStatsManager
        val uuid = StorageManager.UUID_DEFAULT
        val totalBytes = try {
            ssm?.getTotalBytes(uuid) ?: StatFs(Environment.getDataDirectory().path).totalBytes
        } catch (_: Exception) {
            StatFs(Environment.getDataDirectory().path).totalBytes
        }
        val freeBytes = try {
            ssm?.getFreeBytes(uuid) ?: StatFs(Environment.getDataDirectory().path).availableBytes
        } catch (_: Exception) {
            StatFs(Environment.getDataDirectory().path).availableBytes
        }
        val availableBytes = freeBytes
        val usedBytes = totalBytes - availableBytes

        val appsBytes = if (hasUsageStatsPermission()) {
            calculateAppsSizeAccurate()
        } else {
            null
        }
        val sdCard = getExternalSdCard()

        StorageInfo(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            usedBytes = usedBytes,
            appsBytes = appsBytes,
            mediaBytes = null,
            sdCardAvailable = sdCard != null,
            sdCardTotalBytes = sdCard?.totalBytes,
            sdCardAvailableBytes = sdCard?.availableBytes
        )
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun calculateAppsSizeAccurate(): Long? {
        val ssm = storageStatsManager ?: return null
        return try {
            val uuid = StorageManager.UUID_DEFAULT
            val user = android.os.Process.myUserHandle()
            val stats = ssm.queryStatsForUser(uuid, user)
            val total = stats.appBytes + stats.dataBytes + stats.cacheBytes
            if (total > 0) total else null
        } catch (_: Exception) {
            null
        }
    }

    private fun getExternalSdCard(): SdCardInfo? {
        val externalDirs = context.getExternalFilesDirs(null)
        if (externalDirs.size < 2) return null

        val sdCardDir = externalDirs[1] ?: return null
        return try {
            val stat = StatFs(sdCardDir.path)
            SdCardInfo(
                totalBytes = stat.totalBytes,
                availableBytes = stat.availableBytes
            )
        } catch (e: Exception) {
            null
        }
    }

    data class StorageInfo(
        val totalBytes: Long,
        val availableBytes: Long,
        val usedBytes: Long,
        val appsBytes: Long?,
        val mediaBytes: Long?,
        val sdCardAvailable: Boolean,
        val sdCardTotalBytes: Long?,
        val sdCardAvailableBytes: Long?
    )

    private data class SdCardInfo(
        val totalBytes: Long,
        val availableBytes: Long
    )
}
