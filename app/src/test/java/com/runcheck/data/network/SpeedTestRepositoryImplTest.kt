package com.runcheck.data.network

import com.runcheck.data.db.dao.SpeedTestResultDao
import com.runcheck.data.db.entity.SpeedTestResultEntity
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.util.TestAppDispatchers
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedTestRepositoryImplTest {
    private val dao: SpeedTestResultDao = mockk()

    @Test
    fun `save and explicit count trim run inside the same transaction in order`() =
        runTest {
            val events = mutableListOf<String>()
            var inTransaction = false
            val transactionRunner =
                DatabaseTransactionRunner { block ->
                    events.add("begin")
                    inTransaction = true
                    try {
                        block()
                    } finally {
                        inTransaction = false
                        events.add("end")
                    }
                }
            val repository = SpeedTestRepositoryImpl(mockk(), dao, TestAppDispatchers(), transactionRunner)
            coEvery { dao.insert(any()) } answers {
                assertTrue(inTransaction)
                events.add("insert")
                42L
            }
            coEvery { dao.deleteOldResults(17, 42L) } answers {
                assertTrue(inTransaction)
                events.add("trim")
            }

            repository.saveResultAndTrim(
                SpeedTestResult(
                    timestamp = 1_000L,
                    downloadMbps = 10.0,
                    uploadMbps = 2.0,
                    pingMs = 10,
                    jitterMs = null,
                    serverName = null,
                    serverLocation = null,
                    connectionType = ConnectionType.WIFI,
                    networkSubtype = null,
                    signalDbm = null,
                ),
                keepCount = 17,
            )

            assertEquals(listOf("begin", "insert", "trim", "end"), events)
        }

    @Test
    fun `history forwards explicit query limit and preserves DAO order and count`() =
        runTest {
            val rows = listOf(entity(3L, 2_000L), entity(2L, 2_000L), entity(1L, 1_000L))
            every { dao.getRecentResults(17) } returns flowOf(rows)
            val repository = SpeedTestRepositoryImpl(mockk(), dao, TestAppDispatchers(), mockk())

            val results = repository.getRecentResults(17).first()

            assertEquals(rows.map { it.id }, results.map { it.id })
            assertEquals(rows.map { it.timestamp }, results.map { it.timestamp })
            verify(exactly = 1) { dao.getRecentResults(17) }
        }

    @Test
    fun `unknown latest connection type returns null without querying older rows`() =
        runTest {
            val row = entity(3L, 2_000L, connectionType = "SATELLITE")
            every { dao.getLatestResult() } returns flowOf(row)
            val repository = SpeedTestRepositoryImpl(mockk(), dao, TestAppDispatchers(), mockk())

            assertNull(repository.getLatestResult().first())
            assertEquals("SATELLITE", row.connectionType)
            verify(exactly = 1) { dao.getLatestResult() }
            verify(exactly = 0) { dao.getRecentResults(any()) }
        }

    @Test
    fun `history skips unknown connection types without replacing rows or order`() =
        runTest {
            val rows =
                listOf(
                    entity(5L, 5_000L, connectionType = "WIFI"),
                    entity(4L, 4_000L, connectionType = "SATELLITE"),
                    entity(3L, 3_000L, connectionType = "NONE"),
                    entity(2L, 2_000L, connectionType = "wifi"),
                    entity(1L, 1_000L, connectionType = "CELLULAR"),
                )
            every { dao.getRecentResults(5) } returns flowOf(rows)
            val repository = SpeedTestRepositoryImpl(mockk(), dao, TestAppDispatchers(), mockk())

            val results = repository.getRecentResults(5).first()

            assertEquals(listOf(5L, 3L, 1L), results.map { it.id })
            assertEquals(
                listOf(ConnectionType.WIFI, ConnectionType.NONE, ConnectionType.CELLULAR),
                results.map { it.connectionType },
            )
            assertEquals(listOf("WIFI", "SATELLITE", "NONE", "wifi", "CELLULAR"), rows.map { it.connectionType })
            verify(exactly = 1) { dao.getRecentResults(5) }
        }

    @Test
    fun `known latest NONE connection type remains a domain result`() =
        runTest {
            every { dao.getLatestResult() } returns flowOf(entity(1L, 1_000L, connectionType = "NONE"))
            val repository = SpeedTestRepositoryImpl(mockk(), dao, TestAppDispatchers(), mockk())

            assertEquals(ConnectionType.NONE, repository.getLatestResult().first()?.connectionType)
        }

    private fun entity(
        id: Long,
        timestamp: Long,
        connectionType: String = "WIFI",
    ) = SpeedTestResultEntity(
        id = id,
        timestamp = timestamp,
        downloadMbps = 10.0,
        uploadMbps = 2.0,
        pingMs = 10,
        jitterMs = null,
        serverName = null,
        serverLocation = null,
        connectionType = connectionType,
        networkSubtype = null,
        signalDbm = null,
    )
}
