package com.runcheck.data.network

import com.runcheck.data.db.dao.NetworkReadingDao
import com.runcheck.data.db.entity.NetworkReadingEntity
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SignalQuality
import com.runcheck.util.TestAppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkRepositoryImplTest {
    private val networkDataSource: NetworkDataSource = mockk(relaxed = true)
    private val latencyMeasurer: LatencyMeasurer = mockk(relaxed = true)
    private val networkReadingDao: NetworkReadingDao = mockk(relaxed = true)
    private val repository =
        NetworkRepositoryImpl(
            networkDataSource = networkDataSource,
            latencyMeasurer = latencyMeasurer,
            networkReadingDao = networkReadingDao,
            dispatchers = TestAppDispatchers(),
        )

    @Test
    fun `measureLatency returns null when connection is not validated`() =
        runTest {
            every { networkDataSource.hasValidatedConnection() } returns false

            assertNull(repository.measureLatency())
            coVerify(exactly = 0) { latencyMeasurer.measureLatency() }
        }

    @Test
    fun `measureLatency returns ping from latency measurer when connection is validated`() =
        runTest {
            every { networkDataSource.hasValidatedConnection() } returns true
            coEvery { latencyMeasurer.measureLatency() } returns
                LatencyMeasurer.LatencyResult(pingMs = 42, jitterMs = 3)

            assertEquals(42, repository.measureLatency())
        }

    @Test
    fun `reading queries map entities to domain models`() =
        runTest {
            val entity = networkReadingEntity()
            val expected = networkReading()
            every { networkReadingDao.getReadingsSince(10L) } returns flowOf(listOf(entity))
            every { networkReadingDao.getReadingsSinceLimited(10L, 1) } returns flowOf(listOf(entity))
            coEvery { networkReadingDao.getReadingsSinceSync(10L) } returns listOf(entity)
            coEvery { networkReadingDao.getAll() } returns listOf(entity)

            assertEquals(listOf(expected), repository.getReadingsSince(10L, limit = null).first())
            assertEquals(listOf(expected), repository.getReadingsSince(10L, limit = 1).first())
            assertEquals(listOf(expected), repository.getReadingsSinceSync(10L))
            assertEquals(listOf(expected), repository.getAllReadings())
        }

    @Test
    fun `save and delete methods delegate mapped values to dao`() =
        runTest {
            val inserted = slot<NetworkReadingEntity>()

            repository.saveReading(
                NetworkState(
                    connectionType = ConnectionType.WIFI,
                    signalDbm = -62,
                    signalQuality = SignalQuality.GOOD,
                    wifiSpeedMbps = 866,
                    wifiFrequencyMhz = 5_200,
                    carrier = "Carrier",
                    networkSubtype = "NR",
                    latencyMs = 42,
                ),
            )
            repository.deleteOlderThan(100L)
            repository.deleteAll()

            coVerify(exactly = 1) { networkReadingDao.insert(capture(inserted)) }
            assertEquals(ConnectionType.WIFI.name, inserted.captured.type)
            assertEquals(-62, inserted.captured.signalDbm)
            assertEquals(866, inserted.captured.wifiSpeedMbps)
            assertEquals(5_200, inserted.captured.wifiFrequency)
            assertEquals("Carrier", inserted.captured.carrier)
            assertEquals("NR", inserted.captured.networkSubtype)
            assertEquals(42, inserted.captured.latencyMs)
            coVerify(exactly = 1) { networkReadingDao.deleteOlderThan(100L) }
            coVerify(exactly = 1) { networkReadingDao.deleteAll() }
        }

    private fun networkReadingEntity(): NetworkReadingEntity =
        NetworkReadingEntity(
            id = 3L,
            timestamp = 1_234L,
            type = ConnectionType.WIFI.name,
            signalDbm = -62,
            wifiSpeedMbps = 866,
            wifiFrequency = 5_200,
            carrier = "Carrier",
            networkSubtype = "NR",
            latencyMs = 42,
        )

    private fun networkReading(): NetworkReading =
        NetworkReading(
            timestamp = 1_234L,
            type = ConnectionType.WIFI.name,
            signalDbm = -62,
            wifiSpeedMbps = 866,
            wifiFrequency = 5_200,
            carrier = "Carrier",
            networkSubtype = "NR",
            latencyMs = 42,
        )
}
