package com.runcheck.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.runcheck.R
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.SpeedTestConnectionInfo
import com.runcheck.domain.model.SpeedTestProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import net.measurementlab.ndt7.android.NdtTest
import net.measurementlab.ndt7.android.models.ClientResponse
import net.measurementlab.ndt7.android.models.Measurement
import net.measurementlab.ndt7.android.utils.DataConverter
import net.measurementlab.ndt7.android.utils.HttpClientFactory
import okhttp3.Dns
import java.net.InetAddress
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
class SpeedTestService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val latencyMeasurer: LatencyMeasurer,
        private val networkDataSource: NetworkDataSource,
    ) {
        private val testLock = Any()
        private val activeSessionGuard = ActiveSpeedTestSessionGuard<SpeedTestSession>()
        private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun runSpeedTest(allowCellular: Boolean = false): Flow<SpeedTestProgress> =
            callbackFlow {
                val validatedNetwork = validateConnection(allowCellular)
                if (validatedNetwork == null) {
                    channel.close()
                    return@callbackFlow
                }

                val session =
                    SpeedTestSession(
                        connectionInfo = validatedNetwork.info.toConnectionInfo(),
                        startingDefaultNetwork = validatedNetwork.network,
                        scope = this,
                    )
                if (!activeSessionGuard.tryActivate(session)) {
                    trySend(
                        SpeedTestProgress.Failed(
                            context.getString(R.string.speed_test_error_already_running),
                        ),
                    )
                    channel.close()
                    return@callbackFlow
                }

                val connectionCallback = session.createConnectionCallback()
                var callbackRegistered = false
                try {
                    val registrationError =
                        runCatching {
                            connectivityManager.registerDefaultNetworkCallback(connectionCallback)
                            callbackRegistered = true
                        }.exceptionOrNull()
                    if (registrationError != null) {
                        session.failAndClose(mapError(registrationError))
                        return@callbackFlow
                    }

                    val phaseJob =
                        launch {
                            session.verifyCurrentDefaultNetwork(connectivityManager.activeNetwork)
                            session.runLatencyPhase(validatedNetwork.network)

                            val startError = session.startNdtTest(NdtTest.TestType.DOWNLOAD)
                            if (startError != null) {
                                session.failAndClose(mapError(startError))
                            }
                        }
                    try {
                        awaitClose {}
                    } finally {
                        phaseJob.cancel()
                    }
                } finally {
                    if (callbackRegistered) {
                        runCatching { connectivityManager.unregisterNetworkCallback(connectionCallback) }
                    }
                    session.cancel()
                }
            }

        fun stopTest() {
            activeSessionGuard.current()?.cancelAndClose()
        }

        private fun ProducerScope<SpeedTestProgress>.validateConnection(allowCellular: Boolean): ValidatedNetwork? {
            val defaultNetwork = connectivityManager.activeNetwork
            val capabilities = defaultNetwork?.let(connectivityManager::getNetworkCapabilities)
            if (
                defaultNetwork == null ||
                capabilities == null
            ) {
                trySend(SpeedTestProgress.Failed(context.getString(R.string.speed_test_error_no_internet)))
                return null
            }
            if (
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                trySend(
                    SpeedTestProgress.Failed(
                        context.getString(R.string.speed_test_error_connection_not_validated),
                    ),
                )
                return null
            }
            val startingNetwork =
                networkDataSource.getNetworkInfoFromCallback(
                    capabilities = capabilities,
                    linkProperties = connectivityManager.getLinkProperties(defaultNetwork),
                )
            if (startingNetwork.connectionType == ConnectionType.NONE) {
                trySend(SpeedTestProgress.Failed(context.getString(R.string.speed_test_error_no_internet)))
                return null
            }
            val connectionInfo = startingNetwork.toConnectionInfo()
            if (connectionInfo.connectionType == ConnectionType.CELLULAR && !allowCellular) {
                trySend(SpeedTestProgress.CellularConfirmationRequired(connectionInfo))
                return null
            }
            return ValidatedNetwork(defaultNetwork, startingNetwork)
        }

        private suspend fun SpeedTestSession.runLatencyPhase(network: Network) {
            if (hasEnded) return

            val result = latencyMeasurer.measureLatency(network)
            if (hasEnded) return
            if (result == null) {
                failAndClose(context.getString(R.string.speed_test_error_timeout))
                return
            }

            latestRttMs = result.pingMs
            latestRttVarMs = result.jitterMs
            scope.trySend(SpeedTestProgress.PingPhase(result.pingMs, result.jitterMs))
        }

        private inner class SpeedTestSession(
            val connectionInfo: SpeedTestConnectionInfo,
            startingDefaultNetwork: Network,
            val scope: ProducerScope<SpeedTestProgress>,
        ) {
            private val networkIdentityLock = DefaultNetworkIdentityLock(startingDefaultNetwork)
            private val networkBoundHttpClient =
                HttpClientFactory
                    .createHttpClient()
                    .newBuilder()
                    .socketFactory(startingDefaultNetwork.socketFactory)
                    .dns(
                        object : Dns {
                            override fun lookup(hostname: String): List<InetAddress> =
                                startingDefaultNetwork.getAllByName(hostname).toList()
                        },
                    ).build()
            var downloadMbps = 0.0
            var uploadMbps = 0.0
            var latestRttMs = 0
            var latestRttVarMs: Int? = null
            var serverName: String? = null
            var serverLocation: String? = null
            val downloadStartTime: Long = System.nanoTime()
            var uploadStartTime = 0L
            private var activeTest: NdtTest? = null
            var hasEnded = false
                private set

            fun failAndClose(message: String) {
                if (!endSession()) return
                scope.trySend(SpeedTestProgress.Failed(message))
                scope.channel.close()
            }

            fun cancelAndClose() {
                if (endSession()) scope.channel.close()
            }

            fun cancel() {
                endSession()
            }

            private fun endSession(): Boolean {
                val testToStop =
                    synchronized(testLock) {
                        if (hasEnded) return false
                        hasEnded = true
                        activeSessionGuard.release(this)
                        activeTest.also { activeTest = null }
                    }
                runCatching { testToStop?.stopTest() }
                runCatching { networkBoundHttpClient.dispatcher.cancelAll() }
                runCatching { networkBoundHttpClient.connectionPool.evictAll() }
                return true
            }

            fun applyServerMetadata(clientResponse: ClientResponse?) {
                parseServerMetadata(clientResponse)?.let { metadata ->
                    metadata.name?.let { serverName = it }
                    metadata.location?.let { serverLocation = it }
                }
            }

            fun updateRttFromTcpInfo(tcpInfo: net.measurementlab.ndt7.android.models.TcpInfo?) {
                if (tcpInfo != null && tcpInfo.rtt > 0) {
                    latestRttMs = (tcpInfo.rtt / 1000).toInt()
                    latestRttVarMs = (tcpInfo.rttVar / 1000).toInt()
                }
            }

            private fun failConnectionChanged() {
                failAndClose(context.getString(R.string.speed_test_error_connection_changed))
            }

            private fun failNoInternet() {
                failAndClose(context.getString(R.string.speed_test_error_no_internet))
            }

            private fun handleNetworkLockFailure(failure: NetworkLockFailure?) {
                when (failure) {
                    NetworkLockFailure.CONNECTION_CHANGED -> failConnectionChanged()
                    NetworkLockFailure.NO_INTERNET -> failNoInternet()
                    null -> Unit
                }
            }

            fun verifyCurrentDefaultNetwork(network: Network?) {
                handleNetworkLockFailure(networkIdentityLock.failureForCurrentDefault(network))
            }

            fun startNdtTest(testType: NdtTest.TestType): Throwable? {
                val test = createNdtTest()
                return synchronized(testLock) {
                    verifyCurrentDefaultNetwork(connectivityManager.activeNetwork)
                    if (hasEnded || !activeSessionGuard.isActive(this)) {
                        null
                    } else {
                        activeTest = test
                        runCatching { test.startTest(testType) }.exceptionOrNull()
                    }
                }
            }

            fun createConnectionCallback(): ConnectivityManager.NetworkCallback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        handleNetworkLockFailure(networkIdentityLock.failureForAvailable(network))
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        handleNetworkLockFailure(
                            networkIdentityLock.failureForCapabilities(network, capabilities),
                        )
                    }

                    override fun onLost(network: Network) {
                        handleNetworkLockFailure(networkIdentityLock.failureForLost(network))
                    }
                }

            fun createNdtTest(): NdtTest =
                object : NdtTest(networkBoundHttpClient) {
                    override fun onDownloadProgress(clientResponse: ClientResponse) {
                        applyServerMetadata(clientResponse)
                        val mbps = DataConverter.convertToMbps(clientResponse)
                        downloadMbps = mbps
                        val elapsed = (System.nanoTime() - downloadStartTime).toFloat()
                        val progress = (elapsed / (TEST_DURATION_NS)).coerceAtMost(1f)
                        scope.trySend(SpeedTestProgress.DownloadPhase(mbps, progress))
                    }

                    override fun onMeasurementDownloadProgress(measurement: Measurement) {
                        measurement.connectionInfo?.server?.takeIf { it.isNotBlank() }?.let { host ->
                            serverName = host
                        }
                        updateRttFromTcpInfo(measurement.tcpInfo)
                    }

                    override fun onUploadProgress(clientResponse: ClientResponse) {
                        applyServerMetadata(clientResponse)
                        val mbps = DataConverter.convertToMbps(clientResponse)
                        uploadMbps = mbps
                        if (uploadStartTime == 0L) uploadStartTime = System.nanoTime()
                        val elapsed = (System.nanoTime() - uploadStartTime).toFloat()
                        val progress = (elapsed / (TEST_DURATION_NS)).coerceAtMost(1f)
                        scope.trySend(SpeedTestProgress.UploadPhase(mbps, progress))
                    }

                    override fun onMeasurementUploadProgress(measurement: Measurement) {
                        updateRttFromTcpInfo(measurement.tcpInfo)
                    }

                    override fun onFinished(
                        clientResponse: ClientResponse?,
                        error: Throwable?,
                        testType: TestType,
                    ) {
                        if (error != null) {
                            failAndClose(mapError(error))
                            return
                        }

                        if (testType == TestType.DOWNLOAD) {
                            clientResponse?.let { downloadMbps = DataConverter.convertToMbps(it) }
                            scope.trySend(SpeedTestProgress.DownloadPhase(downloadMbps, 1f))
                            startNdtTest(TestType.UPLOAD)?.let { startError ->
                                failAndClose(mapError(startError))
                            }
                            return
                        }

                        clientResponse?.let { uploadMbps = DataConverter.convertToMbps(it) }
                        applyServerMetadata(clientResponse)
                        if (!endSession()) return
                        // Upload finished — all phases completed successfully.
                        scope.trySend(SpeedTestProgress.UploadPhase(uploadMbps, 1f))
                        scope.trySend(
                            SpeedTestProgress.Completed(
                                downloadMbps = downloadMbps,
                                uploadMbps = uploadMbps,
                                pingMs = latestRttMs,
                                jitterMs = latestRttVarMs,
                                serverName = serverName,
                                serverLocation = serverLocation,
                                connectionInfo = connectionInfo,
                            ),
                        )
                        scope.channel.close()
                    }
                }
        }

        private fun mapError(error: Throwable): String {
            val message = error.message.orEmpty()
            return when {
                error is SocketTimeoutException || message.contains("timeout", ignoreCase = true) -> {
                    context.getString(R.string.speed_test_error_timeout)
                }

                error is UnknownHostException ||
                    message.contains("unable to resolve host", ignoreCase = true) ||
                    message.contains("unreachable", ignoreCase = true) ||
                    message.contains("failed to connect", ignoreCase = true) -> {
                    context.getString(R.string.speed_test_error_server_unreachable)
                }

                message.contains("network", ignoreCase = true) ||
                    message.contains("internet", ignoreCase = true) ||
                    message.contains("offline", ignoreCase = true) -> {
                    context.getString(R.string.speed_test_error_no_internet)
                }

                else -> {
                    context.getString(R.string.speed_test_error_generic)
                }
            }
        }

        private data class ValidatedNetwork(
            val network: Network,
            val info: NetworkDataSource.NetworkInfo,
        )

        private fun NetworkDataSource.NetworkInfo.toConnectionInfo(): SpeedTestConnectionInfo {
            val subtype =
                when (connectionType) {
                    ConnectionType.WIFI -> wifiStandard
                    else -> networkSubtype
                }
            return SpeedTestConnectionInfo(
                connectionType = connectionType,
                networkSubtype = subtype,
                signalDbm = signalDbm,
            )
        }

        companion object {
            private const val TEST_DURATION_NS = 10_000_000_000f // ~10 seconds per phase
        }
    }

