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
class MediaStoreScanner
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val resolver: ContentResolver = context.contentResolver
        private val externalFilesUri: Uri = MediaStore.Files.getContentUri("external")

        suspend fun getMediaBreakdown(): MediaBreakdown =
            withContext(Dispatchers.IO) {
                MediaBreakdown(
                    imagesBytes = queryTotalSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                    videosBytes = queryTotalSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
                    audioBytes = queryTotalSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
                    documentsBytes = queryDocumentsSize(),
                    downloadsBytes = queryDownloadsSize(),
                )
            }

        suspend fun getTrashInfo(): TrashInfo? =
            withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext null

                var totalSize = 0L
                var itemCount = 0
                val coroutineContext = currentCoroutineContext()

                for (collection in MEDIA_COLLECTIONS) {
                    coroutineContext.ensureActive()
                    val bundle =
                        Bundle().apply {
                            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                            putStringArray(
                                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                                arrayOf(MediaStore.MediaColumns.SIZE),
                            )
                        }
                    try {
                        resolver
                            .query(collection, arrayOf(MediaStore.MediaColumns.SIZE), bundle, null)
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

        suspend fun getTrashedUris(): List<Uri> =
            withContext(Dispatchers.IO) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()

                val uris = mutableListOf<Uri>()
                val coroutineContext = currentCoroutineContext()

                for (collection in MEDIA_COLLECTIONS) {
                    coroutineContext.ensureActive()
                    val bundle =
                        Bundle().apply {
                            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                        }
                    try {
                        resolver
                            .query(collection, arrayOf(MediaStore.MediaColumns._ID), bundle, null)
                            ?.use { cursor ->
                                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                                while (cursor.moveToNext()) {
                                    coroutineContext.ensureActive()
                                    uris.add(
                                        android.content.ContentUris.withAppendedId(collection, cursor.getLong(idCol)),
                                    )
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

        suspend fun findExistingUris(uriStrings: Collection<String>): Set<String> =
            withContext(Dispatchers.IO) {
                if (uriStrings.isEmpty()) return@withContext emptySet()
                val coroutineContext = currentCoroutineContext()

                buildSet {
                    uriStrings.forEach { uriString ->
                        coroutineContext.ensureActive()
                        try {
                            resolver
                                .query(Uri.parse(uriString), arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                                ?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        add(uriString)
                                    }
                                }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            // Treat inaccessible or missing rows as deleted to avoid overstating failures.
                        }
                    }
                }
            }

        suspend fun getCleanupSummary(query: CleanupScanQuery): CleanupSummary =
            withContext(Dispatchers.IO) {
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
            limit: Int,
        ): List<ScannedFile> =
            withContext(Dispatchers.IO) {
                when (query.source) {
                    CleanupScanSource.LARGE_FILES -> loadLargeFilesPage(category, query.filterValue, offset, limit)
                    CleanupScanSource.OLD_DOWNLOADS -> loadOldDownloadsPage(offset, limit, query.filterValue)
                    CleanupScanSource.APK_FILES -> loadApkPage(offset, limit)
                }
            }

        suspend fun getCleanupGroupUris(
            query: CleanupScanQuery,
            category: MediaCategory,
        ): Set<String> =
            withContext(Dispatchers.IO) {
                when (query.source) {
                    CleanupScanSource.LARGE_FILES -> queryUrisForLargeFileCategory(category, query.filterValue)
                    CleanupScanSource.OLD_DOWNLOADS -> queryOldDownloadUris(query.filterValue)
                    CleanupScanSource.APK_FILES -> queryApkUris()
                }
            }

        private fun queryTotalSize(contentUri: Uri): Long =
            try {
                var total = 0L
                resolver
                    .query(
                        contentUri,
                        arrayOf(MediaStore.MediaColumns.SIZE),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        while (cursor.moveToNext()) {
                            total += cursor.getLong(sizeCol)
                        }
                    }
                total
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to query total size for $contentUri", error)
                0L
            }

        private suspend fun queryDocumentsSize(): Long {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0L

            val docMimeTypes =
                listOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats%",
                    "text/%",
                    "application/vnd.oasis.opendocument%",
                )

            var total = 0L
            val uri = MediaStore.Files.getContentUri("external")
            val coroutineContext = currentCoroutineContext()

            for (mimePattern in docMimeTypes) {
                coroutineContext.ensureActive()
                try {
                    resolver
                        .query(
                            uri,
                            arrayOf(MediaStore.MediaColumns.SIZE),
                            "${MediaStore.MediaColumns.MIME_TYPE} LIKE ?",
                            arrayOf(mimePattern),
                            null,
                        )?.use { cursor ->
                            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                            while (cursor.moveToNext()) {
                                coroutineContext.ensureActive()
                                total += cursor.getLong(sizeCol)
                            }
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

        private suspend fun getLargeFilesSummary(thresholdBytes: Long): CleanupSummary {
            val groups =
                buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        queryAggregateSummary(
                            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            category = MediaCategory.DOWNLOAD,
                            selection = "${MediaStore.MediaColumns.SIZE} > ?",
                            selectionArgs = arrayOf(thresholdBytes.toString()),
                        )?.let(::add)
                    }

                    largeFileCollections().forEach { descriptor ->
                        queryAggregateSummary(
                            collection = descriptor.uri,
                            category = descriptor.category,
                            selection = "${MediaStore.MediaColumns.SIZE} > ?",
                            selectionArgs = arrayOf(thresholdBytes.toString()),
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
            val group =
                queryAggregateSummary(
                    collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    category = MediaCategory.DOWNLOAD,
                    selection = selection,
                    selectionArgs = selectionArgs,
                )
            return listOfNotNull(group).toCleanupSummary()
        }

        private suspend fun getApkSummary(): CleanupSummary {
            val collections = apkCollections()
            val groups =
                collections.mapNotNull { uri ->
                    queryAggregateSummary(
                        collection = uri,
                        category = MediaCategory.APK,
                        selection = APK_SELECTION,
                        selectionArgs = APK_SELECTION_ARGS,
                    )
                }
            return groups.toCleanupSummary()
        }

        private fun apkCollections(): List<Uri> =
            buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
                }
                add(externalFilesUri)
            }

        private suspend fun queryAggregateSummary(
            collection: Uri,
            category: MediaCategory,
            selection: String,
            selectionArgs: Array<String>,
        ): CleanupAggregateSummary? {
            val projection = arrayOf(MediaStore.MediaColumns.SIZE)
            return try {
                val coroutineContext = currentCoroutineContext()
                resolver
                    .query(collection, projection, selection, selectionArgs, null)
                    ?.use { cursor ->
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        var count = 0
                        var totalBytes = 0L
                        var maxSize = 0L
                        while (cursor.moveToNext()) {
                            coroutineContext.ensureActive()
                            count++
                            val size = cursor.getLong(sizeCol)
                            totalBytes += size
                            if (size > maxSize) maxSize = size
                        }
                        if (count == 0) return@use null
                        CleanupAggregateSummary(
                            group =
                                CleanupGroupSummary(
                                    category = category,
                                    itemCount = count,
                                    totalBytes = totalBytes,
                                ),
                            maxFileSizeBytes = maxSize,
                        )
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to aggregate cleanup summary for $category", error)
                null
            }
        }

        private suspend fun loadLargeFilesPage(
            category: MediaCategory,
            thresholdBytes: Long,
            offset: Int,
            limit: Int,
        ): List<ScannedFile> {
            val descriptor = largeFileCollectionFor(category) ?: return emptyList()
            return queryPagedFiles(
                collection = descriptor.uri,
                category = descriptor.category,
                selection = "${MediaStore.MediaColumns.SIZE} > ?",
                selectionArgs = arrayOf(thresholdBytes.toString()),
                sortOrder = "${MediaStore.MediaColumns.SIZE} DESC",
                offset = offset,
                limit = limit,
            )
        }

        private suspend fun loadOldDownloadsPage(
            offset: Int,
            limit: Int,
            olderThanMs: Long,
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
                limit = limit,
            )
        }

        private suspend fun loadApkPage(
            offset: Int,
            limit: Int,
        ): List<ScannedFile> {
            val seen = mutableSetOf<String>()
            val results = mutableListOf<ScannedFile>()
            for (uri in apkCollections()) {
                results +=
                    queryPagedFiles(
                        collection = uri,
                        category = MediaCategory.APK,
                        selection = APK_SELECTION,
                        selectionArgs = APK_SELECTION_ARGS,
                        sortOrder = "${MediaStore.MediaColumns.SIZE} DESC",
                        offset = 0,
                        limit = offset + limit,
                    ).filter { seen.add(it.uri) }
            }
            return results
                .sortedByDescending { it.sizeBytes }
                .drop(offset)
                .take(limit)
        }

        private suspend fun queryUrisForLargeFileCategory(
            category: MediaCategory,
            thresholdBytes: Long,
        ): Set<String> {
            val descriptor = largeFileCollectionFor(category) ?: return emptySet()
            return queryUris(
                collection = descriptor.uri,
                selection = "${MediaStore.MediaColumns.SIZE} > ?",
                selectionArgs = arrayOf(thresholdBytes.toString()),
            )
        }

        private suspend fun queryOldDownloadUris(olderThanMs: Long): Set<String> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptySet()
            val cutoff = (System.currentTimeMillis() - olderThanMs) / 1000
            return queryUris(
                collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                selection = "${MediaStore.MediaColumns.DATE_MODIFIED} < ?",
                selectionArgs = arrayOf(cutoff.toString()),
            )
        }

        private suspend fun queryApkUris(): Set<String> {
            val uris = mutableSetOf<String>()
            for (uri in apkCollections()) {
                uris +=
                    queryUris(
                        collection = uri,
                        selection = APK_SELECTION,
                        selectionArgs = APK_SELECTION_ARGS,
                    )
            }
            return uris
        }

        private suspend fun queryPagedFiles(
            collection: Uri,
            category: MediaCategory,
            selection: String,
            selectionArgs: Array<String>,
            sortOrder: String,
            offset: Int,
            limit: Int,
        ): List<ScannedFile> =
            try {
                val cursor =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val queryArgs =
                            Bundle().apply {
                                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                            }
                        resolver.query(collection, FILE_PROJECTION, queryArgs, null)
                    } else {
                        resolver.query(
                            collection,
                            FILE_PROJECTION,
                            selection,
                            selectionArgs,
                            "$sortOrder LIMIT $limit OFFSET $offset",
                        )
                    }
                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val mimeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    buildList {
                        while (c.moveToNext()) {
                            add(
                                ScannedFile(
                                    uri =
                                        android.content.ContentUris
                                            .withAppendedId(
                                                collection,
                                                c.getLong(idCol),
                                            ).toString(),
                                    displayName =
                                        c.getString(
                                            nameCol,
                                        ) ?: context.getString(R.string.fallback_unknown),
                                    sizeBytes = c.getLong(sizeCol),
                                    mimeType = c.getString(mimeCol) ?: "",
                                    dateModified = c.getLong(dateCol) * 1000,
                                    category = category,
                                ),
                            )
                        }
                    }
                } ?: emptyList()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to query paged cleanup items for $category", error)
                emptyList()
            }

        private suspend fun queryUris(
            collection: Uri,
            selection: String,
            selectionArgs: Array<String>,
        ): Set<String> =
            withContext(Dispatchers.IO) {
                try {
                    resolver
                        .query(
                            collection,
                            arrayOf(MediaStore.MediaColumns._ID),
                            selection,
                            selectionArgs,
                            null,
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            buildSet {
                                while (cursor.moveToNext()) {
                                    add(
                                        android.content.ContentUris
                                            .withAppendedId(collection, cursor.getLong(idCol))
                                            .toString(),
                                    )
                                }
                            }
                        } ?: emptySet()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    ReleaseSafeLog.error(TAG, "Failed to query cleanup URIs for $collection", error)
                    emptySet()
                }
            }

        private fun largeFileCollections(): List<CollectionDescriptor> =
            listOf(
                CollectionDescriptor(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaCategory.IMAGE),
                CollectionDescriptor(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaCategory.VIDEO),
                CollectionDescriptor(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaCategory.AUDIO),
            )

        private fun largeFileCollectionFor(category: MediaCategory): CollectionDescriptor? =
            when (category) {
                MediaCategory.DOWNLOAD -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        CollectionDescriptor(MediaStore.Downloads.EXTERNAL_CONTENT_URI, MediaCategory.DOWNLOAD)
                    } else {
                        null
                    }
                }

                else -> {
                    largeFileCollections().firstOrNull { it.category == category }
                }
            }

        private fun List<CleanupAggregateSummary>.toCleanupSummary(): CleanupSummary =
            CleanupSummary(
                groups = map { it.group },
                totalCount = sumOf { it.group.itemCount },
                totalBytes = sumOf { it.group.totalBytes },
                maxFileSizeBytes = maxOfOrNull { it.maxFileSizeBytes } ?: 0L,
            )

        private data class CollectionDescriptor(
            val uri: Uri,
            val category: MediaCategory,
        )

        private data class CleanupAggregateSummary(
            val group: CleanupGroupSummary,
            val maxFileSizeBytes: Long,
        )

        private companion object {
            private const val TAG = "MediaStoreScanner"
            private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
            private const val APK_SELECTION =
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.MIME_TYPE} = ?"
            private val APK_SELECTION_ARGS = arrayOf("%.apk", APK_MIME_TYPE)
            private val MEDIA_COLLECTIONS =
                listOf(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                )
            private val FILE_PROJECTION =
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                )
        }
    }
