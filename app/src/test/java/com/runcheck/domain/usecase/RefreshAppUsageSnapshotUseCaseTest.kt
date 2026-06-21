package com.runcheck.domain.usecase

import androidx.paging.PagingData
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.AppUsageListSummary
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshAppUsageSnapshotUseCaseTest {
    @Test
    fun `does not collect app usage when pro access is inactive`() =
        runTest {
            val repository = RecordingAppBatteryUsageRepository()
            val useCase = RefreshAppUsageSnapshotUseCase(repository, FakeAppUsageProStatusProvider(initial = false))

            useCase()

            assertEquals(0, repository.collectCalls)
        }

    @Test
    fun `collects app usage when pro access is active`() =
        runTest {
            val repository = RecordingAppBatteryUsageRepository()
            val useCase = RefreshAppUsageSnapshotUseCase(repository, FakeAppUsageProStatusProvider(initial = true))

            useCase()

            assertEquals(1, repository.collectCalls)
        }
}

private class RecordingAppBatteryUsageRepository : AppBatteryUsageRepository {
    var collectCalls = 0

    override fun getAggregatedUsageSince(since: Long): Flow<PagingData<AppBatteryUsage>> = emptyFlow()

    override fun getUsageSummarySince(since: Long): Flow<AppUsageListSummary> = emptyFlow()

    override suspend fun getUsageSinceSync(since: Long): List<AppBatteryUsage> = emptyList()

    override suspend fun collectUsageSnapshot() {
        collectCalls++
    }

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}

private class FakeAppUsageProStatusProvider(
    initial: Boolean,
) : ProStatusProvider {
    private val state = MutableStateFlow(initial)

    override val isProUser: Flow<Boolean> = state

    override fun isPro(): Boolean = state.value
}