internal fun parseServerMetadata(clientResponse: ClientResponse?): ServerMetadata? {
    val serverName = clientResponse?.origin.toServerMetadataValue(PROTOCOL_ORIGINS)
    val serverLocation = clientResponse?.test.toServerMetadataValue(PROTOCOL_TEST_TYPES)
    if (serverName == null && serverLocation == null) return null
    return ServerMetadata(
        name = serverName,
        location = serverLocation?.takeUnless { it.equals(serverName, ignoreCase = true) },
    )
}

private fun String?.toServerMetadataValue(protocolValues: Set<String>): String? {
    val trimmed = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (protocolValues.any { it.equals(trimmed, ignoreCase = true) }) return null
    return runCatching { URI(trimmed).host?.takeIf { it.isNotBlank() } ?: trimmed }
        .getOrDefault(trimmed)
}

internal data class ServerMetadata(
    val name: String?,
    val location: String?,
)

internal class ActiveSpeedTestSessionGuard<T : Any> {
    private val lock = Any()
    private var active: T? = null

    fun tryActivate(candidate: T): Boolean =
        synchronized(lock) {
            if (active != null) {
                false
            } else {
                active = candidate
                true
            }
        }

    fun isActive(candidate: T): Boolean = synchronized(lock) { active === candidate }

