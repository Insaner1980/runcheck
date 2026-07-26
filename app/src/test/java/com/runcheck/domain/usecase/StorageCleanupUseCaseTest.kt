package com.runcheck.domain.usecase

import androidx.paging.PagingData
import com.runcheck.domain.model.CleanupGroupSummary
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupScanSource
import com.runcheck.domain.model.CleanupSummary
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.StorageCleanupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class StorageCleanupUseCaseTest {
    private val query =
        CleanupScanQuery(
            source = CleanupScanSource.LARGE_FILES,
            filterValue = 1L,
            startedAtMillis = 100L,
        )
    private val category = MediaCategory.VIDEO
    private val uriStrings = listOf("content://media/1")

    @Test
    fun `unresolved Pro readiness locks every cleanup operation without repository access`() =
        runTest {
            val fixture = fixture(isReady = false, isPro = true)

            fixture.assertEveryOperationLocked()
            fixture.verifyNoRepositoriesCalled()
            verify(exactly = 0) { fixture.proStatusProvider.isPro() }
        }

    @Test
    fun `free or expired access locks every cleanup operation without repository access`() =
        runTest {
            val fixture = fixture(isReady = true, isPro = false)

            fixture.assertEveryOperationLocked()
            fixture.verifyNoRepositoriesCalled()
        }

    @Test
    fun `trial and purchased access both delegate every cleanup operation`() =
        runTest {
            listOf("trial", "purchased").forEach { accessKind ->
                val fixture = fixture(isReady = true, isPro = true)

                fixture.assertEveryOperationAvailable(accessKind)
                fixture.verifyEveryRepositoryCalled()
            }
        }

    private fun fixture(
        isReady: Boolean,
        isPro: Boolean,
    ): Fixture {
        val getStorageStateUseCase: GetStorageStateUseCase = mockk()
        val repository: StorageCleanupRepository = mockk()
        val proStatusProvider: ProStatusProvider = mockk()
        val storageState =
            StorageState(
                totalBytes = 1_000L,
                availableBytes = 400L,
                usedBytes = 600L,
                usagePercent = 60f,
            )
        val summary =
            CleanupSummary(
                groups =
                    listOf(
                        CleanupGroupSummary(
                            category = category,
                            itemCount = 1,
                            totalBytes = 200L,
                        ),
                    ),
                totalCount = 1,
                totalBytes = 200L,
                maxFileSizeBytes = 200L,
            )
        val items: Flow<PagingData<ScannedFile>> = flowOf(PagingData.empty())

        every { proStatusProvider.isProStatusReady } returns isReady
        every { proStatusProvider.isPro() } returns isPro
        every { getStorageStateUseCase() } returns flowOf(storageState)
        coEvery { repository.getTrashedUris() } returns uriStrings
        coEvery { repository.getCleanupSummary(query) } returns summary
        every { repository.getCleanupItems(query, category) } returns items
        coEvery { repository.getCleanupGroupFileSizes(query, category) } returns
            mapOf(uriStrings.single() to 200L)
        coEvery { repository.findExistingUris(uriStrings) } returns uriStrings.toSet()
        coEvery { repository.deleteLegacy(uriStrings) } returns uriStrings.toSet()

        return Fixture(
            useCase =
                StorageCleanupUseCase(
                    getStorageStateUseCase = getStorageStateUseCase,
                    storageCleanupRepository = repository,
                    proStatusProvider = proStatusProvider,
                ),
            getStorageStateUseCase = getStorageStateUseCase,
            repository = repository,
            proStatusProvider = proStatusProvider,
            storageState = storageState,
            summary = summary,
            items = items,
        )
    }

    private inner class Fixture(
        val useCase: StorageCleanupUseCase,
        val getStorageStateUseCase: GetStorageStateUseCase,
        val repository: StorageCleanupRepository,
        val proStatusProvider: ProStatusProvider,
        val storageState: StorageState,
        val summary: CleanupSummary,
        val items: Flow<PagingData<ScannedFile>>,
    ) {
        suspend fun assertEveryOperationLocked() {
            assertSame(StorageCleanupResult.Locked, useCase.getCurrentStorageState())
            assertSame(StorageCleanupResult.Locked, useCase.getTrashedUris())
            assertSame(StorageCleanupResult.Locked, useCase.getCleanupSummary(query))
            assertSame(StorageCleanupResult.Locked, useCase.getCleanupItems(query, category))
            assertSame(StorageCleanupResult.Locked, useCase.getCleanupGroupFileSizes(query, category))
            assertSame(StorageCleanupResult.Locked, useCase.findExistingUris(uriStrings))
            assertSame(StorageCleanupResult.Locked, useCase.deleteLegacy(uriStrings))
        }

        suspend fun assertEveryOperationAvailable(accessKind: String) {
            assertEquals(storageState, useCase.getCurrentStorageState().availableValue(accessKind))
            assertEquals(uriStrings, useCase.getTrashedUris().availableValue(accessKind))
            assertEquals(summary, useCase.getCleanupSummary(query).availableValue(accessKind))
            assertSame(items, useCase.getCleanupItems(query, category).availableValue(accessKind))
            assertEquals(
                mapOf(uriStrings.single() to 200L),
                useCase.getCleanupGroupFileSizes(query, category).availableValue(accessKind),
            )
            assertEquals(uriStrings.toSet(), useCase.findExistingUris(uriStrings).availableValue(accessKind))
            assertEquals(uriStrings.toSet(), useCase.deleteLegacy(uriStrings).availableValue(accessKind))
        }

        fun verifyNoRepositoriesCalled() {
            verify(exactly = 0) { getStorageStateUseCase() }
            coVerify(exactly = 0) { repository.getTrashedUris() }
            coVerify(exactly = 0) { repository.getCleanupSummary(any()) }
            verify(exactly = 0) { repository.getCleanupItems(any(), any()) }
            coVerify(exactly = 0) { repository.getCleanupGroupFileSizes(any(), any()) }
            coVerify(exactly = 0) { repository.findExistingUris(any()) }
            coVerify(exactly = 0) { repository.deleteLegacy(any()) }
        }

        fun verifyEveryRepositoryCalled() {
            verify(exactly = 1) { getStorageStateUseCase() }
            coVerify(exactly = 1) { repository.getTrashedUris() }
            coVerify(exactly = 1) { repository.getCleanupSummary(query) }
            verify(exactly = 1) { repository.getCleanupItems(query, category) }
            coVerify(exactly = 1) { repository.getCleanupGroupFileSizes(query, category) }
            coVerify(exactly = 1) { repository.findExistingUris(uriStrings) }
            coVerify(exactly = 1) { repository.deleteLegacy(uriStrings) }
        }
    }
}

private fun <T> StorageCleanupResult<T>.availableValue(accessKind: String): T =
    when (this) {
        is StorageCleanupResult.Available -> value
        StorageCleanupResult.Locked -> error("$accessKind access should be available")
    }
