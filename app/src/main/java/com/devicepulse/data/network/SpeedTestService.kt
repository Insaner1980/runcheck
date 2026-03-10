package com.devicepulse.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Speed test service that measures download speed, upload speed, and latency
 * using HTTP-based throughput measurement against public CDN endpoints.
 */
@Singleton
class SpeedTestService @Inject constructor() {

    sealed interface SpeedTestProgress {
        data class PingPhase(val pingMs: Int, val jitterMs: Int) : SpeedTestProgress
        data class DownloadPhase(val currentMbps: Double, val progress: Float) : SpeedTestProgress
        data class UploadPhase(val currentMbps: Double, val progress: Float) : SpeedTestProgress
        data class Completed(
            val downloadMbps: Double,
            val uploadMbps: Double,
            val pingMs: Int,
            val jitterMs: Int,
            val serverName: String,
            val serverLocation: String
        ) : SpeedTestProgress
        data class Failed(val error: String) : SpeedTestProgress
    }

    fun runSpeedTest(): Flow<SpeedTestProgress> = flow {
        try {
            // Phase 1: Ping test
            val pingResult = measurePing()
            emit(SpeedTestProgress.PingPhase(pingResult.avgMs, pingResult.jitterMs))

            // Phase 2: Download test
            var finalDownloadMbps = 0.0
            val downloadSamples = mutableListOf<Double>()
            val downloadStartTime = System.nanoTime()
            val downloadDurationNs = DOWNLOAD_DURATION_MS * 1_000_000L
            var totalDownloadBytes = 0L

            for (url in DOWNLOAD_URLS) {
                if (System.nanoTime() - downloadStartTime > downloadDurationNs) break
                try {
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5_000
                        readTimeout = 10_000
                        requestMethod = "GET"
                        setRequestProperty("Cache-Control", "no-cache")
                    }
                    connection.connect()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val buffer = ByteArray(BUFFER_SIZE)
                        val inputStream = connection.inputStream
                        val chunkStart = System.nanoTime()
                        var chunkBytes = 0L

                        while (true) {
                            if (System.nanoTime() - downloadStartTime > downloadDurationNs) break
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            totalDownloadBytes += bytesRead
                            chunkBytes += bytesRead

                            val chunkElapsed = System.nanoTime() - chunkStart
                            if (chunkElapsed > 500_000_000L) { // Update every 500ms
                                val mbps = (chunkBytes * 8.0) / (chunkElapsed / 1_000_000_000.0) / 1_000_000.0
                                downloadSamples.add(mbps)
                                val elapsed = System.nanoTime() - downloadStartTime
                                val progress = (elapsed.toFloat() / downloadDurationNs).coerceAtMost(1f)
                                emit(SpeedTestProgress.DownloadPhase(mbps, progress))
                                chunkBytes = 0
                            }
                        }
                        inputStream.close()
                    }
                    connection.disconnect()
                } catch (_: Exception) {
                    // Try next URL
                }
            }

            val totalDownloadElapsed = System.nanoTime() - downloadStartTime
            finalDownloadMbps = if (totalDownloadBytes > 0 && totalDownloadElapsed > 0) {
                (totalDownloadBytes * 8.0) / (totalDownloadElapsed / 1_000_000_000.0) / 1_000_000.0
            } else {
                downloadSamples.lastOrNull() ?: 0.0
            }
            emit(SpeedTestProgress.DownloadPhase(finalDownloadMbps, 1f))

            // Phase 3: Upload test
            var finalUploadMbps = 0.0
            val uploadStartTime = System.nanoTime()
            val uploadDurationNs = UPLOAD_DURATION_MS * 1_000_000L
            var totalUploadBytes = 0L
            val uploadData = ByteArray(BUFFER_SIZE) { 0 }

