package com.runcheck.ui.chart

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ChargingStatus
import com.sun.management.ThreadMXBean
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.lang.management.ManagementFactory

/** Opt-in host JVM benchmark. Timings are descriptive, never CI pass/fail thresholds. */
class ChartPerformanceBenchmark {
    @Test
    fun benchmark() {
        assumeTrue(System.getenv("RUNCHECK_PERFORMANCE_BENCHMARK") == "1")
        println("JVM=${System.getProperty("java.runtime.version")}; OS=${System.getProperty("os.name")}")
        for (size in listOf(240, 2_880, 28_800)) {
            val readings = List(size) { index -> reading(index) }
            val summary = requireNotNull(calculateChargingSessionSummary(readings, 80, ChargingStatus.CHARGING))
            val missing = summary.copy(readings = readings.map { it.copy(currentMa = null) })
            val points = readings.map { it.timestamp to (it.currentMa ?: 0).toFloat() }
            measure("graph-available", size) { summary.hasGraphData() }
            measure("graph-unavailable", size) { missing.hasGraphData() }
            measure("downsample-300", size) { points.downsamplePairs(300) }
            println("CHECKSUM,$size,${points.downsamplePairs(300).hashCode()}")
        }
    }

    private fun measure(
        name: String,
        size: Int,
        operation: () -> Any,
    ) {
        val bean = ManagementFactory.getThreadMXBean() as ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported)
        bean.isThreadAllocatedMemoryEnabled = true
        @Suppress("DEPRECATION") // Keep the benchmark compatible with the Java 17 test toolchain.
        val threadId = Thread.currentThread().id
        val warmupEnd = System.nanoTime() + 1_000_000_000L
        do {
            repeat(200) { sink = operation() }
        } while (System.nanoTime() < warmupEnd)
        val timings = mutableListOf<Long>()
        val allocations = mutableListOf<Long>()
        repeat(15) {
            val allocated = bean.getThreadAllocatedBytes(threadId)
            val started = System.nanoTime()
            repeat(200) { sink = operation() }
            timings.add((System.nanoTime() - started) / 200)
            allocations.add((bean.getThreadAllocatedBytes(threadId) - allocated) / 200)
        }
        println(
            "BENCH,$name,$size,median-ns=${timings.sorted()[7]}," +
                "median-bytes=${allocations.sorted()[7]},samples-ns=$timings",
        )
    }

    private fun reading(index: Int) =
        BatteryReading(
            timestamp = 1_700_000_000_000L + index * 60_000L,
            level = 50,
            voltageMv = 4_000,
            temperatureC = 30f,
            currentMa = 500 + index % 1_500,
            currentConfidence = "ACCURATE",
            status = "CHARGING",
            plugType = "USB",
            health = "GOOD",
            cycleCount = null,
            healthPct = null,
        )

    private companion object {
        @Volatile
        var sink: Any? = null
    }
}
