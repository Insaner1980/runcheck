package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalStatusPersistence
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.FileExportRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.util.AppDispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportDataUseCase
    @Inject
    constructor(
        private val batteryRepository: BatteryRepository,
        private val networkRepository: NetworkRepository,
        private val thermalRepository: ThermalRepository,
        private val storageRepository: StorageRepository,
        private val fileExportRepository: FileExportRepository,
        private val proStatusProvider: ProStatusProvider,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val dispatchers: AppDispatchers,
    ) {
        private data class CsvColumn<T>(
            val header: String,
            val value: (T) -> String,
        )

        private val isoFormatter: DateTimeFormatter =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())

        private fun formatTimestamp(epochMs: Long): String = isoFormatter.format(Instant.ofEpochMilli(epochMs))

        private val batteryColumns =
            listOf(
                CsvColumn<BatteryReading>("timestamp") { formatTimestamp(it.timestamp) },
                CsvColumn("level") { it.level.toString() },
                CsvColumn("voltage_mv") { it.voltageMv.toString() },
                CsvColumn("temperature_c") { it.temperatureC.toString() },
                CsvColumn("current_ma") { it.currentMa?.toString().orEmpty() },
                CsvColumn("current_confidence") { it.currentConfidence },
                CsvColumn("status") { it.status },
                CsvColumn("plug_type") { it.plugType },
                CsvColumn("health") { it.health },
                CsvColumn("cycle_count") { it.cycleCount?.toString().orEmpty() },
                CsvColumn("health_pct") { it.healthPct?.toString().orEmpty() },
            )

        private val networkColumns =
            listOf(
                CsvColumn<NetworkReading>("timestamp") { formatTimestamp(it.timestamp) },
                CsvColumn("type") { it.type },
                CsvColumn("signal_dbm") { it.signalDbm?.toString().orEmpty() },
                CsvColumn("wifi_speed_mbps") { it.wifiSpeedMbps?.toString().orEmpty() },
                CsvColumn("wifi_frequency") { it.wifiFrequency?.toString().orEmpty() },
                CsvColumn("carrier") { it.carrier.orEmpty() },
                CsvColumn("network_subtype") { it.networkSubtype.orEmpty() },
                CsvColumn("latency_ms") { it.latencyMs?.toString().orEmpty() },
            )

        private val thermalColumns =
            listOf(
                CsvColumn<ThermalReading>("timestamp") { formatTimestamp(it.timestamp) },
                CsvColumn("battery_temp_c") { it.batteryTempC.toString() },
                CsvColumn("cpu_temp_c") { it.cpuTempC?.toString().orEmpty() },
                CsvColumn("thermal_status") { formatThermalStatus(it.thermalStatus) },
                CsvColumn("throttling") { it.throttling.toString() },
            )

        private val storageColumns =
            listOf(
                CsvColumn<StorageReading>("timestamp") { formatTimestamp(it.timestamp) },
                CsvColumn("total_bytes") { it.totalBytes.toString() },
                CsvColumn("available_bytes") { it.availableBytes.toString() },
                CsvColumn("apps_bytes") { it.appsBytes?.toString().orEmpty() },
                CsvColumn("media_bytes") { it.mediaBytes?.toString().orEmpty() },
            )

        private fun escapeCsv(value: String?): String {
            if (value == null) return ""
            return if (
                value.contains(',') ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r')
            ) {
                "\"${value.replace("\"", "\"\"")}\""
            } else {
                value
            }
        }

        private fun requirePro() {
            check(proStatusProvider.isPro()) { "CSV export requires runcheck Pro" }
        }

        private suspend fun exportCutoff(): Long? {
            val retention = userPreferencesRepository.getPreferences().first().dataRetention
            val now = System.currentTimeMillis()
            return retention.durationMillis?.let { now - it }
        }

        private fun <T> List<T>.filterByRetention(
            cutoff: Long?,
            timestampOf: (T) -> Long,
        ): List<T> = if (cutoff == null) this else filter { timestampOf(it) >= cutoff }

        private fun formatThermalStatus(status: Int): String =
            ThermalStatusPersistence.fromCode(status)?.let(ThermalStatusPersistence::toId) ?: status.toString()

        private fun <T> buildCsv(
            columns: List<CsvColumn<T>>,
            rows: List<T>,
        ): String =
            buildString {
                appendLine(columns.joinToString(",") { it.header })
                for (row in rows) {
                    appendLine(columns.joinToString(",") { column -> escapeCsv(column.value(row)) })
                }
            }

        suspend fun exportBatteryCsv(): String {
            requirePro()
            val cutoff = exportCutoff()
            val readings =
                batteryRepository
                    .getAllReadings()
                    .filterByRetention(cutoff) { it.timestamp }
            return buildCsv(batteryColumns, readings)
        }

        suspend fun exportNetworkCsv(): String {
            requirePro()
            val cutoff = exportCutoff()
            val readings =
                networkRepository
                    .getAllReadings()
                    .filterByRetention(cutoff) { it.timestamp }
            return buildCsv(networkColumns, readings)
        }

        suspend fun exportThermalCsv(): String {
            requirePro()
            val cutoff = exportCutoff()
            val readings =
                thermalRepository
                    .getAllReadings()
                    .filterByRetention(cutoff) { it.timestamp }
            return buildCsv(thermalColumns, readings)
        }

        suspend fun exportStorageCsv(): String {
            requirePro()
            val cutoff = exportCutoff()
            val readings =
                storageRepository
                    .getAllReadings()
                    .filterByRetention(cutoff) { it.timestamp }
            return buildCsv(storageColumns, readings)
        }

        suspend fun exportAllCsv(): Map<String, String> =
            withContext(dispatchers.default) {
                mapOf(
                    "runcheck_battery.csv" to exportBatteryCsv(),
                    "runcheck_network.csv" to exportNetworkCsv(),
                    "runcheck_thermal.csv" to exportThermalCsv(),
                    "runcheck_storage.csv" to exportStorageCsv(),
                )
            }

        suspend fun prepareExportShare(): List<String> {
            val files = exportAllCsv()
            requirePro()
            val uris = fileExportRepository.prepareExportShare(files)
            if (!proStatusProvider.isPro()) {
                fileExportRepository.clearPreparedExports()
                error("CSV export requires runcheck Pro")
            }
            return uris
        }
    }
