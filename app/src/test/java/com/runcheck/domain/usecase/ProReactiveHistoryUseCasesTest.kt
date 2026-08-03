package com.runcheck.domain.usecase

import com.runcheck.domain.insights.rules.TestThrottlingRepository
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.ThrottlingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
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

            val emissions = collectAfterProUnlock(proStatusProvider, useCase(HistoryPeriod.ALL))

            assertAllPeriodRequery(emissions, repo.requestedSince)
        }

    @Test
    fun `network history re-queries when pro unlocks`() =
        runTest {
            val repo = FakeNetworkRepository()
            val proStatusProvider = FakeProStatusProvider(initial = false)
            val useCase = GetNetworkHistoryUseCase(repo, proStatusProvider)

            val emissions = collectAfterProUnlock(proStatusProvider, useCase(HistoryPeriod.ALL))

            assertAllPeriodRequery(emissions, repo.requestedSince)
        }

    @Test
    fun `throttling history updates when pro unlocks`() =
        runTest {
            val repo = TestThrottlingRepository(listOf(testThrottlingEvent))
            val proStatusProvider = FakeProStatusProvider(initial = false)
            val useCase = GetThrottlingHistoryUseCase(repo, proStatusProvider)

            val emissions = collectAfterProUnlock(proStatusProvider, useCase())

            assertEquals(
                listOf(
                    emptyList<ThrottlingEvent>(),
                    listOf(ThrottlingEvent(1L, 1_000L, "SEVERE", 43f, null, null, null)),
                ),
                emissions,
            )
        }

    @Test
    fun `storage history is empty until pro unlocks`() =
        runTest {
            val repo = FakeStorageRepository()
            val proStatusProvider = FakeProStatusProvider(initial = false)
            val useCase = GetStorageHistoryUseCase(repo, proStatusProvider)

            val emissions = collectAfterProUnlock(proStatusProvider, useCase(HistoryPeriod.ALL))

            assertEquals(listOf(emptyList<StorageReading>(), listOf(testStorageReading)), emissions)
            assertEquals(listOf(0L), repo.requestedSince)
        }

    @Test
    fun `thermal history is empty until pro unlocks`() =
        runTest {
            val repo = FakeThermalRepository()
            val proStatusProvider = FakeProStatusProvider(initial = false)
            val useCase = GetThermalHistoryUseCase(repo, proStatusProvider)

            val emissions = collectAfterProUnlock(proStatusProvider, useCase(HistoryPeriod.ALL))

            assertEquals(listOf(emptyList<ThermalReading>(), listOf(testThermalReading)), emissions)
            assertEquals(listOf(0L), repo.requestedSince)
        }
}

private fun <T> assertAllPeriodRequery(
    emissions: List<List<T>>,
    requestedSince: List<Long>,
) {
    assertEquals(2, emissions.size)
    assertTrue(requestedSince[0] >= System.currentTimeMillis() - HistoryPeriod.DAY.durationMs - 5_000L)
    assertEquals(0L, requestedSince[1])
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun <T> TestScope.collectAfterProUnlock(
    proStatusProvider: FakeProStatusProvider,
    history: Flow<List<T>>,
): List<List<T>> {
    val emissionsDeferred = async { history.take(2).toList() }
    advanceUntilIdle()
    proStatusProvider.setPro(true)
    return emissionsDeferred.await()
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

    override suspend fun getReadingsSinceSync(since: Long): List<NetworkReading> = emptyList()

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}

private val testThrottlingEvent =
    ThrottlingEvent(
        id = 1L,
        timestamp = 1_000L,
        thermalStatus = "SEVERE",
        batteryTempC = 43f,
        cpuTempC = null,
        foregroundApp = null,
        durationMs = null,
    )

private val testStorageReading =
    StorageReading(
        timestamp = 1_000L,
        totalBytes = 128_000_000L,
        availableBytes = 64_000_000L,
        appsBytes = 12_000_000L,
        mediaBytes = 24_000_000L,
    )

private class FakeStorageRepository : StorageRepository {
    val requestedSince = mutableListOf<Long>()

    override fun getStorageState() = emptyFlow<StorageState>()

    override suspend fun saveReading(state: StorageState) = Unit

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<StorageReading>> {
        requestedSince += since
        return flowOf(listOf(testStorageReading))
    }

    override suspend fun getReadingsSinceSync(since: Long): List<StorageReading> = emptyList()

    override suspend fun getAllReadings(): List<StorageReading> = emptyList()

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}

private val testThermalReading =
    ThermalReading(
        timestamp = 1_000L,
        batteryTempC = 38f,
        cpuTempC = null,
        thermalStatus = 0,
        throttling = false,
    )

private class FakeThermalRepository : ThermalRepository {
    val requestedSince = mutableListOf<Long>()

    override fun getThermalState() = emptyFlow<ThermalState>()

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<ThermalReading>> {
        requestedSince += since
        return flowOf(listOf(testThermalReading))
    }

    override suspend fun getReadingsSinceSync(since: Long): List<ThermalReading> = emptyList()

    override suspend fun saveReading(state: ThermalState) = Unit

    override suspend fun getAllReadings(): List<ThermalReading> = emptyList()

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
