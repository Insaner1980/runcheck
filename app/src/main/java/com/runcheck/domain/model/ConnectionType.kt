package com.runcheck.domain.model

enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    NONE,
}

internal fun decodePersistedConnectionType(raw: String): ConnectionType? =
    ConnectionType.entries.firstOrNull { it.name == raw }
