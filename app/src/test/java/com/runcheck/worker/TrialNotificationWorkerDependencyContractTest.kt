package com.runcheck.worker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class TrialNotificationWorkerDependencyContractTest {
    private val rootDir: Path = findRootDir()

    @Test
    fun `trial notification worker depends on narrow purchase status refresher port`() {
        val workerSource =
            rootDir
                .resolve("app/src/main/java/com/runcheck/worker/TrialNotificationWorker.kt")
                .readText()
        val systemBindingsSource =
            rootDir
                .resolve("app/src/main/java/com/runcheck/di/SystemBindingsModule.kt")
                .readText()
        val refresherPath =
            rootDir.resolve("app/src/main/java/com/runcheck/billing/ProPurchaseStatusRefresher.kt")

        assertFalse(
            "TrialNotificationWorker should not depend on the concrete data-layer BillingManager",
            workerSource.contains("com.runcheck.data.billing.BillingManager") ||
                workerSource.contains("private val billingManager: BillingManager"),
        )
        assertTrue(
            "TrialNotificationWorker should depend on ProPurchaseStatusRefresher",
            workerSource.contains("ProPurchaseStatusRefresher"),
        )
        assertTrue(
            "ProPurchaseStatusRefresher should own initialization-aware purchase refresh",
            Files.exists(refresherPath) &&
                refresherPath.readText().contains("refreshPurchaseStatusAfterInitialization"),
        )
        assertTrue(
            "SystemBindingsModule should bind ProPurchaseStatusRefresher to BillingManager",
            systemBindingsSource.contains("bindProPurchaseStatusRefresher"),
        )
    }

    private fun findRootDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .first { Files.exists(it.resolve("gradle/libs.versions.toml")) }
    }
}
