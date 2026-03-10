package com.devicepulse.data.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val storageStatsManager =
        context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    private val storageManager =
        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val availableBytes = stat.availableBytes
        val usedBytes = totalBytes - availableBytes

        val categoryBreakdown = getCategoryBreakdown(totalBytes, availableBytes)

        val sdCard = getExternalSdCard()

        StorageInfo(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            usedBytes = usedBytes,
            appsBytes = categoryBreakdown.appsBytes,
            mediaBytes = categoryBreakdown.mediaBytes,
            sdCardAvailable = sdCard != null,
            sdCardTotalBytes = sdCard?.totalBytes,
            sdCardAvailableBytes = sdCard?.availableBytes
        )
    }

    private fun getCategoryBreakdown(totalBytes: Long, availableBytes: Long): CategoryBreakdown {
        return try {
            val uuid = StorageManager.UUID_DEFAULT
            val totalSpace = storageStatsManager.getTotalBytes(uuid)
            val freeSpace = storageStatsManager.getFreeBytes(uuid)

            // Approximate breakdown — exact per-category requires PACKAGE_USAGE_STATS
            val usedBytes = totalBytes - availableBytes
            CategoryBreakdown(
                appsBytes = usedBytes / 3, // Rough estimate
                mediaBytes = usedBytes / 3  // Rough estimate
            )
        } catch (e: Exception) {
            val usedBytes = totalBytes - availableBytes
            CategoryBreakdown(
                appsBytes = usedBytes / 3,
                mediaBytes = usedBytes / 3
            )
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
        val appsBytes: Long,
        val mediaBytes: Long,
        val sdCardAvailable: Boolean,
        val sdCardTotalBytes: Long?,
        val sdCardAvailableBytes: Long?
    )

    private data class CategoryBreakdown(
        val appsBytes: Long,
        val mediaBytes: Long
    )

    private data class SdCardInfo(
        val totalBytes: Long,
        val availableBytes: Long
    )
}
