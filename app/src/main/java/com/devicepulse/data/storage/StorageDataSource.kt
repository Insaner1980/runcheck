package com.devicepulse.data.storage

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
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

        val appsBytes = calculateInstalledAppsSize()
        val mediaBytes = calculateMediaSize()
        val sdCard = getExternalSdCard()

        StorageInfo(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            usedBytes = usedBytes,
            appsBytes = appsBytes,
            mediaBytes = mediaBytes,
            sdCardAvailable = sdCard != null,
            sdCardTotalBytes = sdCard?.totalBytes,
            sdCardAvailableBytes = sdCard?.availableBytes
        )
    }

    private fun calculateInstalledAppsSize(): Long? {
        return try {
            val pm = context.packageManager
            pm.getInstalledApplications(0)
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .sumOf { app ->
                    try {
                        java.io.File(app.sourceDir).length()
                    } catch (_: Exception) {
                        0L
                    }
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateMediaSize(): Long? {
        return try {
            val collections = listOf(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            )
            var total = 0L
            for (uri in collections) {
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.MediaColumns.SIZE),
                    null, null, null
                )?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    while (cursor.moveToNext()) {
                        total += cursor.getLong(sizeIndex)
                    }
                }
            }
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
