package com.runcheck.data.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.runcheck.R
import com.runcheck.domain.model.CleanupGroupSummary
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupScanSource
import com.runcheck.domain.model.CleanupSummary
import com.runcheck.domain.model.MediaBreakdown
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.TrashInfo
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreScanner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val resolver: ContentResolver = context.contentResolver
    private val externalFilesUri: Uri = MediaStore.Files.getContentUri("external")

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
        val coroutineContext = currentCoroutineContext()

        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        )

        for (collection in collections) {
            coroutineContext.ensureActive()
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
                            coroutineContext.ensureActive()
                            totalSize += cursor.getLong(sizeCol)
                            itemCount++
                        }
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to query trashed media for $collection", error)
            }
        }

        if (itemCount == 0) return@withContext null
        TrashInfo(totalBytes = totalSize, itemCount = itemCount)
    }

    suspend fun getTrashedUris(): List<Uri> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()

        val uris = mutableListOf<Uri>()
        val coroutineContext = currentCoroutineContext()
        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        )

        for (collection in collections) {
            coroutineContext.ensureActive()
            val bundle = Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            }
            try {
                resolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), bundle, null)
                    ?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        while (cursor.moveToNext()) {
                            coroutineContext.ensureActive()
                            uris.add(android.content.ContentUris.withAppendedId(collection, cursor.getLong(idCol)))
                        }
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to query trashed URIs for $collection", error)
            }
        }

        uris
    }

    suspend fun findExistingUris(uriStrings: Collection<String>): Set<String> = withContext(Dispatchers.IO) {
        if (uriStrings.isEmpty()) return@withContext emptySet()
        val coroutineContext = currentCoroutineContext()

        buildSet {
            uriStrings.forEach { uriString ->
                coroutineContext.ensureActive()
                try {
                    resolver.query(Uri.parse(uriString), arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                add(uriString)
                            }
                        }
                } catch (_: Exception) {
                    // Treat inaccessible or missing rows as deleted to avoid overstating failures.
                }
            }
        }
    }

    suspend fun getCleanupSummary(query: CleanupScanQuery): CleanupSummary = withContext(Dispatchers.IO) {
        when (query.source) {
            CleanupScanSource.LARGE_FILES -> getLargeFilesSummary(query.filterValue)
            CleanupScanSource.OLD_DOWNLOADS -> getOldDownloadsSummary(query.filterValue)
            CleanupScanSource.APK_FILES -> getApkSummary()
        }
    }

    suspend fun loadCleanupPage(
        query: CleanupScanQuery,
        category: MediaCategory,
        offset: Int,
        limit: Int
    ): List<ScannedFile> = withContext(Dispatchers.IO) {
        when (query.source) {
            CleanupScanSource.LARGE_FILES -> loadLargeFilesPage(category, query.filterValue, offset, limit)
            CleanupScanSource.OLD_DOWNLOADS -> loadOldDownloadsPage(offset, limit, query.filterValue)
            CleanupScanSource.APK_FILES -> loadApkPage(offset, limit)
        }
    }

    suspend fun getCleanupGroupUris(
        query: CleanupScanQuery,
        category: MediaCategory
    ): Set<String> = withContext(Dispatchers.IO) {
        when (query.source) {
            CleanupScanSource.LARGE_FILES -> queryUrisForLargeFileCategory(category, query.filterValue)
            CleanupScanSource.OLD_DOWNLOADS -> queryOldDownloadUris(query.filterValue)
            CleanupScanSource.APK_FILES -> queryApkUris()
        }
    }

    suspend fun scanLargeFiles(thresholdBytes: Long): List<ScannedFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedFile>()
        val coroutineContext = currentCoroutineContext()
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
            coroutineContext.ensureActive()
            scanCollection(uri, category, thresholdBytes, results)
        }

        results.sortedByDescending { it.sizeBytes }
    }

    suspend fun scanOldDownloads(olderThanMs: Long): List<ScannedFile> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext emptyList()

        val cutoff = (System.currentTimeMillis() - olderThanMs) / 1000 // MediaStore uses seconds
        val results = mutableListOf<ScannedFile>()
        val coroutineContext = currentCoroutineContext()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.MediaColumns.DATE_MODIFIED} < ?"
        val selectionArgs = arrayOf(cutoff.toString())
        val sortOrder = "${MediaStore.MediaColumns.SIZE} DESC "

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
                    coroutineContext.ensureActive()
                    val id = cursor.getLong(idCol)
                    results.add(
                        ScannedFile(
                            uri = android.content.ContentUris.withAppendedId(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI, id
                            ).toString(),
                            displayName = cursor.getString(nameCol) ?: context.getString(R.string.fallback_unknown),
                            sizeBytes = cursor.getLong(sizeCol),
                            mimeType = cursor.getString(mimeCol) ?: "",
                            dateModified = cursor.getLong(dateCol) * 1000,
                            category = MediaCategory.DOWNLOAD
                        )
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to scan old downloads", error)
        }

        results
    }

    suspend fun scanApkFiles(): List<ScannedFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedFile>()
        val seen = mutableSetOf<Long>()
        val coroutineContext = currentCoroutineContext()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        // Search multiple collections — APKs can live in Downloads or Files
        val collections = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
            }
            add(MediaStore.Files.getContentUri("external"))
        }

        for (collectionUri in collections) {
            coroutineContext.ensureActive()
            // Try both filename match and MIME type match
            val queries = listOf(
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?" to arrayOf("%.apk"),
                "${MediaStore.MediaColumns.MIME_TYPE} = ?" to arrayOf("application/vnd.android.package-archive")
            )

            for ((selection, selectionArgs) in queries) {
                coroutineContext.ensureActive()
                try {
                    resolver.query(
                        collectionUri, projection, selection, selectionArgs,
                        "${MediaStore.MediaColumns.SIZE} DESC"
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                        while (cursor.moveToNext()) {
                            coroutineContext.ensureActive()
                            val id = cursor.getLong(idCol)
                            if (!seen.add(id)) continue
                            results.add(
                                ScannedFile(
                                    uri = android.content.ContentUris.withAppendedId(collectionUri, id).toString(),
                                    displayName = cursor.getString(nameCol) ?: context.getString(R.string.fallback_unknown),
                                    sizeBytes = cursor.getLong(sizeCol),
                                    mimeType = "application/vnd.android.package-archive",
                                    dateModified = cursor.getLong(dateCol) * 1000,
                                    category = MediaCategory.APK
                                )
                            )
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    ReleaseSafeLog.error(TAG, "Failed to scan APK files in $collectionUri with $selection", error)
                }
            }
        }

        results.sortedByDescending { it.sizeBytes }
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
        } catch (error: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to query total size for $contentUri", error)
            0L
        }
    }

    private suspend fun queryDocumentsSize(): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0L

        val docMimeTypes = listOf(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats%", "text/%",
            "application/vnd.oasis.opendocument%"
        )

        var total = 0L
        val uri = MediaStore.Files.getContentUri("external")
        val coroutineContext = currentCoroutineContext()

        for (mimePattern in docMimeTypes) {
            coroutineContext.ensureActive()
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
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to query documents size for $mimePattern", error)
            }
        }

        return total
    }

    private fun queryDownloadsSize(): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0L
        return queryTotalSize(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
    }

    private suspend fun scanCollection(
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
        val sortOrder = "${MediaStore.MediaColumns.SIZE} DESC "
        val coroutineContext = currentCoroutineContext()

        try {
            coroutineContext.ensureActive()
            resolver.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        coroutineContext.ensureActive()
                        val id = cursor.getLong(idCol)
                        results.add(
                            ScannedFile(
                                uri = android.content.ContentUris.withAppendedId(uri, id).toString(),
                                displayName = cursor.getString(nameCol) ?: context.getString(R.string.fallback_unknown),
                                sizeBytes = cursor.getLong(sizeCol),
                                mimeType = cursor.getString(mimeCol) ?: "",
                                dateModified = cursor.getLong(dateCol) * 1000,
                                category = category
                            )
                        )
                    }
                }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to scan $category collection", error)
        }
    }

    private suspend fun getLargeFilesSummary(thresholdBytes: Long): CleanupSummary {
        val groups = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                queryAggregateSummary(
                    collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    category = MediaCategory.DOWNLOAD,
                    selection = "${MediaStore.MediaColumns.SIZE} > ?",
                    selectionArgs = arrayOf(thresholdBytes.toString())
                )?.let(::add)
            }

            largeFileCollections().forEach { descriptor ->
                queryAggregateSummary(
                    collection = descriptor.uri,
                    category = descriptor.category,
                    selection = "${MediaStore.MediaColumns.SIZE} > ?",
                    selectionArgs = arrayOf(thresholdBytes.toString())
                )?.let(::add)
            }
        }.sortedByDescending { it.group.totalBytes }

        return groups.toCleanupSummary()
    }

    private suspend fun getOldDownloadsSummary(olderThanMs: Long): CleanupSummary {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return CleanupSummary(emptyList(), 0, 0L, 0L)
        val cutoff = (System.currentTimeMillis() - olderThanMs) / 1000
        val selection = "${MediaStore.MediaColumns.DATE_MODIFIED} < ?"
        val selectionArgs = arrayOf(cutoff.toString())
        val group = queryAggregateSummary(
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            category = MediaCategory.DOWNLOAD,
            selection = selection,
            selectionArgs = selectionArgs
        )
        return listOfNotNull(group).toCleanupSummary()
    }

    private suspend fun getApkSummary(): CleanupSummary {
        val group = queryAggregateSummary(
            collection = externalFilesUri,
            category = MediaCategory.APK,
            selection = APK_SELECTION,
            selectionArgs = APK_SELECTION_ARGS
        )
        return listOfNotNull(group).toCleanupSummary()
    }

    private suspend fun queryAggregateSummary(
        collection: Uri,
        category: MediaCategory,
        selection: String,
        selectionArgs: Array<String>
    ): CleanupAggregateSummary? {
        val projection = arrayOf(
            "COUNT(*)",
            "COALESCE(SUM(${MediaStore.MediaColumns.SIZE}), 0)",
            "COALESCE(MAX(${MediaStore.MediaColumns.SIZE}), 0)"
        )
        return try {
            resolver.query(collection, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val count = cursor.getInt(0)
                    if (count == 0) return@use null
                    CleanupAggregateSummary(
                        group = CleanupGroupSummary(
                            category = category,
                            itemCount = count,
                            totalBytes = cursor.getLong(1)
                        ),
                        maxFileSizeBytes = cursor.getLong(2)
                    )
                }
        } catch (error: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to aggregate cleanup summary for $category", error)
            null
        }
    }

    private suspend fun loadLargeFilesPage(
        category: MediaCategory,
        thresholdBytes: Long,
        offset: Int,
        limit: Int
    ): List<ScannedFile> {
        val descriptor = largeFileCollectionFor(category) ?: return emptyList()
        return queryPagedFiles(
            collection = descriptor.uri,
            category = descriptor.category,
            selection = "${MediaStore.MediaColumns.SIZE} > ?",
            selectionArgs = arrayOf(thresholdBytes.toString()),
            sortOrder = "${MediaStore.MediaColumns.SIZE} DESC",
            offset = offset,
            limit = limit
        )
    }

    private suspend fun loadOldDownloadsPage(
        offset: Int,
        limit: Int,
        olderThanMs: Long
    ): List<ScannedFile> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val cutoff = (System.currentTimeMillis() - olderThanMs) / 1000
        return queryPagedFiles(
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            category = MediaCategory.DOWNLOAD,
            selection = "${MediaStore.MediaColumns.DATE_MODIFIED} < ?",
            selectionArgs = arrayOf(cutoff.toString()),
            sortOrder = "${MediaStore.MediaColumns.SIZE} DESC",
            offset = offset,
            limit = limit
        )
    }

    private suspend fun loadApkPage(offset: Int, limit: Int): List<ScannedFile> =
        queryPagedFiles(
            collection = externalFilesUri,
            category = MediaCategory.APK,
            selection = APK_SELECTION,
            selectionArgs = APK_SELECTION_ARGS,
            sortOrder = "${MediaStore.MediaColumns.SIZE} DESC",
            offset = offset,
            limit = limit
        )

    private suspend fun queryUrisForLargeFileCategory(
        category: MediaCategory,
        thresholdBytes: Long
    ): Set<String> {
        val descriptor = largeFileCollectionFor(category) ?: return emptySet()
        return queryUris(
            collection = descriptor.uri,
            selection = "${MediaStore.MediaColumns.SIZE} > ?",
            selectionArgs = arrayOf(thresholdBytes.toString())
        )
    }

    private suspend fun queryOldDownloadUris(olderThanMs: Long): Set<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptySet()
        val cutoff = (System.currentTimeMillis() - olderThanMs) / 1000
        return queryUris(
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            selection = "${MediaStore.MediaColumns.DATE_MODIFIED} < ?",
            selectionArgs = arrayOf(cutoff.toString())
        )
    }

    private suspend fun queryApkUris(): Set<String> =
        queryUris(
            collection = externalFilesUri,
            selection = APK_SELECTION,
            selectionArgs = APK_SELECTION_ARGS
        )

    private suspend fun queryPagedFiles(
        collection: Uri,
        category: MediaCategory,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        offset: Int,
        limit: Int
    ): List<ScannedFile> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        return try {
            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "$sortOrder LIMIT $limit OFFSET $offset"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            ScannedFile(
                                uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(idCol)).toString(),
                                displayName = cursor.getString(nameCol) ?: context.getString(R.string.fallback_unknown),
                                sizeBytes = cursor.getLong(sizeCol),
                                mimeType = cursor.getString(mimeCol) ?: "",
                                dateModified = cursor.getLong(dateCol) * 1000,
                                category = category
                            )
                        )
                    }
                }
            } ?: emptyList()
        } catch (error: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to query paged cleanup items for $category", error)
            emptyList()
        }
    }

    private suspend fun queryUris(
        collection: Uri,
        selection: String,
        selectionArgs: Array<String>
    ): Set<String> = withContext(Dispatchers.IO) {
        try {
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                buildSet {
                    while (cursor.moveToNext()) {
                        add(android.content.ContentUris.withAppendedId(collection, cursor.getLong(idCol)).toString())
                    }
                }
            } ?: emptySet()
        } catch (error: Exception) {
            ReleaseSafeLog.error(TAG, "Failed to query cleanup URIs for $collection", error)
            emptySet()
        }
    }

    private fun largeFileCollections(): List<CollectionDescriptor> = listOf(
        CollectionDescriptor(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaCategory.IMAGE),
        CollectionDescriptor(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaCategory.VIDEO),
        CollectionDescriptor(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaCategory.AUDIO)
    )

    private fun largeFileCollectionFor(category: MediaCategory): CollectionDescriptor? = when (category) {
        MediaCategory.DOWNLOAD ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                CollectionDescriptor(MediaStore.Downloads.EXTERNAL_CONTENT_URI, MediaCategory.DOWNLOAD)
            } else {
                null
            }
        else -> largeFileCollections().firstOrNull { it.category == category }
    }

    private fun List<CleanupAggregateSummary>.toCleanupSummary(): CleanupSummary = CleanupSummary(
        groups = map { it.group },
        totalCount = sumOf { it.group.itemCount },
        totalBytes = sumOf { it.group.totalBytes },
        maxFileSizeBytes = maxOfOrNull { it.maxFileSizeBytes } ?: 0L
    )

    private data class CollectionDescriptor(
        val uri: Uri,
        val category: MediaCategory
    )

    private data class CleanupAggregateSummary(
        val group: CleanupGroupSummary,
        val maxFileSizeBytes: Long
    )

    private companion object {
        private const val TAG = "MediaStoreScanner"
        private const val APK_SELECTION =
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.MIME_TYPE} = ?"
        private val APK_SELECTION_ARGS = arrayOf("%.apk", "application/vnd.android.package-archive")
    }
}
