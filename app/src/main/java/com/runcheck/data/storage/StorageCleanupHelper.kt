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

    /**
     * Collects all trashed media URIs for emptying the trash.
     */
    suspend fun getTrashedUris(): List<Uri> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()

        val uris = mutableListOf<Uri>()
        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        )

        for (collection in collections) {
            val bundle = android.os.Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            }
            try {
                context.contentResolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    bundle,
                    null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        uris.add(android.content.ContentUris.withAppendedId(collection, id))
                    }
                }
            } catch (_: Exception) { }
        }
        uris
    }
}
