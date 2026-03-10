package com.devicepulse.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.SignalQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    // Cached WiFi details from dedicated WiFi callback (survives VPN overlay)
    @Volatile
    private var cachedWifiDetails: WifiDetails? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerTelephonyCallback()
            registerWifiCallback()
        }
    }

    fun getNetworkInfo(): Flow<NetworkInfo> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                FLAG_INCLUDE_LOCATION_INFO else 0
        ) {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                trySend(buildNetworkInfo(capabilities))
            }

            override fun onLost(network: Network) {
                trySend(NetworkInfo.disconnected())
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        // Emit initial state
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)
        }
        trySend(
            if (capabilities != null) buildNetworkInfo(capabilities)
            else NetworkInfo.disconnected()
        )

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    private fun buildNetworkInfo(capabilities: NetworkCapabilities): NetworkInfo {
        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        val connectionType = when {
            isWifi -> ConnectionType.WIFI
            isCellular -> ConnectionType.CELLULAR
            else -> ConnectionType.NONE
        }

        // Try NetworkCapabilities first, fall back to specific sources
        var signalDbm: Int? = capabilities.signalStrength.let {
            if (it == Int.MIN_VALUE) null else it
        }
        if (signalDbm == null && isCellular) {
            signalDbm = getCellularSignalDbm()
        }

        val wifiInfo = if (isWifi) getWifiDetails(capabilities) else null

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

    // Cached signal level from TelephonyManager (0-4, matches status bar)
    @Volatile
    private var cachedSignalLevel: Int? = null

    private fun getCellularSignalDbm(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val signalStrength = telephonyManager?.signalStrength ?: return null
            // Cache the level (0-4) that matches the status bar display
            cachedSignalLevel = signalStrength.level

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
                    val d = css.dbm
                    if (d == Int.MIN_VALUE || d == Int.MAX_VALUE) null else d
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
                val ssid = wifiInfo.ssid?.removeSurrounding("\"")
                if (ssid != null && ssid != "<unknown ssid>" && ssid != "Unknown") {
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
        val info = wifiManager.connectionInfo ?: return null
        val rssi = info.rssi
        return WifiDetails(
            ssid = info.ssid?.removeSurrounding("\"") ?: "Unknown",
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
    private fun registerWifiCallback() {
        try {
            val wifiRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            val wifiCallback = object : ConnectivityManager.NetworkCallback(
                FLAG_INCLUDE_LOCATION_INFO
            ) {
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    val wifiInfo = capabilities.transportInfo as? WifiInfo ?: return
                    val ssid = wifiInfo.ssid?.removeSurrounding("\"")
                    val rssi = wifiInfo.rssi
                    cachedWifiDetails = WifiDetails(
                        ssid = ssid ?: "Unknown",
                        speedMbps = wifiInfo.linkSpeed,
                        frequencyMhz = wifiInfo.frequency,
                        rssi = if (rssi != -127 && rssi != 0) rssi else null
                    )
                }

                override fun onLost(network: Network) {
                    cachedWifiDetails = null
                }
            }
            connectivityManager.registerNetworkCallback(wifiRequest, wifiCallback)
        } catch (_: Exception) {
            // Ignore if registration fails
        }
    }

    // Cached network type name from TelephonyCallback (API 31+)
    @Volatile
    private var cachedNetworkTypeName: String = "Unknown"

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback() {
        telephonyManager ?: return
        try {
            val executor = Executors.newSingleThreadExecutor()
            val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                override fun onDisplayInfoChanged(displayInfo: TelephonyDisplayInfo) {
                    cachedNetworkTypeName = mapDisplayInfo(displayInfo)
                }
            }
            telephonyManager.registerTelephonyCallback(executor, callback)
        } catch (_: Exception) {
            // Ignore if registration fails
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
        return CellularDetails(
            carrier = telephonyManager?.networkOperatorName ?: "Unknown",
            networkType = cachedNetworkTypeName
        )
    }

    @Suppress("DEPRECATION")
    private fun classifySignal(dbm: Int?, type: ConnectionType): SignalQuality {
        if (type == ConnectionType.NONE) return SignalQuality.NO_SIGNAL

        // WiFi: use WifiManager.calculateSignalLevel (matches system WiFi indicator)
        if (type == ConnectionType.WIFI && dbm != null) {
            val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiManager.calculateSignalLevel(dbm)
            } else {
                WifiManager.calculateSignalLevel(dbm, 5)
            }
            return when (level) {
                4 -> SignalQuality.EXCELLENT
                3 -> SignalQuality.GOOD
                2 -> SignalQuality.FAIR
                1 -> SignalQuality.POOR
                else -> SignalQuality.NO_SIGNAL
            }
        }

        // Cellular: use Android's own signal level (0-4) — matches status bar bars
        cachedSignalLevel?.let { level ->
            return when (level) {
                4 -> SignalQuality.EXCELLENT
                3 -> SignalQuality.GOOD
                2 -> SignalQuality.FAIR
                1 -> SignalQuality.POOR
                else -> SignalQuality.NO_SIGNAL
            }
        }

        // Fallback to dBm thresholds
        if (dbm == null) return SignalQuality.FAIR
        return when {
            dbm >= -65 -> SignalQuality.EXCELLENT
            dbm >= -85 -> SignalQuality.GOOD
            dbm >= -105 -> SignalQuality.FAIR
            dbm >= -120 -> SignalQuality.POOR
            else -> SignalQuality.NO_SIGNAL
        }
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
}
