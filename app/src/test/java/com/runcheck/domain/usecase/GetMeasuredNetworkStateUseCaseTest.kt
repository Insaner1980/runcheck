package com.runcheck.domain.usecase

import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.repository.NetworkRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetMeasuredNetworkStateUseCaseTest {
    private val networkRepository = mockk<NetworkRepository>()

    @Test
    fun `connection type change clears previous network latency before remeasurement`() =
        runTest {
            val networkState = MutableStateFlow(networkState(ConnectionType.WIFI))
            val wifiLatency = CompletableDeferred<Int?>()
            val cellularLatency = CompletableDeferred<Int?>()
            var measurementCount = 0
            every { networkRepository.getNetworkState() } returns networkState
            coEvery { networkRepository.measureLatency() } coAnswers {
                if (measurementCount++ == 0) wifiLatency.await() else cellularLatency.await()
            }
            val useCase = GetMeasuredNetworkStateUseCase(GetNetworkStateUseCase(networkRepository), networkRepository)
            val emissions = mutableListOf<NetworkState>()
            val collection = useCase().onEach(emissions::add).launchIn(backgroundScope)

            runCurrent()
            wifiLatency.complete(24)
            runCurrent()
            assertEquals(networkState(ConnectionType.WIFI, latencyMs = 24), emissions.last())

            networkState.value = networkState(ConnectionType.CELLULAR)
            runCurrent()

            assertEquals(networkState(ConnectionType.CELLULAR), emissions.last())
            cellularLatency.complete(41)
            runCurrent()
            assertEquals(networkState(ConnectionType.CELLULAR, latencyMs = 41), emissions.last())

            networkState.value = networkState(ConnectionType.NONE)
            runCurrent()

            assertEquals(networkState(ConnectionType.NONE), emissions.last())
            collection.cancel()
        }

    private fun networkState(
        connectionType: ConnectionType,
        latencyMs: Int? = null,
    ) = NetworkState(
        connectionType = connectionType,
        signalDbm = null,
        signalQuality = SignalQuality.NO_SIGNAL,
        latencyMs = latencyMs,
    )
}
