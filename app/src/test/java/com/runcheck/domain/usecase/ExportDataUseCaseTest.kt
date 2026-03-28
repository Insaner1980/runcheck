package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.FileExportRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExportDataUseCaseTest {
    private lateinit var useCase: ExportDataUseCase
    private lateinit var batteryRepository: BatteryRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var thermalRepository: ThermalRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var fileExportRepository: FileExportRepository
    private lateinit var proStatusProvider: ProStatusProvider
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        batteryRepository = mockk()
        networkRepository = mockk()
        thermalRepository = mockk()
        storageRepository = mockk()
        fileExportRepository = mockk()
        proStatusProvider = mockk()
        userPreferencesRepository = mockk()

        every { proStatusProvider.isPro() } returns true
        every { userPreferencesRepository.getPreferences() } returns
            flowOf(
                UserPreferences(dataRetention = DataRetention.FOREVER),
            )

        // Default empty data
        coEvery { batteryRepository.getAllReadings() } returns emptyList()
        coEvery { networkRepository.getAllReadings() } returns emptyList()
        coEvery { thermalRepository.getAllReadings() } returns emptyList()
        coEvery { storageRepository.getAllReadings() } returns emptyList()

        useCase =
            ExportDataUseCase(
                batteryRepository = batteryRepository,
                networkRepository = networkRepository,
                thermalRepository = thermalRepository,
                storageRepository = storageRepository,
                fileExportRepository = fileExportRepository,
                proStatusProvider = proStatusProvider,
                userPreferencesRepository = userPreferencesRepository,
            )
    }

    // --- CSV escaping tests (via battery export) ---

    @Test
    fun `CSV escaping - value with comma is quoted`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(
                    BatteryReading(
                        timestamp = 1_700_000_000_000L,
                        level = 80,
                        voltageMv = 4000,
                        temperatureC = 30f,
                        currentMa = -400,
                        currentConfidence = "HIGH,MEDIUM", // contains comma
                        status = "DISCHARGING",
                        plugType = "NONE",
                        health = "GOOD",
                        cycleCount = null,
                        healthPct = null,
                    ),
                )

            val csv = useCase.exportBatteryCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            // "HIGH,MEDIUM" should be wrapped in quotes
            assertTrue(
                "Value with comma should be quoted: $dataLine",
                dataLine.contains("\"HIGH,MEDIUM\""),
            )
        }

    @Test
    fun `CSV escaping - value with quote is double-quoted`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(
                    BatteryReading(
                        timestamp = 1_700_000_000_000L,
                        level = 80,
                        voltageMv = 4000,
                        temperatureC = 30f,
                        currentMa = -400,
                        currentConfidence = "says \"hello\"", // contains quotes
                        status = "DISCHARGING",
                        plugType = "NONE",
                        health = "GOOD",
                        cycleCount = null,
                        healthPct = null,
                    ),
                )

            val csv = useCase.exportBatteryCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            // Quotes inside should be doubled and whole value quoted
            assertTrue(
                "Value with quotes should be escaped: $dataLine",
                dataLine.contains("\"says \"\"hello\"\"\""),
            )
        }

    @Test
    fun `CSV escaping - value with newline is quoted`() =
        runTest {
            coEvery { networkRepository.getAllReadings() } returns
                listOf(
                    NetworkReading(
                        timestamp = 1_700_000_000_000L,
                        type = "WIFI",
                        signalDbm = -50,
                        wifiSpeedMbps = 100,
                        wifiFrequency = 5000,
                        carrier = "Test\nCarrier", // contains newline
                        networkSubtype = null,
                        latencyMs = 20,
                    ),
                )

            val csv = useCase.exportNetworkCsv()

            // The carrier value should be wrapped in quotes
            assertTrue(
                "Value with newline should be quoted",
                csv.contains("\"Test\nCarrier\""),
            )
        }

    // --- Empty data tests ---

    @Test
    fun `empty battery data produces header row only`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns emptyList()

            val csv = useCase.exportBatteryCsv()
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(1, lines.size)
            assertTrue(lines[0].startsWith("timestamp,"))
        }

    @Test
    fun `empty network data produces header row only`() =
        runTest {
            coEvery { networkRepository.getAllReadings() } returns emptyList()

            val csv = useCase.exportNetworkCsv()
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(1, lines.size)
            assertTrue(lines[0].startsWith("timestamp,"))
        }

    @Test
    fun `empty thermal data produces header row only`() =
        runTest {
            coEvery { thermalRepository.getAllReadings() } returns emptyList()

            val csv = useCase.exportThermalCsv()
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(1, lines.size)
            assertTrue(lines[0].startsWith("timestamp,"))
        }

    @Test
    fun `empty storage data produces header row only`() =
        runTest {
            coEvery { storageRepository.getAllReadings() } returns emptyList()

            val csv = useCase.exportStorageCsv()
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(1, lines.size)
            assertTrue(lines[0].startsWith("timestamp,"))
        }

    // --- Normal export tests ---

    @Test
    fun `battery export has correct column count`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(
                    BatteryReading(
                        timestamp = 1_700_000_000_000L,
                        level = 80,
                        voltageMv = 4200,
                        temperatureC = 32.5f,
                        currentMa = -450,
                        currentConfidence = "HIGH",
                        status = "DISCHARGING",
                        plugType = "NONE",
                        health = "GOOD",
                        cycleCount = 150,
                        healthPct = 95,
                    ),
                )

            val csv = useCase.exportBatteryCsv()
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(2, lines.size) // header + 1 data row

            // Header has 11 columns
            val headerCols = lines[0].split(",")
            assertEquals(11, headerCols.size)

            // Data row should also have 11 columns
            val dataCols = lines[1].split(",")
            assertEquals(11, dataCols.size)
        }

    @Test
    fun `battery export formats values correctly`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(
                    BatteryReading(
                        timestamp = 1_700_000_000_000L,
                        level = 80,
                        voltageMv = 4200,
                        temperatureC = 32.5f,
                        currentMa = -450,
                        currentConfidence = "HIGH",
                        status = "DISCHARGING",
                        plugType = "NONE",
                        health = "GOOD",
                        cycleCount = 150,
                        healthPct = 95,
                    ),
                )

            val csv = useCase.exportBatteryCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            assertTrue("Should contain level 80", dataLine.contains(",80,"))
            assertTrue("Should contain voltage 4200", dataLine.contains(",4200,"))
            assertTrue("Should contain temp 32.5", dataLine.contains(",32.5,"))
            assertTrue("Should contain current -450", dataLine.contains(",-450,"))
            assertTrue("Should contain cycle count 150", dataLine.contains(",150,"))
            assertTrue("Should contain health pct 95", dataLine.contains(",95"))
        }

    @Test
    fun `battery export handles null optional fields as empty`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(
                    BatteryReading(
                        timestamp = 1_700_000_000_000L,
                        level = 80,
                        voltageMv = 4200,
                        temperatureC = 32.5f,
                        currentMa = null,
                        currentConfidence = "UNAVAILABLE",
                        status = "DISCHARGING",
                        plugType = "NONE",
                        health = "GOOD",
                        cycleCount = null,
                        healthPct = null,
                    ),
                )

            val csv = useCase.exportBatteryCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            // null fields should produce empty strings between commas
            // currentMa is null -> ",,"
            // cycleCount is null -> ",,"
            // healthPct is null -> trailing empty
            assertTrue("Null currentMa should be empty", dataLine.contains(",32.5,,"))
        }

    @Test
    fun `network export has correct header`() =
        runTest {
            val csv = useCase.exportNetworkCsv()
            val header = csv.lines().first()

            assertEquals(
                "timestamp,type,signal_dbm,wifi_speed_mbps,wifi_frequency,carrier,network_subtype,latency_ms",
                header,
            )
        }

    @Test
    fun `thermal export has correct header`() =
        runTest {
            val csv = useCase.exportThermalCsv()
            val header = csv.lines().first()

            assertEquals(
                "timestamp,battery_temp_c,cpu_temp_c,thermal_status,throttling",
                header,
            )
        }

    @Test
    fun `storage export has correct header`() =
        runTest {
            val csv = useCase.exportStorageCsv()
            val header = csv.lines().first()

            assertEquals(
                "timestamp,total_bytes,available_bytes,apps_bytes,media_bytes",
                header,
            )
        }

    // --- exportAllCsv tests ---

    @Test
    fun `exportAllCsv returns 4 entries`() =
        runTest {
            val result = useCase.exportAllCsv()

            assertEquals(4, result.size)
            assertTrue(result.containsKey("runcheck_battery.csv"))
            assertTrue(result.containsKey("runcheck_network.csv"))
            assertTrue(result.containsKey("runcheck_thermal.csv"))
            assertTrue(result.containsKey("runcheck_storage.csv"))
        }

    @Test
    fun `exportAllCsv values are non-empty strings`() =
        runTest {
            val result = useCase.exportAllCsv()

            result.values.forEach { csv ->
                assertTrue("Each CSV should have at least a header", csv.isNotBlank())
            }
        }

    // --- Pro gate tests ---

    @Test(expected = IllegalStateException::class)
    fun `battery export throws for non-Pro user`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase.exportBatteryCsv()
        }

    @Test(expected = IllegalStateException::class)
    fun `network export throws for non-Pro user`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase.exportNetworkCsv()
        }

    @Test(expected = IllegalStateException::class)
    fun `thermal export throws for non-Pro user`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase.exportThermalCsv()
        }

    @Test(expected = IllegalStateException::class)
    fun `storage export throws for non-Pro user`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase.exportStorageCsv()
        }

    // --- Timestamp formatting ---

    @Test
    fun `timestamps are formatted as ISO 8601`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(
                    BatteryReading(
                        timestamp = 1_700_000_000_000L,
                        level = 80,
                        voltageMv = 4200,
                        temperatureC = 30f,
                        currentMa = null,
                        currentConfidence = "HIGH",
                        status = "DISCHARGING",
                        plugType = "NONE",
                        health = "GOOD",
                        cycleCount = null,
                        healthPct = null,
                    ),
                )

            val csv = useCase.exportBatteryCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            // ISO 8601 timestamp should contain 'T' and timezone offset (+ or Z)
            val timestampField = dataLine.split(",").first()
            assertTrue(
                "Timestamp should be ISO 8601 format: $timestampField",
                timestampField.contains("T"),
            )
        }

    // --- Thermal status formatting ---

    @Test
    fun `thermal status integer is formatted as enum name`() =
        runTest {
            coEvery { thermalRepository.getAllReadings() } returns
                listOf(
                    ThermalReading(
                        timestamp = 1_700_000_000_000L,
                        batteryTempC = 35f,
                        cpuTempC = 60f,
                        thermalStatus = 3, // SEVERE (0=NONE, 1=LIGHT, 2=MODERATE, 3=SEVERE)
                        throttling = true,
                    ),
                )

            val csv = useCase.exportThermalCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            assertTrue(
                "Thermal status 3 should be formatted as SEVERE: $dataLine",
                dataLine.contains("SEVERE"),
            )
        }

    @Test
    fun `storage export formats bytes correctly`() =
        runTest {
            coEvery { storageRepository.getAllReadings() } returns
                listOf(
                    StorageReading(
                        timestamp = 1_700_000_000_000L,
                        totalBytes = 128_000_000_000L,
                        availableBytes = 64_000_000_000L,
                        appsBytes = 20_000_000_000L,
                        mediaBytes = 30_000_000_000L,
                    ),
                )

            val csv = useCase.exportStorageCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            assertTrue("Should contain total bytes", dataLine.contains("128000000000"))
            assertTrue("Should contain available bytes", dataLine.contains("64000000000"))
            assertTrue("Should contain apps bytes", dataLine.contains("20000000000"))
            assertTrue("Should contain media bytes", dataLine.contains("30000000000"))
        }

    // --- CSV escaping edge case: carriage return ---

    @Test
    fun `CSV escaping - value with carriage return is quoted`() =
        runTest {
            coEvery { networkRepository.getAllReadings() } returns
                listOf(
                    NetworkReading(
                        timestamp = 1_700_000_000_000L,
                        type = "WIFI",
                        signalDbm = -50,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Test\rCarrier", // contains carriage return
                        networkSubtype = null,
                        latencyMs = null,
                    ),
                )

            val csv = useCase.exportNetworkCsv()

            assertTrue(
                "Value with carriage return should be quoted",
                csv.contains("\"Test\rCarrier\""),
            )
        }

    @Test
    fun `null values in CSV are empty strings`() =
        runTest {
            coEvery { networkRepository.getAllReadings() } returns
                listOf(
                    NetworkReading(
                        timestamp = 1_700_000_000_000L,
                        type = "WIFI",
                        signalDbm = null,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = null,
                        networkSubtype = null,
                        latencyMs = null,
                    ),
                )

            val csv = useCase.exportNetworkCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }
            val cols = dataLine.split(",")

            // 8 columns: timestamp, type, signalDbm, wifiSpeedMbps, wifiFrequency, carrier, networkSubtype, latencyMs
            assertEquals(8, cols.size)
            // null String fields should be empty
            assertEquals("", cols[5]) // carrier
            assertEquals("", cols[6]) // networkSubtype
        }
}