            for (url in UPLOAD_URLS) {
                if (System.nanoTime() - uploadStartTime > uploadDurationNs) break
                try {
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5_000
                        readTimeout = 10_000
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/octet-stream")
                        setChunkedStreamingMode(BUFFER_SIZE)
                    }

                    val outputStream: OutputStream = connection.outputStream
                    val chunkStart = System.nanoTime()
                    var chunkBytes = 0L

                    while (System.nanoTime() - uploadStartTime < uploadDurationNs) {
                        try {
                            outputStream.write(uploadData)
                            outputStream.flush()
                            totalUploadBytes += uploadData.size
                            chunkBytes += uploadData.size

                            val chunkElapsed = System.nanoTime() - chunkStart
                            if (chunkElapsed > 500_000_000L) {
                                val mbps = (chunkBytes * 8.0) / (chunkElapsed / 1_000_000_000.0) / 1_000_000.0
                                val elapsed = System.nanoTime() - uploadStartTime
                                val progress = (elapsed.toFloat() / uploadDurationNs).coerceAtMost(1f)
                                emit(SpeedTestProgress.UploadPhase(mbps, progress))
                                chunkBytes = 0
                            }
                        } catch (_: Exception) {
                            break
                        }
                    }
                    outputStream.close()
                    connection.disconnect()
                } catch (_: Exception) {
                    // Try next URL
                }
            }

            val totalUploadElapsed = System.nanoTime() - uploadStartTime
            finalUploadMbps = if (totalUploadBytes > 0 && totalUploadElapsed > 0) {
                (totalUploadBytes * 8.0) / (totalUploadElapsed / 1_000_000_000.0) / 1_000_000.0
            } else {
                0.0
            }
            emit(SpeedTestProgress.UploadPhase(finalUploadMbps, 1f))

            emit(
                SpeedTestProgress.Completed(
                    downloadMbps = finalDownloadMbps,
                    uploadMbps = finalUploadMbps,
                    pingMs = pingResult.avgMs,
                    jitterMs = pingResult.jitterMs,
                    serverName = SERVER_NAME,
                    serverLocation = SERVER_LOCATION
                )
            )
        } catch (e: Exception) {
            emit(SpeedTestProgress.Failed(e.message ?: "Speed test failed"))
        }
    }.flowOn(Dispatchers.IO)

    private data class PingResult(val avgMs: Int, val jitterMs: Int)

    private suspend fun measurePing(): PingResult = withContext(Dispatchers.IO) {
        val samples = mutableListOf<Long>()
        val hosts = listOf(
            InetSocketAddress("1.1.1.1", 53),
            InetSocketAddress("8.8.8.8", 53)
        )

        for (i in 0 until PING_COUNT) {
            for (host in hosts) {
                try {
                    val socket = Socket()
                    val start = System.nanoTime()
                    socket.connect(host, 3_000)
                    val elapsed = (System.nanoTime() - start) / 1_000_000
                    socket.close()
                    samples.add(elapsed)
                    break
                } catch (_: Exception) {
                    // Try next host
                }
            }
        }

        if (samples.isEmpty()) {
            return@withContext PingResult(avgMs = -1, jitterMs = 0)
        }

        val avgMs = samples.average().toInt()
        val jitterMs = if (samples.size > 1) {
            samples.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average().toInt()
        } else {
            0
        }
        PingResult(avgMs = avgMs, jitterMs = jitterMs)
    }

    companion object {
        private const val DOWNLOAD_DURATION_MS = 10_000L
        private const val UPLOAD_DURATION_MS = 10_000L
        private const val PING_COUNT = 5
        private const val BUFFER_SIZE = 65536

        private const val SERVER_NAME = "Cloudflare"
        private const val SERVER_LOCATION = "Nearest Edge"

        // Cloudflare speed test endpoints (publicly available, no API key)
        private val DOWNLOAD_URLS = listOf(
            "https://speed.cloudflare.com/__down?bytes=25000000",
            "https://speed.cloudflare.com/__down?bytes=10000000",
            "https://speed.cloudflare.com/__down?bytes=10000000"
        )

        private val UPLOAD_URLS = listOf(
            "https://speed.cloudflare.com/__up"
        )
    }
}
