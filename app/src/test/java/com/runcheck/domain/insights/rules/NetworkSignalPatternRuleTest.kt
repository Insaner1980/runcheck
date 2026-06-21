package com.runcheck.domain.insights.rules

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSignalPatternRuleTest {
    @Test
    fun `returns network insight when cellular signal is repeatedly weak`() =
        runTest {
            val rule = NetworkSignalPatternRule(TestNetworkRepository(sustainedWeakSignalReadings()))

            val insights = rule.evaluate(NOW)

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
            val rule = NetworkSignalPatternRule(TestNetworkRepository(mixedSignalReadings()))

            val insights = rule.evaluate(NOW)

            assertTrue(insights.isEmpty())
        }

    private fun sustainedWeakSignalReadings() =
        listOf(
            cellularReading(offsetHours = 50, signalDbm = -112, latencyMs = 85),
            cellularReading(offsetHours = 42, signalDbm = -116, latencyMs = 110),
            cellularReading(offsetHours = 34, signalDbm = -114, latencyMs = 120),
            cellularReading(offsetHours = 26, signalDbm = -118, latencyMs = 140),
            cellularReading(offsetHours = 18, signalDbm = -113, latencyMs = 95),
            cellularReading(offsetHours = 10, signalDbm = -108, latencyMs = 80),
        )

    private fun mixedSignalReadings() =
        listOf(
            cellularReading(offsetHours = 50, signalDbm = -101, latencyMs = 40),
            cellularReading(offsetHours = 42, signalDbm = -105, latencyMs = 45),
            cellularReading(offsetHours = 34, signalDbm = -109, latencyMs = 55),
            cellularReading(offsetHours = 26, signalDbm = -111, latencyMs = 60),
            wifiReading(offsetHours = 18),
            cellularReading(offsetHours = 10, signalDbm = -104, latencyMs = 48),
        )

    private fun cellularReading(
        offsetHours: Long,
        signalDbm: Int,
        latencyMs: Int,
    ) = networkReading(
        timestamp = NOW - offsetHours * HOUR_MS,
        type = "CELLULAR",
        signalDbm = signalDbm,
        latencyMs = latencyMs,
    )

    private fun wifiReading(offsetHours: Long) =
        networkReading(
            timestamp = NOW - offsetHours * HOUR_MS,
            type = "WIFI",
            signalDbm = null,
            latencyMs = 18,
            wifiSpeedMbps = 320,
            wifiFrequency = 5200,
        )

    private companion object {
        private const val HOUR_MS = 60L * 60L * 1000L
        private const val NOW = 96L * HOUR_MS
    }
}
