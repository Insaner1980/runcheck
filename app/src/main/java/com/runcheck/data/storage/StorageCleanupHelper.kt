package com.runcheck.data.storage

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageCleanupHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * API 30+: Creates a PendingIntent that shows the system confirmation dialog
     * for batch deletion. Returns null on older API levels.
     */
    fun createDeleteRequest(uris: List<Uri>): PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uris.isEmpty()) return null
        return MediaStore.createDeleteRequest(context.contentResolver, uris)
    }

    /**
     * API 29 and below: Deletes files one by one without system dialog.
     * Returns count of successfully deleted files.
     */
    suspend fun deleteLegacy(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        uris.forEach { uri ->
            try {
                if (context.contentResolver.delete(uri, null, null) > 0) deleted++
            } catch (_: SecurityException) { }
        }
        deleted
    }

}
