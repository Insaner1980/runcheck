package com.runcheck.domain.model

fun classifyNetworkSignalQuality(
    dbm: Int?,
    type: ConnectionType,
    networkSubtype: String? = null,
): SignalQuality {
    if (type == ConnectionType.NONE) return SignalQuality.NO_SIGNAL
    if (type == ConnectionType.VPN && dbm == null) return SignalQuality.GOOD
    if (dbm == null) return SignalQuality.NO_SIGNAL

    return when (type) {
        ConnectionType.WIFI -> classifyWifiSignalQuality(dbm)
        ConnectionType.CELLULAR -> classifyCellularSignalQuality(dbm, networkSubtype)
        ConnectionType.VPN -> classifyVpnSignalQuality(dbm)
        ConnectionType.NONE -> SignalQuality.NO_SIGNAL
    }
}

private fun classifyWifiSignalQuality(dbm: Int): SignalQuality =
    when {
        dbm > -50 -> SignalQuality.EXCELLENT
        dbm > -60 -> SignalQuality.GOOD
        dbm > -70 -> SignalQuality.FAIR
        dbm > -80 -> SignalQuality.POOR
        else -> SignalQuality.NO_SIGNAL
    }

// Android AOSP CellSignalStrengthNr default SS-RSRP thresholds:
//   GREAT >= -65, GOOD -80..-66, MODERATE -90..-81, POOR -110..-91
// Android AOSP CellSignalStrengthLte default RSRP thresholds:
//   GREAT >= -98, GOOD -108..-99, MODERATE -118..-109, POOR -128..-119
// Carriers customize via CarrierConfig. We map AOSP 5-level to our
// 5-level enum (EXCELLENT/GOOD/FAIR/POOR/NO_SIGNAL).
private fun classifyCellularSignalQuality(
    dbm: Int,
    networkSubtype: String?,
): SignalQuality {
    val is5g = networkSubtype?.contains("5G") == true
    return if (is5g) {
        when {
            dbm >= -65 -> SignalQuality.EXCELLENT
            dbm >= -80 -> SignalQuality.GOOD
            dbm >= -90 -> SignalQuality.FAIR
            dbm >= -110 -> SignalQuality.POOR
            else -> SignalQuality.NO_SIGNAL
        }
    } else {
        when {
            dbm >= -98 -> SignalQuality.EXCELLENT
            dbm >= -108 -> SignalQuality.GOOD
            dbm >= -118 -> SignalQuality.FAIR
            dbm >= -128 -> SignalQuality.POOR
            else -> SignalQuality.NO_SIGNAL
        }
    }
}

private fun classifyVpnSignalQuality(dbm: Int): SignalQuality =
    when {
        dbm > -80 -> SignalQuality.EXCELLENT
        dbm > -90 -> SignalQuality.GOOD
        dbm > -100 -> SignalQuality.FAIR
        dbm > -110 -> SignalQuality.POOR
        else -> SignalQuality.NO_SIGNAL
    }
