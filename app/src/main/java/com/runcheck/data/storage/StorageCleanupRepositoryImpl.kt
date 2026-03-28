package com.runcheck.data.storage

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupSummary
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.repository.StorageCleanupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageCleanupRepositoryImpl
    @Inject
    constructor(
        private val mediaStoreScanner: MediaStoreScanner,
        private val storageCleanupHelper: StorageCleanupHelper,
    ) : StorageCleanupRepository {
        override suspend fun getTrashedUris(): List<String> = mediaStoreScanner.getTrashedUris().map { it.toString() }

        override suspend fun getCleanupSummary(query: CleanupScanQuery): CleanupSummary =
            mediaStoreScanner.getCleanupSummary(query)

        override fun getCleanupItems(
            query: CleanupScanQuery,
            category: MediaCategory,
        ): Flow<PagingData<ScannedFile>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = PAGE_SIZE,
                        prefetchDistance = PAGE_SIZE / 2,
                        enablePlaceholders = false,
                    ),
                pagingSourceFactory = {
                    CleanupPagingSource { offset, limit ->
                        mediaStoreScanner.loadCleanupPage(query, category, offset, limit)
                    }
                },
            ).flow

        override suspend fun getCleanupGroupUris(
            query: CleanupScanQuery,
            category: MediaCategory,
        ): Set<String> = mediaStoreScanner.getCleanupGroupUris(query, category)

        override suspend fun findExistingUris(uriStrings: Collection<String>): Set<String> =
            mediaStoreScanner.findExistingUris(uriStrings)

        override suspend fun deleteLegacy(uriStrings: Collection<String>): Set<String> =
            storageCleanupHelper.deleteLegacy(uriStrings)

        private companion object {
            private const val PAGE_SIZE = 40
        }
    }
