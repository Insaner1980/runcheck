package com.runcheck.data.storage

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import com.runcheck.util.AppDispatchers
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Suppress("LargeClass") // A single MediaStore boundary shares the resolver and API-level guards.
@Singleton
class MediaStoreScanner
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val dispatchers: AppDispatchers,
    ) {
        private val resolver: ContentResolver = context.contentResolver
        private val externalFilesUri: Uri = MediaStore.Files.getContentUri("external")

        suspend fun getMediaBreakdown(): MediaBreakdown =
            withContext(dispatchers.io) {
                val coroutineContext = currentCoroutineContext()
                MediaBreakdown(
                    imagesBytes = queryMediaCategorySize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                    videosBytes = queryMediaCategorySize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI),
                    audioBytes = queryMediaCategorySize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI),
                    documentsBytes = queryDocumentsSize(coroutineContext),
                    downloadsBytes = queryDownloadsSize(),
                )
            }

        suspend fun getTrashInfo(): TrashInfo? =
            withContext(dispatchers.io) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext null

                var totalSize = 0L
                var itemCount = 0
                val coroutineContext = currentCoroutineContext()

                for (collection in MEDIA_COLLECTIONS) {
                    coroutineContext.ensureActive()
                    val result = queryTrashedCollection(collection, coroutineContext)
                    totalSize += result.first
                    itemCount += result.second
                }

                if (itemCount == 0) return@withContext null
                TrashInfo(totalBytes = totalSize, itemCount = itemCount)
            }

        private fun queryTrashedCollection(
            collection: Uri,
            coroutineContext: CoroutineContext,
        ): Pair<Long, Int> {
            val bundle =
                Bundle().apply {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.MediaColumns.SIZE),
                    )
                }
            var size = 0L
            var count = 0
            try {
                resolver
                    .query(collection, arrayOf(MediaStore.MediaColumns.SIZE), bundle, null)
                    ?.use { cursor ->
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        while (cursor.moveToNext()) {
                            coroutineContext.ensureActive()
                            size += cursor.getLong(sizeCol)
                            count++
                        }
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: SecurityException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to query trashed media for $collection", error)
            }
            return size to count
        }

        suspend fun getTrashedUris(): List<Uri> =
            withContext(dispatchers.io) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()

                val uris = mutableListOf<Uri>()
                val coroutineContext = currentCoroutineContext()

                for (collection in MEDIA_COLLECTIONS) {
                    coroutineContext.ensureActive()
                    uris += queryTrashedUris(collection, coroutineContext)
                }

                uris
            }

        private fun queryTrashedUris(
            collection: Uri,
            coroutineContext: CoroutineContext,
        ): List<Uri> =
            try {
                val bundle =
                    Bundle().apply {
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                    }
                buildList {
                    resolver
                        .query(collection, arrayOf(MediaStore.MediaColumns._ID), bundle, null)
                        ?.use { cursor ->
                            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                            while (cursor.moveToNext()) {
                                coroutineContext.ensureActive()
                                add(android.content.ContentUris.withAppendedId(collection, cursor.getLong(idCol)))
                            }
                        }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: SecurityException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to query trashed URIs for $collection", error)
                emptyList()
            }

        suspend fun findExistingUris(uriStrings: Collection<String>): Set<String> =
            withContext(dispatchers.io) {
                if (uriStrings.isEmpty()) return@withContext emptySet()
                val coroutineContext = currentCoroutineContext()

                buildSet {
                    uriStrings.forEach { uriString ->
                        coroutineContext.ensureActive()
                        resolver
                            .query(Uri.parse(uriString), arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                            ?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    add(uriString)
                                }
                            }
                    }
                }
            }

        suspend fun getCleanupSummary(query: CleanupScanQuery): CleanupSummary =
            withContext(dispatchers.io) {
                val coroutineContext = currentCoroutineContext()
                when (query.source) {
                    CleanupScanSource.LARGE_FILES -> {
                        getLargeFilesSummary(query.filterValue, coroutineContext)
                    }

                    CleanupScanSource.OLD_DOWNLOADS -> {
                        getOldDownloadsSummary(
                            olderThanMs = query.filterValue,
                            startedAtMillis = query.startedAtMillis,
                            coroutineContext = coroutineContext,
                        )
                    }

                    CleanupScanSource.APK_FILES -> {
                        getApkSummary(coroutineContext)
                    }
                }
            }

        suspend fun loadCleanupPage(
            query: CleanupScanQuery,
            category: MediaCategory,
            offset: Int,
            limit: Int,
        ): List<ScannedFile> =
            withContext(dispatchers.io) {
                val coroutineContext = currentCoroutineContext()
                when (query.source) {
                    CleanupScanSource.LARGE_FILES -> {
                        loadLargeFilesPage(category, query.filterValue, offset, limit, coroutineContext)
                    }

                    CleanupScanSource.OLD_DOWNLOADS -> {
                        loadOldDownloadsPage(
                            offset = offset,
                            limit = limit,
                            olderThanMs = query.filterValue,
                            startedAtMillis = query.startedAtMillis,
                            coroutineContext = coroutineContext,
                        )
                    }

                    CleanupScanSource.APK_FILES -> {
                        loadApkPage(offset, limit, coroutineContext)
                    }
                }
            }

        suspend fun getCleanupGroupFileSizes(
            query: CleanupScanQuery,
            category: MediaCategory,
        ): Map<String, Long> =
            withContext(dispatchers.io) {
                val coroutineContext = currentCoroutineContext()
                when (query.source) {
                    CleanupScanSource.LARGE_FILES -> {
                        queryFileSizesForLargeFileCategory(
                            category,
                            query.filterValue,
                            coroutineContext,
                        )
                    }

                    CleanupScanSource.OLD_DOWNLOADS -> {
                        queryOldDownloadFileSizes(
                            olderThanMs = query.filterValue,
                            startedAtMillis = query.startedAtMillis,
                            coroutineContext = coroutineContext,
                        )
                    }

                    CleanupScanSource.APK_FILES -> {
                        queryApkFileSizes(coroutineContext)
                    }
                }
            }

        internal fun registerCleanupInvalidation(
            query: CleanupScanQuery,
            category: MediaCategory,
            onChange: () -> Unit,
        ): () -> Unit {
            val collections = cleanupCollections(query, category).distinct()
            if (collections.isEmpty()) return {}

            val observer =
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        onChange()
                    }
                }
            collections.forEach { collection ->
                resolver.registerContentObserver(collection, true, observer)
            }
            return { resolver.unregisterContentObserver(observer) }
        }

        private fun queryMediaCategorySize(contentUri: Uri): Long {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return queryTotalSize(contentUri)
            val selection = mediaBreakdownSelection()
            return queryTotalSize(contentUri, selection.sql, selection.args)
        }

        private fun queryTotalSize(
            contentUri: Uri,
            selection: String? = null,
            selectionArgs: Array<String>? = null,
        ): Long =
            try {
                var total = 0L
                resolver
                    .query(
                        contentUri,
                        arrayOf(MediaStore.MediaColumns.SIZE),
                        selection,
                        selectionArgs,
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

        private fun queryDocumentsSize(coroutineContext: CoroutineContext): Long {
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

            for (mimePattern in docMimeTypes) {
                coroutineContext.ensureActive()
                val selection = mediaBreakdownSelection(mimePattern)
                try {
                    resolver
                        .query(
                            uri,
                            arrayOf(MediaStore.MediaColumns.SIZE),
                            selection.sql,
                            selection.args,
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

        private fun getLargeFilesSummary(
            thresholdBytes: Long,
            coroutineContext: CoroutineContext,
        ): CleanupSummary {
            val selection = largeFileSelection(thresholdBytes)
            val groups =
                buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        queryAggregateSummary(
                            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            category = MediaCategory.DOWNLOAD,
                            selection = selection.sql,
                            selectionArgs = selection.args,
                            coroutineContext = coroutineContext,
                        )?.let(::add)
                    }

                    largeFileCollections().forEach { descriptor ->
                        queryAggregateSummary(
                            collection = descriptor.uri,
                            category = descriptor.category,
                            selection = selection.sql,
                            selectionArgs = selection.args,
                            coroutineContext = coroutineContext,
                        )?.let(::add)
                    }
                }.sortedByDescending { it.group.totalBytes }

            return groups.toCleanupSummary()
        }

        private fun getOldDownloadsSummary(
            olderThanMs: Long,
            startedAtMillis: Long,
            coroutineContext: CoroutineContext,
        ): CleanupSummary {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return CleanupSummary(emptyList(), 0, 0L, 0L)
            val selection = oldDownloadSelection(olderThanMs, startedAtMillis)
            val group =
                queryAggregateSummary(
                    collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    category = MediaCategory.DOWNLOAD,
                    selection = selection.sql,
                    selectionArgs = selection.args,
                    coroutineContext = coroutineContext,
                )
            return listOfNotNull(group).toCleanupSummary()
        }

        private fun getApkSummary(coroutineContext: CoroutineContext): CleanupSummary {
            val groups =
                apkCollectionQueries().mapNotNull { query ->
                    queryAggregateSummary(
                        collection = query.collection,
                        category = MediaCategory.APK,
                        selection = query.selection,
                        selectionArgs = query.selectionArgs,
                        coroutineContext = coroutineContext,
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

        private fun queryAggregateSummary(
            collection: Uri,
            category: MediaCategory,
            selection: String,
            selectionArgs: Array<String>,
            coroutineContext: CoroutineContext,
        ): CleanupAggregateSummary? {
            val projection = arrayOf(MediaStore.MediaColumns.SIZE)
            return try {
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
            } catch (error: SecurityException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to aggregate cleanup summary for $category", error)
                null
            }
        }

        private fun loadLargeFilesPage(
            category: MediaCategory,
            thresholdBytes: Long,
            offset: Int,
            limit: Int,
            coroutineContext: CoroutineContext,
        ): List<ScannedFile> {
            val descriptor = largeFileCollectionFor(category) ?: return emptyList()
            val selection = largeFileSelection(thresholdBytes)
            return queryPagedFiles(
                query =
                    PagedFileQuery(
                        collection = descriptor.uri,
                        category = descriptor.category,
                        selection = selection.sql,
                        selectionArgs = selection.args,
                        sortOrder = FILE_PAGE_SORT_ORDER,
                    ),
                offset = offset,
                limit = limit,
                coroutineContext = coroutineContext,
            )
        }

        private fun loadOldDownloadsPage(
            offset: Int,
            limit: Int,
            olderThanMs: Long,
            startedAtMillis: Long,
            coroutineContext: CoroutineContext,
        ): List<ScannedFile> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
            val selection = oldDownloadSelection(olderThanMs, startedAtMillis)
            return queryPagedFiles(
                query =
                    PagedFileQuery(
                        collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        category = MediaCategory.DOWNLOAD,
                        selection = selection.sql,
                        selectionArgs = selection.args,
                        sortOrder = FILE_PAGE_SORT_ORDER,
                    ),
                offset = offset,
                limit = limit,
                coroutineContext = coroutineContext,
            )
        }

        private fun loadApkPage(
            offset: Int,
            limit: Int,
            coroutineContext: CoroutineContext,
        ): List<ScannedFile> {
            val seen = mutableSetOf<String>()
            val results = mutableListOf<ScannedFile>()
            for (apkQuery in apkCollectionQueries()) {
                results +=
                    queryPagedFiles(
                        query =
                            PagedFileQuery(
                                collection = apkQuery.collection,
                                category = MediaCategory.APK,
                                selection = apkQuery.selection,
                                selectionArgs = apkQuery.selectionArgs,
                                sortOrder = FILE_PAGE_SORT_ORDER,
                            ),
                        offset = 0,
                        limit = offset + limit,
                        coroutineContext = coroutineContext,
                    ).filter { seen.add(it.uri) }
            }
            return results
                .sortedWith(compareByDescending<ScannedFile> { it.sizeBytes }.thenBy { it.uri })
                .drop(offset)
                .take(limit)
        }

        private fun queryFileSizesForLargeFileCategory(
            category: MediaCategory,
            thresholdBytes: Long,
            coroutineContext: CoroutineContext,
        ): Map<String, Long> {
            val descriptor = largeFileCollectionFor(category) ?: return emptyMap()
            val selection = largeFileSelection(thresholdBytes)
            return queryFileSizes(
                collection = descriptor.uri,
                selection = selection.sql,
                selectionArgs = selection.args,
                coroutineContext = coroutineContext,
            )
        }

        private fun queryOldDownloadFileSizes(
            olderThanMs: Long,
            startedAtMillis: Long,
            coroutineContext: CoroutineContext,
        ): Map<String, Long> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyMap()
            val selection = oldDownloadSelection(olderThanMs, startedAtMillis)
            return queryFileSizes(
                collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                selection = selection.sql,
                selectionArgs = selection.args,
                coroutineContext = coroutineContext,
            )
        }

        private fun queryApkFileSizes(coroutineContext: CoroutineContext): Map<String, Long> {
            val fileSizes = mutableMapOf<String, Long>()
            for (query in apkCollectionQueries()) {
                fileSizes +=
                    queryFileSizes(
                        collection = query.collection,
                        selection = query.selection,
                        selectionArgs = query.selectionArgs,
                        coroutineContext = coroutineContext,
                    )
            }
            return fileSizes
        }

        private fun apkCollectionQueries(): List<ApkCollectionQuery> =
            apkCollections().map { collection ->
                val selection =
                    apkFileSelection(
                        excludeDownloads =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                collection == externalFilesUri,
                    )
                ApkCollectionQuery(collection, selection.sql, selection.args)
            }

        private fun queryPagedFiles(
            query: PagedFileQuery,
            offset: Int,
            limit: Int,
            coroutineContext: CoroutineContext,
        ): List<ScannedFile> =
            try {
                val cursor =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val queryArgs =
                            Bundle().apply {
                                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, query.selection)
                                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, query.selectionArgs)
                                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, query.sortOrder)
                                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                            }
                        resolver.query(query.collection, FILE_PROJECTION, queryArgs, null)
                    } else {
                        resolver.query(
                            query.collection,
                            FILE_PROJECTION,
                            query.selection,
                            query.selectionArgs,
                            "${query.sortOrder} LIMIT $limit OFFSET $offset",
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
                            coroutineContext.ensureActive()
                            add(
                                ScannedFile(
                                    uri =
                                        android.content.ContentUris
                                            .withAppendedId(
                                                query.collection,
                                                c.getLong(idCol),
                                            ).toString(),
                                    displayName =
                                        c.getString(
                                            nameCol,
                                        ) ?: context.getString(R.string.fallback_unknown),
                                    sizeBytes = c.getLong(sizeCol),
                                    mimeType = c.getString(mimeCol) ?: "",
                                    dateModified = c.getLong(dateCol) * 1000,
                                    category = query.category,
                                ),
                            )
                        }
                    }
                } ?: emptyList()
            } catch (error: CancellationException) {
                throw error
            } catch (error: SecurityException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to query paged cleanup items for ${query.category}", error)
                emptyList()
            }

        private fun queryFileSizes(
            collection: Uri,
            selection: String,
            selectionArgs: Array<String>,
            coroutineContext: CoroutineContext,
        ): Map<String, Long> =
            try {
                resolver
                    .query(
                        collection,
                        arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE),
                        selection,
                        selectionArgs,
                        null,
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                        buildMap {
                            while (cursor.moveToNext()) {
                                coroutineContext.ensureActive()
                                put(
                                    android.content.ContentUris
                                        .withAppendedId(collection, cursor.getLong(idCol))
                                        .toString(),
                                    cursor.getLong(sizeCol),
                                )
                            }
                        }
                    } ?: emptyMap()
            } catch (error: CancellationException) {
                throw error
            } catch (error: SecurityException) {
                throw error
            } catch (error: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to query cleanup file sizes for $collection", error)
                emptyMap()
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

        private fun cleanupCollections(
            query: CleanupScanQuery,
            category: MediaCategory,
        ): List<Uri> =
            when (query.source) {
                CleanupScanSource.LARGE_FILES -> {
                    listOfNotNull(largeFileCollectionFor(category)?.uri)
                }

                CleanupScanSource.OLD_DOWNLOADS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        listOf(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
                    } else {
                        emptyList()
                    }
                }

                CleanupScanSource.APK_FILES -> {
                    apkCollections()
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

        private class ApkCollectionQuery(
            val collection: Uri,
            val selection: String,
            val selectionArgs: Array<String>,
        )

        private class PagedFileQuery(
            val collection: Uri,
            val category: MediaCategory,
            val selection: String,
            val selectionArgs: Array<String>,
            val sortOrder: String,
        )

        private data class CleanupAggregateSummary(
            val group: CleanupGroupSummary,
            val maxFileSizeBytes: Long,
        )

        private companion object {
            private const val TAG = "MediaStoreScanner"
            private const val FILE_PAGE_SORT_ORDER =
                "${MediaStore.MediaColumns.SIZE} DESC, ${MediaStore.MediaColumns._ID} ASC"
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

internal data class MediaBreakdownSelection(
    val sql: String,
    val args: Array<String>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaBreakdownSelection) return false

        return sql == other.sql && args.contentEquals(other.args)
    }

    override fun hashCode(): Int = 31 * sql.hashCode() + args.contentHashCode()
}

internal fun mediaBreakdownSelection(
    mimePattern: String? = null,
    downloadDirectory: String = Environment.DIRECTORY_DOWNLOADS,
): MediaBreakdownSelection {
    val nonDownloadPath =
        "${MediaStore.MediaColumns.RELATIVE_PATH} IS NULL OR " +
            "${MediaStore.MediaColumns.RELATIVE_PATH} NOT LIKE ?"
    val downloadPathPattern = "$downloadDirectory/%"
    return if (mimePattern == null) {
        MediaBreakdownSelection(nonDownloadPath, arrayOf(downloadPathPattern))
    } else {
        MediaBreakdownSelection(
            "${MediaStore.MediaColumns.MIME_TYPE} LIKE ? AND ($nonDownloadPath)",
            arrayOf(mimePattern, downloadPathPattern),
        )
    }
}

internal fun apkFileSelection(excludeDownloads: Boolean): MediaBreakdownSelection {
    val baseSelection =
        "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.MIME_TYPE} = ?"
    val baseArgs = arrayOf("%.apk", "application/vnd.android.package-archive")
    if (!excludeDownloads) return MediaBreakdownSelection(baseSelection, baseArgs)

    val notDownload =
        "${MediaStore.DownloadColumns.IS_DOWNLOAD} IS NULL OR " +
            "${MediaStore.DownloadColumns.IS_DOWNLOAD} != 1"
    return MediaBreakdownSelection(
        "($baseSelection) AND ($notDownload)",
        baseArgs,
    )
}

internal fun oldDownloadSelection(
    olderThanMs: Long,
    startedAtMillis: Long,
): MediaBreakdownSelection =
    MediaBreakdownSelection(
        "${MediaStore.MediaColumns.DATE_MODIFIED} < ?",
        arrayOf(((startedAtMillis - olderThanMs) / 1000).toString()),
    )

internal fun largeFileSelection(thresholdBytes: Long): MediaBreakdownSelection =
    MediaBreakdownSelection(
        "${MediaStore.MediaColumns.SIZE} > ?",
        arrayOf(thresholdBytes.toString()),
    )
