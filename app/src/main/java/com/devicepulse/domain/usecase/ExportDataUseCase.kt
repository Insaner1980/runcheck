package com.devicepulse.domain.usecase

import com.devicepulse.data.db.dao.BatteryReadingDao
import com.devicepulse.data.db.dao.NetworkReadingDao
import com.devicepulse.data.db.dao.StorageReadingDao
import com.devicepulse.data.db.dao.ThermalReadingDao
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Exports stored device readings as CSV strings.
 * Timestamps are formatted as ISO 8601 in the device's local timezone.
 */
class ExportDataUseCase @Inject constructor(
    private val batteryReadingDao: BatteryReadingDao,
    private val networkReadingDao: NetworkReadingDao,
    private val thermalReadingDao: ThermalReadingDao,
    private val storageReadingDao: StorageReadingDao
) {

    private val isoFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())

    private fun formatTimestamp(epochMs: Long): String =
        isoFormatter.format(Instant.ofEpochMilli(epochMs))

    /** Escapes a CSV field value, wrapping in quotes if it contains commas or quotes. */
    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /** Returns a CSV string of all battery readings. */
    suspend fun exportBatteryCsv(): String {
        val readings = batteryReadingDao.getAll()
        val sb = StringBuilder()
        sb.appendLine("timestamp,level,voltage_mv,temperature_c,current_ma,current_confidence,status,plug_type,health,cycle_count,health_pct")
        for (r in readings) {
            sb.appendLine(
                "${formatTimestamp(r.timestamp)},${r.level},${r.voltageMv},${r.temperatureC}," +
                    "${r.currentMa ?: ""},${escapeCsv(r.currentConfidence)},${escapeCsv(r.status)}," +
                    "${escapeCsv(r.plugType)},${escapeCsv(r.health)},${r.cycleCount ?: ""},${r.healthPct ?: ""}"
            )
        }
        return sb.toString()
    }

    /** Returns a CSV string of all network readings. */
    suspend fun exportNetworkCsv(): String {
        val readings = networkReadingDao.getAll()
        val sb = StringBuilder()
        sb.appendLine("timestamp,type,signal_dbm,wifi_speed_mbps,wifi_frequency,carrier,network_subtype,latency_ms")
        for (r in readings) {
            sb.appendLine(
                "${formatTimestamp(r.timestamp)},${escapeCsv(r.type)},${r.signalDbm ?: ""}," +
                    "${r.wifiSpeedMbps ?: ""},${r.wifiFrequency ?: ""},${escapeCsv(r.carrier)}," +
                    "${escapeCsv(r.networkSubtype)},${r.latencyMs ?: ""}"
            )
        }
        return sb.toString()
    }

    /** Returns a CSV string of all thermal readings. */
    suspend fun exportThermalCsv(): String {
        val readings = thermalReadingDao.getAll()
        val sb = StringBuilder()
        sb.appendLine("timestamp,battery_temp_c,cpu_temp_c,thermal_status,throttling")
        for (r in readings) {
            sb.appendLine(
                "${formatTimestamp(r.timestamp)},${r.batteryTempC},${r.cpuTempC ?: ""}," +
                    "${r.thermalStatus},${r.throttling}"
            )
        }
        return sb.toString()
    }

    /** Returns a CSV string of all storage readings. */
    suspend fun exportStorageCsv(): String {
        val readings = storageReadingDao.getAll()
        val sb = StringBuilder()
        sb.appendLine("timestamp,total_bytes,available_bytes,apps_bytes,media_bytes")
        for (r in readings) {
            sb.appendLine(
                "${formatTimestamp(r.timestamp)},${r.totalBytes},${r.availableBytes}," +
                    "${r.appsBytes},${r.mediaBytes}"
            )
        }
        return sb.toString()
    }

    /** Returns a map of filename to CSV content for all reading types. */
    suspend fun exportAllCsv(): Map<String, String> = mapOf(
        "devicepulse_battery.csv" to exportBatteryCsv(),
        "devicepulse_network.csv" to exportNetworkCsv(),
        "devicepulse_thermal.csv" to exportThermalCsv(),
        "devicepulse_storage.csv" to exportStorageCsv()
    )
}
