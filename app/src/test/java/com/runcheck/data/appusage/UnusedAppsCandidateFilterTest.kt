package com.runcheck.data.appusage

import com.runcheck.domain.model.UnusedAppError
import com.runcheck.domain.model.UnusedAppsPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant

class UnusedAppsCandidateFilterTest {
    private val now = Instant.parse("2026-04-01T00:00:00Z")

    @Test
    fun `filters system updated-system self recent and recently used packages`() {
        val result =
            filterUnusedApps(
                apps =
                    listOf(
                        app("keep", installedDaysAgo = 100),
                        app("system", installedDaysAgo = 100, system = true),
                        app("updated", installedDaysAgo = 100, updatedSystem = true),
                        app("self", installedDaysAgo = 100),
                        app("recent-install", installedDaysAgo = 10),
                        app("recent-use", installedDaysAgo = 100),
                    ),
                lastUsedAt =
                    mapOf(
                        "keep" to now.minusSeconds(40L * 86_400),
                        "recent-use" to now.minusSeconds(2L * 86_400),
                    ),
                selfPackageName = "self",
                period = UnusedAppsPeriod.DAYS_30,
                observedAt = now,
            )

        assertEquals(listOf("keep"), result.map { it.packageName })
    }

    @Test
    fun `missing usage row remains a candidate without claiming a last use`() {
        val result =
            filterUnusedApps(
                apps = listOf(app("unknown", installedDaysAgo = 100)),
                lastUsedAt = emptyMap(),
                selfPackageName = "self",
                period = UnusedAppsPeriod.DAYS_90,
                observedAt = now,
            )

        assertEquals("unknown", result.single().packageName)
        assertEquals(null, result.single().lastRecordedUse)
    }

    @Test
    fun `selected 30 60 and 90 day periods apply their own install threshold`() {
        val apps = listOf(app("sixty-one-days", installedDaysAgo = 61))

        val days30 =
            filterUnusedApps(apps, emptyMap(), "self", UnusedAppsPeriod.DAYS_30, now)
        val days60 =
            filterUnusedApps(apps, emptyMap(), "self", UnusedAppsPeriod.DAYS_60, now)
        val days90 =
            filterUnusedApps(apps, emptyMap(), "self", UnusedAppsPeriod.DAYS_90, now)

        assertEquals(listOf("sixty-one-days"), days30.map { it.packageName })
        assertEquals(listOf("sixty-one-days"), days60.map { it.packageName })
        assertTrue(days90.isEmpty())
    }

    @Test
    fun `missing app label keeps the package fallback and records partial error`() {
        val unlabeled = app("unlabeled", installedDaysAgo = 100).copy(appLabel = null)

        val candidate =
            filterUnusedApps(
                apps = listOf(unlabeled),
                lastUsedAt = emptyMap(),
                selfPackageName = "self",
                period = UnusedAppsPeriod.DAYS_30,
                observedAt = now,
            ).single()

        assertEquals(null, candidate.appLabel)
        assertEquals(setOf(UnusedAppError.PACKAGE_LABEL), candidate.errors)
    }

    @Test
    fun `storage security failure is reported as a permission partial error`() {
        assertEquals(
            UnusedAppError.STORAGE_PERMISSION,
            storageErrorFor(SecurityException()),
        )
    }

    @Test
    fun `storage io failure is reported as an io partial error`() {
        assertEquals(
            UnusedAppError.STORAGE_IO,
            storageErrorFor(IOException()),
        )
    }

    private fun app(
        packageName: String,
        installedDaysAgo: Long,
        system: Boolean = false,
        updatedSystem: Boolean = false,
    ) = InstalledLauncherApp(
        packageName = packageName,
        appLabel = packageName,
        firstInstallTime = now.minusSeconds(installedDaysAgo * 86_400),
        isSystemApp = system,
        isUpdatedSystemApp = updatedSystem,
    )
}
