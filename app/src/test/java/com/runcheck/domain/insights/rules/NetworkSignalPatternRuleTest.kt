package com.runcheck.domain.insights.rules

import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSignalPatternRuleTest {
    @Test
    fun `returns network insight when cellular signal is repeatedly weak`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 96L * hourMs
            val readings =
                listOf(
                    NetworkReading(
                        timestamp = now - 50L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -112,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 85,
                    ),
                    NetworkReading(
                        timestamp = now - 42L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -116,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 110,
                    ),
                    NetworkReading(
                        timestamp = now - 34L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -114,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 120,
                    ),
                    NetworkReading(
                        timestamp = now - 26L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -118,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 140,
                    ),
                    NetworkReading(
                        timestamp = now - 18L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -113,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 95,
                    ),
                    NetworkReading(
                        timestamp = now - 10L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -108,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 80,
                    ),
                )
            val rule = NetworkSignalPatternRule(FakeNetworkRepository(readings))

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(NetworkSignalPatternRule.RULE_ID, insight.ruleId)
            assertEquals("cellular:75plus", insight.dedupeKey)
            assertEquals("83", insight.bodyArgs[0])
            assertEquals("-115", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when weak signal is not sustained`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 96L * hourMs
            val readings =
                listOf(
                    NetworkReading(
                        timestamp = now - 50L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -101,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 40,
                    ),
                    NetworkReading(
                        timestamp = now - 42L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -105,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 45,
                    ),
                    NetworkReading(
                        timestamp = now - 34L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -109,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 55,
                    ),
                    NetworkReading(
                        timestamp = now - 26L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -111,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 60,
                    ),
                    NetworkReading(
                        timestamp = now - 18L * hourMs,
                        type = "WIFI",
                        signalDbm = null,
                        wifiSpeedMbps = 320,
                        wifiFrequency = 5200,
                        carrier = null,
                        networkSubtype = null,
                        latencyMs = 18,
                    ),
                    NetworkReading(
                        timestamp = now - 10L * hourMs,
                        type = "CELLULAR",
                        signalDbm = -104,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Carrier",
                        networkSubtype = "LTE",
                        latencyMs = 48,
                    ),
                )
            val rule = NetworkSignalPatternRule(FakeNetworkRepository(readings))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }
}

private class FakeNetworkRepository(
    private val readings: List<NetworkReading>,
) : NetworkRepository {
    override fun getNetworkState(): Flow<NetworkState> = emptyFlow()

    override suspend fun measureLatency(): Int? = null

    override suspend fun saveReading(state: NetworkState) = Unit

    override suspend fun getAllReadings(): List<NetworkReading> = readings

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<NetworkReading>> = emptyFlow()

    override suspend fun getReadingsSinceSync(since: Long): List<NetworkReading> =
        readings.filter { it.timestamp >= since }

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
