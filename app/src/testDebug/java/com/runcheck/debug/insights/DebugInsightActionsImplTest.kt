package com.runcheck.debug.insights

import com.runcheck.domain.repository.InsightRepository
import com.runcheck.domain.usecase.MonitoringDataCoordinator
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebugInsightActionsImplTest {
    @Test
    fun `clear waits for admitted generation before deleting insights`() =
        runTest {
            val repository = mockk<InsightRepository>(relaxed = true)
            val coordinator = MonitoringDataCoordinator(mockk())
            val actions =
                DebugInsightActionsImpl(
                    insightDao = mockk(),
                    insightRepository = repository,
                    insightEngine = mockk(),
                    insightTestDataSeeder = mockk(),
                    monitoringDataCoordinator = coordinator,
                )

            coordinator.withInsightGeneration {
                launch { actions.clearInsights() }
                runCurrent()
                coVerify(exactly = 0) { repository.clearAll() }
            }
            runCurrent()
            coVerify(exactly = 1) { repository.clearAll() }
        }
}
