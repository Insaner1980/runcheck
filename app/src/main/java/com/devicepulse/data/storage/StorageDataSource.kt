package com.devicepulse.data.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val availableBytes = stat.availableBytes
        val usedBytes = totalBytes - availableBytes

        val sdCard = getExternalSdCard()

        StorageInfo(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            usedBytes = usedBytes,
            appsBytes = null,
            mediaBytes = null,
            sdCardAvailable = sdCard != null,
            sdCardTotalBytes = sdCard?.totalBytes,
            sdCardAvailableBytes = sdCard?.availableBytes
        )
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
