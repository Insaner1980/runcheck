package com.devicepulse.data.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.measurementlab.ndt7.android.NdtTest
import net.measurementlab.ndt7.android.models.ClientResponse
import net.measurementlab.ndt7.android.models.Measurement
import net.measurementlab.ndt7.android.utils.DataConverter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Speed test service using M-Lab's NDT7 protocol for accurate throughput measurement.
 * Auto-selects the nearest M-Lab server via https://locate.measurementlab.net/
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

    private var activeTest: NdtTest? = null

    fun runSpeedTest(): Flow<SpeedTestProgress> = callbackFlow {
        var downloadMbps = 0.0
        var uploadMbps = 0.0
        var latestRttMs = 0
        var latestRttVarMs = 0
        val downloadStartTime = System.nanoTime()
        var uploadStartTime = 0L
        var downloadFinished = false

        val test = object : NdtTest() {
            override fun onDownloadProgress(clientResponse: ClientResponse) {
                val mbps = DataConverter.convertToMbps(clientResponse)
                downloadMbps = mbps
                val elapsed = (System.nanoTime() - downloadStartTime).toFloat()
                val progress = (elapsed / (TEST_DURATION_NS)).coerceAtMost(1f)
                trySend(SpeedTestProgress.DownloadPhase(mbps, progress))
            }

            override fun onMeasurementDownloadProgress(measurement: Measurement) {
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
                    trySend(SpeedTestProgress.Failed(error.message ?: "Speed test failed"))
                    channel.close()
                    return
                }

                if (testType == TestType.DOWNLOAD) {
                    downloadFinished = true
                    trySend(SpeedTestProgress.DownloadPhase(downloadMbps, 1f))
                    // Emit ping results after download (RTT is available from TCP)
                    if (latestRttMs > 0) {
                        trySend(SpeedTestProgress.PingPhase(latestRttMs, latestRttVarMs))
                    }
                    return
                }

                // Upload finished (or both finished) — emit final results
                trySend(SpeedTestProgress.UploadPhase(uploadMbps, 1f))
                trySend(
                    SpeedTestProgress.Completed(
                        downloadMbps = downloadMbps,
                        uploadMbps = uploadMbps,
                        pingMs = latestRttMs,
                        jitterMs = latestRttVarMs,
                        serverName = "M-Lab",
                        serverLocation = "Nearest Server"
                    )
                )
                channel.close()
            }
        }

        activeTest = test
        test.startTest(NdtTest.TestType.DOWNLOAD_AND_UPLOAD)

        awaitClose {
            activeTest?.stopTest()
            activeTest = null
        }
    }

    fun stopTest() {
        activeTest?.stopTest()
        activeTest = null
    }

    companion object {
        private const val TEST_DURATION_NS = 10_000_000_000f // ~10 seconds per phase
    }
}
