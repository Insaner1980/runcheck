package com.runcheck.ui.common

import com.runcheck.domain.model.HealthStatus
import com.runcheck.testutil.findRootDir
import com.runcheck.ui.theme.RuncheckStatusColors
import com.runcheck.ui.theme.forHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class StorageUsagePresentationTest {
    @Test
    fun `severity changes at exact boundaries without rounding`() {
        val cases =
            listOf(
                0f to HealthStatus.HEALTHY,
                25f to HealthStatus.HEALTHY,
                74f to HealthStatus.HEALTHY,
                74.999f to HealthStatus.HEALTHY,
                Math.nextDown(75f) to HealthStatus.HEALTHY,
                75f to HealthStatus.FAIR,
                Math.nextUp(75f) to HealthStatus.FAIR,
                84.9f to HealthStatus.FAIR,
                Math.nextDown(85f) to HealthStatus.FAIR,
                85f to HealthStatus.POOR,
                Math.nextUp(85f) to HealthStatus.POOR,
                94.9f to HealthStatus.POOR,
                Math.nextDown(95f) to HealthStatus.POOR,
                95f to HealthStatus.CRITICAL,
                Math.nextUp(95f) to HealthStatus.CRITICAL,
                100f to HealthStatus.CRITICAL,
            )
        cases.forEach { (value, expected) ->
            assertEquals("used percent $value", expected, StorageUsagePresentation.classify(value))
        }
        assertTrue(Math.nextDown(75f) > 74.999f)
    }

    @Test
    fun `classification does not normalize measurements`() {
        listOf(-1f, Float.NEGATIVE_INFINITY, Float.NaN).forEach {
            assertEquals(HealthStatus.HEALTHY, StorageUsagePresentation.classify(it))
        }
        listOf(101f, Float.POSITIVE_INFINITY).forEach {
            assertEquals(HealthStatus.CRITICAL, StorageUsagePresentation.classify(it))
        }
    }

    @Test
    fun `canonical severity maps to existing theme colors`() {
        val colors = RuncheckStatusColors
        listOf(0f to colors.healthy, 75f to colors.fair, 85f to colors.poor, 95f to colors.critical)
            .forEach { (value, expected) ->
                assertEquals(expected, colors.forHealthStatus(StorageUsagePresentation.classify(value)))
            }
    }

    @Test
    fun `Home and Detail retain their canonical status helper and integer normalization`() {
        // Source contracts verify Compose wiring without claiming a rendered UI test.
        val root = findRootDir().resolve("app/src/main/java/com/runcheck/ui")

        fun source(path: String) = root.resolve(path).readText().replace(Regex("\\s+"), " ")
        assertTrue(
            source("theme/StatusColors.kt").contains(
                "statusColor(StorageUsagePresentation.classify(usedPercent.toFloat()))",
            ),
        )
        assertTrue(
            source("home/HomeStatusTiles.kt").contains(
                "statusColorForStoragePercent( state.storageState.usagePercent .toInt() .coerceIn(0, 100), ).toHealthStatus(statusColors)",
            ),
        )
        val detail = source("storage/StorageDetailScreen.kt")
        assertTrue(detail.contains("val usagePercent = storage.usagePercent.toInt().coerceIn(0, 100)"))
        assertTrue(detail.contains("progressColor = statusColorForStoragePercent(usagePercent)"))
        assertTrue(detail.contains("lineColor = statusColorForStoragePercent(usagePercent)"))
        assertTrue(detail.contains("qualityZones = storageQualityZones(metric)"))
    }
}
