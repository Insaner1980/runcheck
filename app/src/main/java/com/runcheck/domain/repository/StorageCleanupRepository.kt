package com.runcheck.domain.repository

import androidx.paging.PagingData
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupSummary
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.MediaCategory
import kotlinx.coroutines.flow.Flow

interface StorageCleanupRepository {
    suspend fun getTrashedUris(): List<String>
    suspend fun getCleanupSummary(query: CleanupScanQuery): CleanupSummary
    fun getCleanupItems(
        query: CleanupScanQuery,
        category: MediaCategory
    ): Flow<PagingData<ScannedFile>>
    suspend fun getCleanupGroupUris(
        query: CleanupScanQuery,
        category: MediaCategory
    ): Set<String>
    suspend fun findExistingUris(uriStrings: Collection<String>): Set<String>
    suspend fun deleteLegacy(uriStrings: Collection<String>): Set<String>
}
