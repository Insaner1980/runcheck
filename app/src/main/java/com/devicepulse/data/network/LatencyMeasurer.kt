package com.devicepulse.data.network

import com.devicepulse.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

private const val LATENCY_TIMEOUT_MS = 3000
private const val LATENCY_SAMPLE_COUNT = 3

@Singleton
class LatencyMeasurer @Inject constructor() {

    suspend fun measureLatency(): Int? {
        return withContext(Dispatchers.IO) {
            // Take multiple TCP connect samples and use the minimum (closest to true RTT)
            val results = List(LATENCY_SAMPLE_COUNT) { measureTcpConnect() }.mapNotNull { it }
            results.minOrNull()
        }
    }

    private fun measureTcpConnect(): Int? {
        return try {
            Socket().use { socket ->
                val startTime = System.nanoTime()
                socket.connect(
                    InetSocketAddress(BuildConfig.LATENCY_HOST, BuildConfig.LATENCY_PORT),
                    LATENCY_TIMEOUT_MS
                )
                val endTime = System.nanoTime()
                ((endTime - startTime) / 1_000_000).toInt()
            }
        } catch (_: Exception) {
            null
        }
    }
}
