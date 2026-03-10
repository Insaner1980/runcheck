package com.devicepulse.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LatencyMeasurer @Inject constructor() {

    suspend fun measureLatency(): Int? {
        return withContext(Dispatchers.IO) {
            // Take multiple TCP connect samples and use the minimum (closest to true RTT)
            val results = mutableListOf<Int>()
            repeat(SAMPLE_COUNT) {
                measureTcpConnect()?.let { results.add(it) }
            }
            results.minOrNull()
        }
    }

    private fun measureTcpConnect(): Int? {
        return try {
            val socket = Socket()
            val startTime = System.nanoTime()
            socket.connect(InetSocketAddress(DNS_HOST, DNS_PORT), TIMEOUT_MS)
            val endTime = System.nanoTime()
            socket.close()
            ((endTime - startTime) / 1_000_000).toInt()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        // Google Public DNS — fast, reliable, worldwide
        private const val DNS_HOST = "8.8.8.8"
        private const val DNS_PORT = 53
        private const val TIMEOUT_MS = 3000
        private const val SAMPLE_COUNT = 3
    }
}
