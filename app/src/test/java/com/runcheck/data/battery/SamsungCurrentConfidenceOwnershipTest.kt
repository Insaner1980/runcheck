package com.runcheck.data.battery

import android.content.Context
import android.os.BatteryManager
import android.os.SystemClock
import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.util.TestAppDispatchers
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SamsungCurrentConfidenceOwnershipTest {
    @Before
    fun setUpClock() {
        mockkStatic(SystemClock::class)
    }

    @After
    fun tearDownClock() {
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `one shot consumer retains recent constant current evidence from continuous consumer`() =
        runTest {
            every { SystemClock.elapsedRealtime() } answers { testScheduler.currentTime }
            val batteryManager =
                mockk<BatteryManager> {
                    every { isCharging } returns false
                    every { getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns -3_500_000
                }
            val context =
                mockk<Context> {
                    every { getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
                }
            val source =
                SamsungBatterySource(
                    context = context,
                    profile =
                        DeviceProfile(
                            manufacturer = "samsung",
                            currentNowReliable = true,
                        ),
                    dispatchers = TestAppDispatchers(StandardTestDispatcher(testScheduler)),
                )

            try {
                // Three physical reads at 0, 2 and 4 seconds establish the existing downgrade.
                val continuousReadings = source.getCurrentNow().take(3).toList()
                assertEquals(
                    listOf(
                        MeasuredValue(-3_500, Confidence.HIGH),
                        MeasuredValue(-3_500, Confidence.HIGH),
                        MeasuredValue(-3_500, Confidence.LOW),
                    ),
                    continuousReadings,
                )

                // Detaching the first consumer must not discard evidence at the next normal poll.
                advanceTimeBy(2_000)
                val oneShotReading = source.getCurrentNow().first()

                assertEquals(MeasuredValue(-3_500, Confidence.LOW), oneShotReading)
            } finally {
                source.close()
            }
        }

    @Test
    fun `five second one shot consumers accumulate evidence and expire after six second gap`() =
        runTest {
            withSource { source, manager ->
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
                advanceTimeBy(5_000)
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
                advanceTimeBy(5_000)
                assertEquals(Confidence.LOW, source.getCurrentNow().first().confidence)
                advanceTimeBy(6_000)
                assertEquals(Confidence.LOW, source.getCurrentNow().first().confidence)
                advanceTimeBy(6_001)
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
                verify(exactly = 5) { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }
            }
        }

    @Test
    fun `overlapping collectors do not accelerate constant current detection`() =
        runTest {
            withSource { source, _ ->
                val readings = List(3) { async { source.getCurrentNow().take(3).toList() } }.awaitAll()
                readings.forEach {
                    assertEquals(listOf(Confidence.HIGH, Confidence.HIGH, Confidence.LOW), it.map { value -> value.confidence })
                }
            }
        }

    @Test
    fun `rereads count only at the two second evidence boundary`() =
        runTest {
            withSource { source, _ ->
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
                advanceTimeBy(1_999)
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
                advanceTimeBy(1)
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
                advanceTimeBy(1_999)
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
                advanceTimeBy(1)
                assertEquals(Confidence.LOW, source.getCurrentNow().first().confidence)
            }
        }

    @Test
    fun `changed current and charging direction restart evidence without stale values`() =
        runTest {
            withSource { source, manager ->
                assertEquals(Confidence.LOW, source.getCurrentNow().take(3).toList().last().confidence)
                every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns -4_000_000
                assertEquals(MeasuredValue(-4_000, Confidence.HIGH), source.getCurrentNow().first())
                advanceTimeBy(2_000)
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
                advanceTimeBy(2_000)
                assertEquals(Confidence.LOW, source.getCurrentNow().first().confidence)
                every { manager.isCharging } returns true
                assertEquals(MeasuredValue(4_000, Confidence.HIGH), source.getCurrentNow().first())
            }
        }

    @Test
    fun `missing observations preserve recent evidence but do not refresh its expiry`() =
        runTest {
            withSource { source, manager ->
                assertEquals(Confidence.LOW, source.getCurrentNow().take(3).toList().last().confidence)
                every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns Int.MIN_VALUE
                advanceTimeBy(2_000)
                assertEquals(Confidence.UNAVAILABLE, source.getCurrentNow().first().confidence)
                every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns -3_500_000
                assertEquals(Confidence.LOW, source.getCurrentNow().first().confidence)
                every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns Int.MIN_VALUE
                advanceTimeBy(6_000)
                assertEquals(Confidence.UNAVAILABLE, source.getCurrentNow().first().confidence)
                advanceTimeBy(1)
                every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns -3_500_000
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
            }
        }

    @Test
    fun `cancellation stops polling releases lock and retains recent evidence`() =
        runTest {
            withSource { source, manager ->
                val collector = backgroundScope.launch { source.getCurrentNow().collect() }
                runCurrent()
                collector.cancelAndJoin()
                advanceTimeBy(2_000)
                runCurrent()
                verify(exactly = 1) { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) }
                assertEquals(Confidence.HIGH, source.getCurrentNow().first().confidence)
                advanceTimeBy(2_000)
                assertEquals(Confidence.LOW, source.getCurrentNow().first().confidence)
            }
        }

    @Test
    fun `generic current and invalid or untrusted confidence rules remain unchanged`() =
        runTest {
            withSource(samsung = false) { source, _ ->
                assertEquals(List(4) { Confidence.HIGH }, source.getCurrentNow().take(4).toList().map { it.confidence })
            }
            for (reliable in listOf(true, false)) {
                withSource(reliable = reliable) { source, manager ->
                    assertEquals(if (reliable) Confidence.HIGH else Confidence.LOW, source.getCurrentNow().first().confidence)
                    for (raw in listOf(0, 10_001_000, -10_001_000, Int.MIN_VALUE)) {
                        every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns raw
                        assertEquals(List(3) { Confidence.UNAVAILABLE }, source.getCurrentNow().take(3).toList().map { it.confidence })
                    }
                }
            }
        }

    private suspend fun TestScope.withSource(
        samsung: Boolean = true,
        reliable: Boolean = true,
        block: suspend (GenericBatterySource, BatteryManager) -> Unit,
    ) {
        every { SystemClock.elapsedRealtime() } answers { testScheduler.currentTime }
        val manager =
            mockk<BatteryManager> {
                every { isCharging } returns false
                every { getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } returns -3_500_000
            }
        val context = mockk<Context> { every { getSystemService(Context.BATTERY_SERVICE) } returns manager }
        val profile = DeviceProfile(manufacturer = if (samsung) "samsung" else "google", currentNowReliable = reliable)
        val dispatchers = TestAppDispatchers(StandardTestDispatcher(testScheduler))
        val source =
            if (samsung) SamsungBatterySource(context, profile, dispatchers) else GenericBatterySource(context, profile, dispatchers)
        try {
            block(source, manager)
        } finally {
            source.close()
        }
    }
}
