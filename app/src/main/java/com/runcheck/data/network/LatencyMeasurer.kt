package com.runcheck.data.network

import androidx.annotation.WorkerThread
import com.runcheck.BuildConfig
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.ceil

private const val LATENCY_TIMEOUT_MS = 1_500L
private const val LATENCY_SAMPLE_COUNT = 5
private const val MIN_JITTER_SAMPLE_COUNT = 4
private const val LATENCY_TOTAL_TIMEOUT_MS = 6_000L

@Singleton
class LatencyMeasurer
    @Inject
    constructor() {
        data class LatencyResult(
            val pingMs: Int,
            val jitterMs: Int?,
        )

        suspend fun measureLatency(): LatencyResult? {
            return withContext(Dispatchers.IO) {
                withTimeoutOrNull(LATENCY_TOTAL_TIMEOUT_MS) {
                    val results =
                        buildList {
                            repeat(LATENCY_SAMPLE_COUNT) {
                                val latency =
                                    withTimeoutOrNull(LATENCY_TIMEOUT_MS) {
                                        measureTcpConnect()
                                    }
                                if (latency != null) {
                                    add(latency)
                                }
                            }
                        }
                    if (results.isEmpty()) return@withTimeoutOrNull null

                    val pingMs = results.minOrNull() ?: return@withTimeoutOrNull null
                    val jitterMs =
                        if (results.size >= MIN_JITTER_SAMPLE_COUNT) {
                            computeRfc3550Jitter(results)
                        } else {
                            null
                        }
                    LatencyResult(pingMs, jitterMs)
                }
            }
        }

        @WorkerThread
        private fun measureTcpConnect(): Int? =
            try {
                Socket().use { socket ->
                    val startTime = System.nanoTime()
                    socket.connect(
                        InetSocketAddress(BuildConfig.LATENCY_HOST, BuildConfig.LATENCY_PORT),
                        LATENCY_TIMEOUT_MS.toInt(),
                    )
                    val endTime = System.nanoTime()
                    ((endTime - startTime) / 1_000_000).toInt()
                }
            } catch (e: java.io.IOException) {
                ReleaseSafeLog.warn(TAG, "TCP connect failed", e)
                null
            }

        private fun computeRfc3550Jitter(samplesMs: List<Int>): Int? {
            if (samplesMs.size < 2) return null

            var jitterMs = 0.0
            samplesMs.zipWithNext().forEach { (previous, current) ->
                val deltaMs = abs(current - previous).toDouble()
                jitterMs += (deltaMs - jitterMs) / 16.0
            }

            if (jitterMs <= 0.0) {
                return if (samplesMs.zipWithNext().any { (previous, current) -> previous != current }) 1 else 0
            }

            return ceil(jitterMs).toInt()
        }

        private companion object {
            private const val TAG = "LatencyMeasurer"
        }
    }
