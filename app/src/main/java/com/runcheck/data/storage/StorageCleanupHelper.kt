package com.runcheck.data.storage

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.runcheck.util.ReleaseSafeLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageCleanupHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * API 30+: Creates a PendingIntent that shows the system confirmation dialog
     * for batch deletion. Returns null on older API levels.
     */
    fun createDeleteRequest(uriStrings: List<String>): PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uriStrings.isEmpty()) return null
        val uris = uriStrings.map { Uri.parse(it) }
        return MediaStore.createDeleteRequest(context.contentResolver, uris)
    }

    /**
     * API 29 and below: Deletes files one by one without system dialog.
     * Returns count of successfully deleted files.
     */
    suspend fun deleteLegacy(uriStrings: List<String>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        uriStrings.forEach { uriString ->
            try {
                val uri = Uri.parse(uriString)
                if (context.contentResolver.delete(uri, null, null) > 0) deleted++
            } catch (error: SecurityException) {
                ReleaseSafeLog.error(TAG, "Failed to delete legacy media item", error)
            }
        }
        deleted
    }

    private companion object {
        private const val TAG = "StorageCleanupHelper"
    }
}
