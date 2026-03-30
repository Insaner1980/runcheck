package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.ThrottlingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProReactiveHistoryUseCasesTest {
    @Test
    fun `battery history re-queries when pro unlocks`() =
        runTest {
            val repo = HistoryBatteryRepository()
            val proStatusProvider = FakeProStatusProvider(initial = false)
            val useCase = GetBatteryHistoryUseCase(repo, proStatusProvider)

            val emissionsDeferred = async { useCase(HistoryPeriod.ALL).take(2).toList() }
            advanceUntilIdle()

            proStatusProvider.setPro(true)
            val emissions = emissionsDeferred.await()

            assertEquals(2, emissions.size)
            assertTrue(repo.requestedSince[0] >= System.currentTimeMillis() - HistoryPeriod.DAY.durationMs - 5_000L)
            assertEquals(0L, repo.requestedSince[1])
        }

    @Test
    fun `network history re-queries when pro unlocks`() =
        runTest {
            val repo = FakeNetworkRepository()
            val proStatusProvider = FakeProStatusProvider(initial = false)
            val useCase = GetNetworkHistoryUseCase(repo, proStatusProvider)

            val emissionsDeferred = async { useCase(HistoryPeriod.ALL).take(2).toList() }
            advanceUntilIdle()

            proStatusProvider.setPro(true)
            val emissions = emissionsDeferred.await()

            assertEquals(2, emissions.size)
            assertTrue(repo.requestedSince[0] >= System.currentTimeMillis() - HistoryPeriod.DAY.durationMs - 5_000L)
            assertEquals(0L, repo.requestedSince[1])
        }

    @Test
    fun `throttling history updates when pro unlocks`() =
        runTest {
            val repo = FakeThrottlingRepository()
            val proStatusProvider = FakeProStatusProvider(initial = false)
            val useCase = GetThrottlingHistoryUseCase(repo, proStatusProvider)

            val emissionsDeferred = async { useCase().take(2).toList() }
            advanceUntilIdle()

            proStatusProvider.setPro(true)
            val emissions = emissionsDeferred.await()

            assertEquals(
                listOf(
                    emptyList<ThrottlingEvent>(),
                    listOf(ThrottlingEvent(1L, 1_000L, "SEVERE", 43f, null, null, null)),
                ),
                emissions,
            )
        }
}

private class FakeProStatusProvider(
    initial: Boolean,
) : ProStatusProvider {
    private val state = MutableStateFlow(initial)

    override val isProUser: Flow<Boolean> = state

    override fun isPro(): Boolean = state.value

    fun setPro(isPro: Boolean) {
        state.value = isPro
    }
}

private class HistoryBatteryRepository : BatteryRepository {
    val requestedSince = mutableListOf<Long>()

    override fun getBatteryState() = emptyFlow<com.runcheck.domain.model.BatteryState>()

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<BatteryReading>> {
        requestedSince += since
        return flowOf(emptyList())
    }

    override suspend fun saveReading(state: com.runcheck.domain.model.BatteryState) = Unit

    override suspend fun getAllReadings(): List<BatteryReading> = emptyList()

    override suspend fun getReadingsSinceSync(since: Long): List<BatteryReading> = emptyList()

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit

    override suspend fun getLastChargingTimestamp(): Long? = 123L

    override suspend fun getLatestReadingTimestamp(): Long? = null
}

private class FakeNetworkRepository : NetworkRepository {
    val requestedSince = mutableListOf<Long>()

    override fun getNetworkState() = emptyFlow<com.runcheck.domain.model.NetworkState>()

    override suspend fun measureLatency(): Int? = null

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<NetworkReading>> {
        requestedSince += since
        return flowOf(emptyList())
    }

    override suspend fun saveReading(state: com.runcheck.domain.model.NetworkState) = Unit

    override suspend fun getAllReadings(): List<NetworkReading> = emptyList()

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}

private class FakeThrottlingRepository : ThrottlingRepository {
    override fun getRecentEvents(limit: Int): Flow<List<ThrottlingEvent>> =
        flowOf(
            listOf(
                ThrottlingEvent(
                    id = 1L,
                    timestamp = 1_000L,
                    thermalStatus = "SEVERE",
                    batteryTempC = 43f,
                    cpuTempC = null,
                    foregroundApp = null,
                    durationMs = null,
                ),
            ),
        )

    override suspend fun insert(event: ThrottlingEvent): Long = 0L

    override suspend fun updateSnapshot(
        id: Long,
        thermalStatus: String,
        batteryTempC: Float,
        cpuTempC: Float?,
        foregroundApp: String?,
    ) = Unit

    override suspend fun updateDuration(
        id: Long,
        durationMs: Long,
    ) = Unit

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
