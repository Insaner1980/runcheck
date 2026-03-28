package com.runcheck.domain.usecase

import androidx.paging.PagingData
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupSummary
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.StorageCleanupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class StorageCleanupUseCase
    @Inject
    constructor(
        private val getStorageStateUseCase: GetStorageStateUseCase,
        private val storageCleanupRepository: StorageCleanupRepository,
    ) {
        suspend fun getCurrentStorageState(): StorageState = getStorageStateUseCase().first()

        suspend fun getTrashedUris(): List<String> = storageCleanupRepository.getTrashedUris()

        suspend fun getCleanupSummary(query: CleanupScanQuery): CleanupSummary =
            storageCleanupRepository.getCleanupSummary(query)

        fun getCleanupItems(
            query: CleanupScanQuery,
            category: MediaCategory,
        ): Flow<PagingData<ScannedFile>> = storageCleanupRepository.getCleanupItems(query, category)

        suspend fun getCleanupGroupUris(
            query: CleanupScanQuery,
            category: MediaCategory,
        ): Set<String> = storageCleanupRepository.getCleanupGroupUris(query, category)

        suspend fun findExistingUris(uriStrings: Collection<String>): Set<String> =
            storageCleanupRepository.findExistingUris(uriStrings)

        suspend fun deleteLegacy(uriStrings: Collection<String>): Set<String> =
            storageCleanupRepository.deleteLegacy(uriStrings)
    }
