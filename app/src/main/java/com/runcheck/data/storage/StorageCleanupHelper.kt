package com.runcheck.data.storage

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.runcheck.domain.model.StorageDeleteFailure
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

    fun createDeleteRequestFromUris(uris: List<Uri>): PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uris.isEmpty()) return null
        return MediaStore.createDeleteRequest(context.contentResolver, uris)
    }

    /**
     * API 29 and below: Deletes files one by one without system dialog.
     * Returns the set of URIs successfully deleted.
     */
    suspend fun deleteLegacy(uriStrings: Collection<String>): Set<String> = withContext(Dispatchers.IO) {
        val deletedUris = mutableSetOf<String>()
        uriStrings.forEach { uriString ->
            try {
                val uri = Uri.parse(uriString)
                if (context.contentResolver.delete(uri, null, null) > 0) {
                    deletedUris += uriString
                }
            } catch (error: RecoverableSecurityException) {
                ReleaseSafeLog.error(TAG, "Delete needs user confirmation on this Android version", error)
                throw StorageDeleteFailure(
                    deletedUris = deletedUris.toSet(),
                    recoverable = true
                )
            } catch (error: SecurityException) {
                ReleaseSafeLog.error(TAG, "Failed to delete legacy media item", error)
                throw StorageDeleteFailure(
                    deletedUris = deletedUris.toSet(),
                    recoverable = false
                )
            }
        }
        deletedUris
    }

    private companion object {
        private const val TAG = "StorageCleanupHelper"
    }
}
