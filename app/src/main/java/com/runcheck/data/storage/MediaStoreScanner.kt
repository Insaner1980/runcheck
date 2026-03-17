package com.runcheck.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.runcheck.domain.model.MediaBreakdown
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.TrashInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun getMediaBreakdown(): MediaBreakdown = withContext(Dispatchers.IO) {
        MediaBreakdown(
            imagesBytes = queryTotalSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            videosBytes = queryTotalSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
            audioBytes = queryTotalSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
            documentsBytes = queryDocumentsSize(),
            downloadsBytes = queryDownloadsSize()
        )
    }

    suspend fun getTrashInfo(): TrashInfo? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext null

        var totalSize = 0L
        var itemCount = 0

        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        )

        for (collection in collections) {
            val bundle = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.MediaColumns.SIZE)
                )
            }
            try {
                resolver.query(collection, arrayOf(MediaStore.MediaColumns.SIZE), bundle, null)
                    ?.use { cursor ->
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        while (cursor.moveToNext()) {
                            totalSize += cursor.getLong(sizeCol)
                            itemCount++
                        }
                    }
            } catch (_: Exception) { }
        }

        if (itemCount == 0) return@withContext null
        TrashInfo(totalBytes = totalSize, itemCount = itemCount)
    }

    suspend fun scanLargeFiles(thresholdBytes: Long): List<ScannedFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedFile>()
        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to MediaCategory.IMAGE,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to MediaCategory.VIDEO,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to MediaCategory.AUDIO
        )

        // Add downloads (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanCollection(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                MediaCategory.DOWNLOAD,
                thresholdBytes,
                results
            )
        }

        for ((uri, category) in collections) {
            scanCollection(uri, category, thresholdBytes, results)
        }

        results.sortedByDescending { it.sizeBytes }
    }

    suspend fun scanOldDownloads(olderThanMs: Long): List<ScannedFile> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext emptyList()

        val cutoff = (System.currentTimeMillis() - olderThanMs) / 1000 // MediaStore uses seconds
        val results = mutableListOf<ScannedFile>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.MediaColumns.DATE_MODIFIED} < ?"
        val selectionArgs = arrayOf(cutoff.toString())
        val sortOrder = "${MediaStore.MediaColumns.SIZE} DESC"

        try {
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    results.add(
                        ScannedFile(
                            uri = android.content.ContentUris.withAppendedId(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI, id
                            ),
                            displayName = cursor.getString(nameCol) ?: "Unknown",
                            sizeBytes = cursor.getLong(sizeCol),
                            mimeType = cursor.getString(mimeCol) ?: "",
                            dateModified = cursor.getLong(dateCol) * 1000,
                            category = MediaCategory.DOWNLOAD
                        )
                    )
                }
            }
        } catch (_: Exception) { }

        results
    }

    suspend fun scanApkFiles(): List<ScannedFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedFile>()
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%.apk")
        val sortOrder = "${MediaStore.MediaColumns.SIZE} DESC"

        try {
            resolver.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        results.add(
                            ScannedFile(
                                uri = android.content.ContentUris.withAppendedId(uri, id),
                                displayName = cursor.getString(nameCol) ?: "Unknown",
                                sizeBytes = cursor.getLong(sizeCol),
                                mimeType = "application/vnd.android.package-archive",
                                dateModified = cursor.getLong(dateCol) * 1000,
                                category = MediaCategory.APK
                            )
                        )
                    }
                }
        } catch (_: Exception) { }

        results
    }

    private fun queryTotalSize(contentUri: Uri): Long {
        return try {
            resolver.query(
                contentUri,
                arrayOf("SUM(${MediaStore.MediaColumns.SIZE})"),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun queryDocumentsSize(): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0L

        val docMimeTypes = listOf(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats%", "text/%",
            "application/vnd.oasis.opendocument%"
        )

        var total = 0L
        val uri = MediaStore.Files.getContentUri("external")

        for (mimePattern in docMimeTypes) {
            try {
                resolver.query(
                    uri,
                    arrayOf("SUM(${MediaStore.MediaColumns.SIZE})"),
                    "${MediaStore.MediaColumns.MIME_TYPE} LIKE ?",
                    arrayOf(mimePattern),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) total += cursor.getLong(0)
                }
            } catch (_: Exception) { }
        }

        return total
    }

    private fun queryDownloadsSize(): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0L
        return queryTotalSize(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
    }

    private fun scanCollection(
        uri: Uri,
        category: MediaCategory,
        thresholdBytes: Long,
        results: MutableList<ScannedFile>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.MediaColumns.SIZE} > ?"
        val selectionArgs = arrayOf(thresholdBytes.toString())
        val sortOrder = "${MediaStore.MediaColumns.SIZE} DESC LIMIT 100"

        try {
            resolver.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        results.add(
                            ScannedFile(
                                uri = android.content.ContentUris.withAppendedId(uri, id),
                                displayName = cursor.getString(nameCol) ?: "Unknown",
                                sizeBytes = cursor.getLong(sizeCol),
                                mimeType = cursor.getString(mimeCol) ?: "",
                                dateModified = cursor.getLong(dateCol) * 1000,
                                category = category
                            )
                        )
                    }
                }
        } catch (_: Exception) { }
    }
}

