package com.runcheck.domain.usecase

import com.runcheck.domain.model.UnusedAppCandidate
import com.runcheck.domain.model.UnusedAppsPeriod
import com.runcheck.domain.model.UnusedAppsResult
import com.runcheck.domain.model.UsageAccess
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UnusedAppsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GetUnusedAppsUseCaseTest {
    @Test
    fun `free access never queries installed apps`() =
        runTest {
            val repository = FakeUnusedAppsRepository()
            val useCase = GetUnusedAppsUseCase(repository, FakeUnusedAppsProStatus(false))

            val result = useCase(UnusedAppsPeriod.DAYS_30, Instant.EPOCH, forceRefresh = false)

            assertTrue(result is UnusedAppsQueryResult.Locked)
            assertEquals(0, repository.queryCount)
        }

    @Test
    fun `period exposes exact 30 60 and 90 day windows`() {
        assertEquals(30, UnusedAppsPeriod.DAYS_30.days)
        assertEquals(60, UnusedAppsPeriod.DAYS_60.days)
        assertEquals(90, UnusedAppsPeriod.DAYS_90.days)
    }

    @Test
    fun `pro query preserves missing usage evidence wording state`() =
        runTest {
            val repository =
                FakeUnusedAppsRepository(
                    UnusedAppsResult(
                        usageAccess = UsageAccess.GRANTED,
                        period = UnusedAppsPeriod.DAYS_60,
                        observedAt = Instant.parse("2026-02-10T00:00:00Z"),
                        candidates =
                            listOf(
                                UnusedAppCandidate(
                                    packageName = "example.app",
                                    appLabel = null,
                                    firstInstallTime = Instant.EPOCH,
                                    lastRecordedUse = null,
                                    storageBytes = null,
                                ),
                            ),
                    ),
                )
            val result =
                GetUnusedAppsUseCase(repository, FakeUnusedAppsProStatus(true))(
                    UnusedAppsPeriod.DAYS_60,
                    Instant.parse("2026-02-10T00:00:00Z"),
                    forceRefresh = true,
                ) as UnusedAppsQueryResult.Available

            assertEquals(1, result.result.candidates.size)
            assertEquals(
                null,
                result.result.candidates
                    .single()
                    .lastRecordedUse,
            )
            assertTrue(repository.lastForceRefresh)
        }
}

private class FakeUnusedAppsRepository(
    private val result: UnusedAppsResult =
        UnusedAppsResult(
            usageAccess = UsageAccess.REQUIRED,
            period = UnusedAppsPeriod.DAYS_30,
            observedAt = Instant.EPOCH,
        ),
) : UnusedAppsRepository {
    var queryCount = 0
    var lastForceRefresh = false

    override suspend fun getUnusedApps(
        period: UnusedAppsPeriod,
        observedAt: Instant,
        forceRefresh: Boolean,
    ): UnusedAppsResult {
        queryCount++
        lastForceRefresh = forceRefresh
        return result
    }
}

private class FakeUnusedAppsProStatus(
    private val value: Boolean,
) : ProStatusProvider {
    override val isProUser: Flow<Boolean> = flowOf(value)
    override val isProStatusReady: Boolean = true

    override fun isPro(): Boolean = value
}
