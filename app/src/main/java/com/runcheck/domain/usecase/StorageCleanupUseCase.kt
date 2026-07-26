package com.runcheck.domain.usecase

import androidx.paging.PagingData
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupSummary
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.StorageCleanupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface StorageCleanupResult<out T> {
    data object Locked : StorageCleanupResult<Nothing>

    data class Available<T>(
        val value: T,
    ) : StorageCleanupResult<T>
}

class StorageCleanupUseCase
    @Inject
    constructor(
        private val getStorageStateUseCase: GetStorageStateUseCase,
        private val storageCleanupRepository: StorageCleanupRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        suspend fun getCurrentStorageState(): StorageCleanupResult<StorageState> =
            withConfirmedAccess { getStorageStateUseCase().first() }

        suspend fun getTrashedUris(): StorageCleanupResult<List<String>> =
            withConfirmedAccess(storageCleanupRepository::getTrashedUris)

        suspend fun getCleanupSummary(query: CleanupScanQuery): StorageCleanupResult<CleanupSummary> =
            withConfirmedAccess { storageCleanupRepository.getCleanupSummary(query) }

        suspend fun getCleanupItems(
            query: CleanupScanQuery,
            category: MediaCategory,
        ): StorageCleanupResult<Flow<PagingData<ScannedFile>>> =
            withConfirmedAccess { storageCleanupRepository.getCleanupItems(query, category) }

        suspend fun getCleanupGroupFileSizes(
            query: CleanupScanQuery,
            category: MediaCategory,
        ): StorageCleanupResult<Map<String, Long>> =
            withConfirmedAccess { storageCleanupRepository.getCleanupGroupFileSizes(query, category) }

        suspend fun findExistingUris(uriStrings: Collection<String>): StorageCleanupResult<Set<String>> =
            withConfirmedAccess { storageCleanupRepository.findExistingUris(uriStrings) }

        suspend fun deleteLegacy(uriStrings: Collection<String>): StorageCleanupResult<Set<String>> =
            withConfirmedAccess { storageCleanupRepository.deleteLegacy(uriStrings) }

        private suspend fun <T> withConfirmedAccess(block: suspend () -> T): StorageCleanupResult<T> {
            if (!proStatusProvider.isProStatusReady || !proStatusProvider.isPro()) {
                return StorageCleanupResult.Locked
            }
            return StorageCleanupResult.Available(block())
        }
    }
