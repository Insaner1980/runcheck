package com.runcheck.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.runcheck.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.measurementlab.ndt7.android.NdtTest
import net.measurementlab.ndt7.android.models.ClientResponse
import net.measurementlab.ndt7.android.models.Measurement
import net.measurementlab.ndt7.android.utils.DataConverter
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Speed test service using M-Lab's NDT7 protocol for accurate throughput measurement.
 * Auto-selects the nearest M-Lab server via https://locate.measurementlab.net/
 */
@Singleton
class SpeedTestService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val latencyMeasurer: LatencyMeasurer
) {

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
            val serverLocation: String?
        ) : SpeedTestProgress
        data class Failed(val error: String) : SpeedTestProgress
    }

    @Volatile
    private var activeTest: NdtTest? = null
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun runSpeedTest(): Flow<SpeedTestProgress> = callbackFlow {
        if (!hasValidatedConnection()) {
            trySend(SpeedTestProgress.Failed(context.getString(R.string.speed_test_error_no_internet)))
            channel.close()
            return@callbackFlow
        }

        var downloadMbps = 0.0
        var uploadMbps = 0.0
        var latestRttMs = 0
        var latestRttVarMs = 0
        var serverName = DEFAULT_SERVER_NAME
        var serverLocation: String? = null
        val downloadStartTime = System.nanoTime()
        var uploadStartTime = 0L

        latencyMeasurer.measureLatency()?.let { initialLatency ->
            latestRttMs = initialLatency
            trySend(SpeedTestProgress.PingPhase(initialLatency, 0))
        }

        val test = object : NdtTest() {
            override fun onDownloadProgress(clientResponse: ClientResponse) {
                updateServerMetadata(clientResponse)?.let { metadata ->
                    serverName = metadata.name
                    serverLocation = metadata.location
                }
                val mbps = DataConverter.convertToMbps(clientResponse)
                downloadMbps = mbps
                val elapsed = (System.nanoTime() - downloadStartTime).toFloat()
                val progress = (elapsed / (TEST_DURATION_NS)).coerceAtMost(1f)
                trySend(SpeedTestProgress.DownloadPhase(mbps, progress))
            }

            override fun onMeasurementDownloadProgress(measurement: Measurement) {
                measurement.connectionInfo?.server?.takeIf { it.isNotBlank() }?.let { host ->
                    serverName = host
                }
                val tcpInfo = measurement.tcpInfo
                if (tcpInfo != null) {
                    val rttUs = tcpInfo.rtt
                    val rttVarUs = tcpInfo.rttVar
                    if (rttUs > 0) {
                        latestRttMs = (rttUs / 1000).toInt()
                        latestRttVarMs = (rttVarUs / 1000).toInt()
                    }
                }
            }

            override fun onUploadProgress(clientResponse: ClientResponse) {
                updateServerMetadata(clientResponse)?.let { metadata ->
                    serverName = metadata.name
                    serverLocation = metadata.location
                }
                val mbps = DataConverter.convertToMbps(clientResponse)
                uploadMbps = mbps
                if (uploadStartTime == 0L) uploadStartTime = System.nanoTime()
                val elapsed = (System.nanoTime() - uploadStartTime).toFloat()
                val progress = (elapsed / (TEST_DURATION_NS)).coerceAtMost(1f)
                trySend(SpeedTestProgress.UploadPhase(mbps, progress))
            }

            override fun onMeasurementUploadProgress(measurement: Measurement) {
                val tcpInfo = measurement.tcpInfo
                if (tcpInfo != null) {
                    val rttUs = tcpInfo.rtt
                    if (rttUs > 0) {
                        latestRttMs = (rttUs / 1000).toInt()
                        latestRttVarMs = (tcpInfo.rttVar / 1000).toInt()
                    }
                }
            }

            override fun onFinished(
                clientResponse: ClientResponse?,
                error: Throwable?,
                testType: TestType
            ) {
                if (error != null && clientResponse == null) {
                    trySend(SpeedTestProgress.Failed(mapError(error)))
                    channel.close()
                    return
                }

                if (testType == TestType.DOWNLOAD) {
                    trySend(SpeedTestProgress.DownloadPhase(downloadMbps, 1f))
                    return
                }

                updateServerMetadata(clientResponse)?.let { metadata ->
                    serverName = metadata.name
                    serverLocation = metadata.location
                }
                // Upload finished (or both finished) — emit final results
                trySend(SpeedTestProgress.UploadPhase(uploadMbps, 1f))
                trySend(
                    SpeedTestProgress.Completed(
                        downloadMbps = downloadMbps,
                        uploadMbps = uploadMbps,
                        pingMs = latestRttMs,
                        jitterMs = latestRttVarMs,
                        serverName = serverName,
                        serverLocation = serverLocation
                    )
                )
                channel.close()
            }
        }

        activeTest = test
        runCatching {
            test.startTest(NdtTest.TestType.DOWNLOAD_AND_UPLOAD)
        }.onFailure { error ->
            trySend(SpeedTestProgress.Failed(mapError(error)))
            activeTest = null
            channel.close()
        }

        awaitClose {
            activeTest?.stopTest()
            activeTest = null
        }
    }

    fun stopTest() {
        activeTest?.stopTest()
        activeTest = null
    }

    private fun hasValidatedConnection(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun updateServerMetadata(clientResponse: ClientResponse?): ServerMetadata? {
        val originHost = clientResponse?.origin?.let(::extractHost)
        val testHost = clientResponse?.test?.let(::extractHost)
        val resolvedName = testHost ?: originHost ?: return null
        return ServerMetadata(
            name = resolvedName,
            location = originHost
                ?.takeUnless { isSameServerLabel(it, resolvedName) }
        )
    }

    private fun extractHost(rawValue: String): String {
        val trimmed = rawValue.trim()
        return runCatching { URI(trimmed).host?.takeIf { it.isNotBlank() } ?: trimmed }
            .getOrDefault(trimmed)
    }

    private fun mapError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            error is SocketTimeoutException || message.contains("timeout", ignoreCase = true) ->
                context.getString(R.string.speed_test_error_timeout)
            error is UnknownHostException ||
                message.contains("unable to resolve host", ignoreCase = true) ||
                message.contains("unreachable", ignoreCase = true) ||
                message.contains("failed to connect", ignoreCase = true) ->
                context.getString(R.string.speed_test_error_server_unreachable)
            message.contains("network", ignoreCase = true) ||
                message.contains("internet", ignoreCase = true) ||
                message.contains("offline", ignoreCase = true) ->
                context.getString(R.string.speed_test_error_no_internet)
            else -> context.getString(R.string.speed_test_error_generic)
        }
    }

    private fun isSameServerLabel(left: String, right: String): Boolean {
        return left.equals(right, ignoreCase = true) ||
            left.contains(right, ignoreCase = true) ||
            right.contains(left, ignoreCase = true)
    }

    private data class ServerMetadata(
        val name: String,
        val location: String?
    )

    companion object {
        private const val TEST_DURATION_NS = 10_000_000_000f // ~10 seconds per phase
        private const val DEFAULT_SERVER_NAME = "M-Lab"
    }
}