    fun release(candidate: T): Boolean =
        synchronized(lock) {
            if (active !== candidate) {
                false
            } else {
                active = null
                true
            }
        }

    fun current(): T? = synchronized(lock) { active }
}

private val PROTOCOL_ORIGINS = setOf("client", "server")
private val PROTOCOL_TEST_TYPES = setOf("download", "upload", "download_and_upload")

internal enum class NetworkLockFailure {
    CONNECTION_CHANGED,
    NO_INTERNET,
}

internal class DefaultNetworkIdentityLock(
    private val lockedNetwork: Network,
) {
    fun failureForCurrentDefault(network: Network?): NetworkLockFailure? =
        when {
            network == null -> NetworkLockFailure.NO_INTERNET
            network != lockedNetwork -> NetworkLockFailure.CONNECTION_CHANGED
            else -> null
        }

    fun failureForAvailable(network: Network): NetworkLockFailure? =
        if (network == lockedNetwork) null else NetworkLockFailure.CONNECTION_CHANGED

    fun failureForCapabilities(
        network: Network,
        capabilities: NetworkCapabilities,
    ): NetworkLockFailure? =
        when {
            network != lockedNetwork -> {
                NetworkLockFailure.CONNECTION_CHANGED
            }

            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> {
                NetworkLockFailure.NO_INTERNET
            }

            else -> {
                null
            }
        }

    fun failureForLost(network: Network): NetworkLockFailure? =
        if (network == lockedNetwork) NetworkLockFailure.NO_INTERNET else null
}
