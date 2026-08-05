package com.runcheck.domain.usecase

import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class SetMonitoringIntervalUseCaseTest {
    @Test
    fun `concurrent changes persist and schedule each interval as one operation`() =
        runTest {
            val events = mutableListOf<String>()
            val preferencesRepository = mockk<UserPreferencesRepository>()
            val scheduler = mockk<MonitoringScheduler>()
            coEvery { preferencesRepository.setMonitoringInterval(any()) } coAnswers {
                events += "write:${firstArg<MonitoringInterval>()}"
                yield()
            }
            every { scheduler.schedule(any()) } answers {
                events += "schedule:${firstArg<MonitoringInterval>()}"
            }
            val useCase = SetMonitoringIntervalUseCase(preferencesRepository, scheduler)

            val first = launch { useCase(MonitoringInterval.FIFTEEN) }
            val second = launch { useCase(MonitoringInterval.SIXTY) }
            joinAll(first, second)

            assertEquals(
                listOf(
                    "write:FIFTEEN",
                    "schedule:FIFTEEN",
                    "write:SIXTY",
                    "schedule:SIXTY",
                ),
                events,
            )
        }
}
