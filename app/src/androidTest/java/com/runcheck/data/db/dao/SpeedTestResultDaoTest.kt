package com.runcheck.data.db.dao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.runcheck.data.db.RuncheckDatabase
import com.runcheck.data.db.entity.SpeedTestResultEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedTestResultDaoTest {
    @Test
    fun recentResults_observesResultInsertedAfterSubscription() =
        runBlocking {
            val database =
                Room
                    .inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation().targetContext,
                        RuncheckDatabase::class.java,
                    ).allowMainThreadQueries()
                    .build()

            try {
                val dao = database.speedTestResultDao()
                val emissions = mutableListOf<List<SpeedTestResultEntity>>()
                val collection = async { dao.getRecentResults(limit = 5).take(2).toList(emissions) }

                withTimeout(5_000) {
                    while (emissions.isEmpty()) {
                        delay(10)
                    }
                }

                dao.insert(speedTestResultEntity(timestamp = 2_000L))
                withTimeout(5_000) { collection.await() }

                assertTrue(emissions.first().isEmpty())
                assertEquals(2_000L, emissions.last().single().timestamp)
            } finally {
                database.close()
            }
        }

    @Test
    fun deleteOldResults_keepsNewestResultsAndProtectedInsertion() =
        runBlocking {
            val database =
                Room
                    .inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation().targetContext,
                        RuncheckDatabase::class.java,
                    ).allowMainThreadQueries()
                    .build()

            try {
                val dao = database.speedTestResultDao()
                dao.insert(speedTestResultEntity(timestamp = 4_000L))
                dao.insert(speedTestResultEntity(timestamp = 3_000L))
                dao.insert(speedTestResultEntity(timestamp = 2_000L))
                val insertedId = dao.insert(speedTestResultEntity(timestamp = 1_000L))

                dao.deleteOldResults(keepCount = 3, protectedId = insertedId)

                assertEquals(listOf(1_000L, 3_000L, 4_000L), dao.getAll().map { it.timestamp })
            } finally {
                database.close()
            }
        }

    private fun speedTestResultEntity(timestamp: Long) =
        SpeedTestResultEntity(
            timestamp = timestamp,
            downloadMbps = 100.0,
            uploadMbps = 50.0,
            pingMs = 10,
            jitterMs = null,
            serverName = null,
            serverLocation = null,
            connectionType = "WIFI",
            networkSubtype = null,
            signalDbm = null,
        )
}
