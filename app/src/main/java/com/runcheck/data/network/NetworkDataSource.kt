package com.runcheck.data.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.location.LocationManager
import android.os.Build
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.PermissionChecker
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.SignalQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val dataSourceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Cached WiFi details from dedicated WiFi callback (survives VPN overlay)
    @Volatile
    private var cachedWifiDetails: WifiDetails? = null

    private val networkInfoFlow: Flow<NetworkInfo> by lazy {
        callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    FLAG_INCLUDE_LOCATION_INFO else 0
            ) {
                override fun onAvailable(network: Network) {
                    emitCurrentNetworkInfo()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    emitCurrentNetworkInfo()
                }

                override fun onLost(network: Network) {
                    emitCurrentNetworkInfo()
                }
            }

            val locationModeReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action != LocationManager.MODE_CHANGED_ACTION) return
                    if (!isLocationEnabled()) {
                        cachedWifiDetails = null
                    }
                    emitCurrentNetworkInfo()
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback)
            ContextCompat.registerReceiver(
                context,
                locationModeReceiver,
                IntentFilter(LocationManager.MODE_CHANGED_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            val wifiCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registerWifiCallback()
            } else {
                null
            }
            val telephonyCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registerTelephonyCallback()
            } else {
                null
            }

            emitCurrentNetworkInfo()

            awaitClose {
                cachedWifiDetails = null
                cachedNetworkTypeName = "Unknown"
                runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                runCatching { context.unregisterReceiver(locationModeReceiver) }
                if (wifiCallback != null) {
                    runCatching { connectivityManager.unregisterNetworkCallback(wifiCallback) }
                }
                if (telephonyCallback != null) {
                    runCatching { telephonyManager?.unregisterTelephonyCallback(telephonyCallback) }
                }
            }
        }.distinctUntilChanged().shareIn(
            scope = dataSourceScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MS),
            replay = 1
        )
    }

    fun getNetworkInfo(): Flow<NetworkInfo> = networkInfoFlow

    fun hasValidatedConnection(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun buildNetworkInfo(capabilities: NetworkCapabilities): NetworkInfo {
        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val canReadWifiDetails = canReadWifiDetails()

        val connectionType = when {
            isWifi -> ConnectionType.WIFI
            isCellular -> ConnectionType.CELLULAR
            else -> ConnectionType.NONE
        }

        // Try NetworkCapabilities first, fall back to specific sources
        val rawSignalDbm = capabilities.signalStrength
        var signalDbm: Int? = rawSignalDbm.takeUnless { it == Int.MIN_VALUE }
        if (signalDbm == null && isCellular) {
            signalDbm = getCellularSignalDbm()
        }

        val wifiInfo = if (isWifi && canReadWifiDetails) getWifiDetails(capabilities) else null

        // For WiFi, use RSSI if NetworkCapabilities didn't provide signal
        if (isWifi && signalDbm == null && wifiInfo != null) {
            signalDbm = wifiInfo.rssi
        }
        val signalQuality = classifySignal(signalDbm, connectionType)
        val cellInfo = if (isCellular) getCellularDetails() else null

        return NetworkInfo(
            connectionType = connectionType,
            signalDbm = signalDbm,
            signalQuality = signalQuality,
            wifiSsid = wifiInfo?.ssid,
            wifiSpeedMbps = wifiInfo?.speedMbps,
            wifiFrequencyMhz = wifiInfo?.frequencyMhz,
            carrier = cellInfo?.carrier,
            networkSubtype = cellInfo?.networkType
        )
    }

    private fun kotlinx.coroutines.channels.ProducerScope<NetworkInfo>.emitCurrentNetworkInfo() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)
        }
        trySend(
            if (capabilities != null) buildNetworkInfo(capabilities)
            else NetworkInfo.disconnected()
        )
    }

    private fun getCellularSignalDbm(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val signalStrength = telephonyManager?.signalStrength ?: return null

            // Detect 5G NR from signal strength classes (no permission needed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasNr = signalStrength.cellSignalStrengths
                    .any { it is CellSignalStrengthNr }
                if (hasNr && !cachedNetworkTypeName.contains("5G")) {
                    cachedNetworkTypeName = "5G"
                }
            }

            // Get the strongest dBm from all cell technologies
            signalStrength.cellSignalStrengths
                .mapNotNull { css ->
                    val signalDbm = css.dbm
                    if (signalDbm == Int.MIN_VALUE || signalDbm == Int.MAX_VALUE) null else signalDbm
                }
                .maxOrNull()
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getWifiDetails(capabilities: NetworkCapabilities): WifiDetails? {
        // API 31+: try transportInfo from capabilities (works when not behind VPN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wifiInfo = capabilities.transportInfo as? WifiInfo
            if (wifiInfo != null) {
                val ssid = normalizeSsid(wifiInfo.ssid)
                if (ssid != null) {
                    val rssi = wifiInfo.rssi
                    return WifiDetails(
                        ssid = ssid,
                        speedMbps = wifiInfo.linkSpeed,
                        frequencyMhz = wifiInfo.frequency,
                        rssi = if (rssi != -127 && rssi != 0) rssi else null
                    )
                }
            }
            // VPN active or SSID not in transportInfo — use cached from WiFi callback
            cachedWifiDetails?.let { return it }
        }
        // Fallback: WifiManager.connectionInfo (pre-API 31)
        val info = wifiManager?.connectionInfo ?: return null
        val rssi = info.rssi
        val ssid = normalizeSsid(info.ssid) ?: return null
        return WifiDetails(
            ssid = ssid,
            speedMbps = info.linkSpeed,
            frequencyMhz = info.frequency,
            rssi = if (rssi != -127 && rssi != 0) rssi else null
        )
    }

    /**
     * Dedicated WiFi callback with FLAG_INCLUDE_LOCATION_INFO and TRANSPORT_WIFI request.
     * This bypasses VPN overlay and delivers real WiFi network info including SSID.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerWifiCallback(): ConnectivityManager.NetworkCallback? {
        return try {
            val wifiRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            object : ConnectivityManager.NetworkCallback(
                FLAG_INCLUDE_LOCATION_INFO
            ) {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    val wifiInfo = capabilities.transportInfo as? WifiInfo ?: return
                    val ssid = normalizeSsid(wifiInfo.ssid) ?: return
                    val rssi = wifiInfo.rssi
                    cachedWifiDetails = WifiDetails(
                        ssid = ssid,
                        speedMbps = wifiInfo.linkSpeed,
                        frequencyMhz = wifiInfo.frequency,
                        rssi = if (rssi != -127 && rssi != 0) rssi else null
                    )
                }

                override fun onLost(network: Network) {
                    cachedWifiDetails = null
                }
            }.also { wifiCallback ->
                connectivityManager.registerNetworkCallback(wifiRequest, wifiCallback)
            }
        } catch (_: Exception) {
            null
        }
    }

    // Cached network type name from TelephonyCallback (API 31+)
    @Volatile
    private var cachedNetworkTypeName: String = "Unknown"

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback(): TelephonyCallback? {
        val manager = telephonyManager ?: return null
        return try {
            object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                    cachedNetworkTypeName = mapDisplayInfo(displayInfo)
                }
            }.also { callback ->
                manager.registerTelephonyCallback(
                    ContextCompat.getMainExecutor(context),
                    callback
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun mapDisplayInfo(info: TelephonyDisplayInfo): String {
        // Check override type first (e.g. 5G NSA shown as 5G icon)
        return when (info.overrideNetworkType) {
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> "5G"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G+"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO -> "4G+"
            TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "4G LTE"
            else -> mapNetworkType(info.networkType)
        }
    }

    private fun mapNetworkType(networkType: Int): String = when (networkType) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_HSPA -> "3G HSPA"
        TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS"
        TelephonyManager.NETWORK_TYPE_EDGE -> "2G EDGE"
        TelephonyManager.NETWORK_TYPE_GPRS -> "2G GPRS"
        else -> "Cellular"
    }

    private fun getCellularDetails(): CellularDetails {
        val runtimeType = telephonyManager?.let { manager ->
            try {
                @Suppress("DEPRECATION")
                mapNetworkType(manager.dataNetworkType)
            } catch (_: SecurityException) {
                "Cellular"
            }
        } ?: "Cellular"

        val carrierName = try {
            telephonyManager?.networkOperatorName?.takeIf { it.isNotBlank() } ?: "Unknown"
        } catch (_: SecurityException) {
            "Unknown"
        }

        return CellularDetails(
            carrier = carrierName,
            networkType = if (cachedNetworkTypeName == "Unknown") runtimeType else cachedNetworkTypeName
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun hasFineLocationPermission(): Boolean {
        return PermissionChecker.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
    }

    private fun canReadWifiDetails(): Boolean = hasFineLocationPermission() && isLocationEnabled()

    private fun normalizeSsid(rawSsid: String?): String? {
        val normalized = rawSsid
            ?.removeSurrounding("\"")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return if (
            normalized.equals("<unknown ssid>", ignoreCase = true) ||
            normalized.equals("unknown ssid", ignoreCase = true) ||
            normalized.equals("unknown", ignoreCase = true)
        ) {
            null
        } else {
            normalized
        }
    }

    @Suppress("DEPRECATION")
    private fun classifySignal(dbm: Int?, type: ConnectionType): SignalQuality {
        if (type == ConnectionType.NONE) return SignalQuality.NO_SIGNAL

        if (dbm != null) {
            return when (type) {
                ConnectionType.WIFI -> when {
                    dbm > -50 -> SignalQuality.EXCELLENT
                    dbm > -60 -> SignalQuality.GOOD
                    dbm > -70 -> SignalQuality.FAIR
                    dbm > -80 -> SignalQuality.POOR
                    else -> SignalQuality.NO_SIGNAL
                }
                ConnectionType.CELLULAR -> when {
                    dbm > -80 -> SignalQuality.EXCELLENT
                    dbm > -90 -> SignalQuality.GOOD
                    dbm > -100 -> SignalQuality.FAIR
                    dbm > -110 -> SignalQuality.POOR
                    else -> SignalQuality.NO_SIGNAL
                }
                ConnectionType.NONE -> SignalQuality.NO_SIGNAL
            }
        }

        return SignalQuality.NO_SIGNAL
    }

    data class NetworkInfo(
        val connectionType: ConnectionType,
        val signalDbm: Int?,
        val signalQuality: SignalQuality,
        val wifiSsid: String? = null,
        val wifiSpeedMbps: Int? = null,
        val wifiFrequencyMhz: Int? = null,
        val carrier: String? = null,
        val networkSubtype: String? = null
    ) {
        companion object {
            fun disconnected() = NetworkInfo(
                connectionType = ConnectionType.NONE,
                signalDbm = null,
                signalQuality = SignalQuality.NO_SIGNAL
            )
        }
    }

    private data class WifiDetails(
        val ssid: String,
        val speedMbps: Int,
        val frequencyMhz: Int,
        val rssi: Int? = null
    )

    private data class CellularDetails(
        val carrier: String,
        val networkType: String
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 0L
    }
}
