package com.runcheck.service.report

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WeeklyReportIntegrationContractTest {
    @Test
    fun `manifest reschedules on timezone changes without broad package visibility`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("android.intent.action.TIMEZONE_CHANGED"))
        assertFalse(manifest.contains("android.permission.QUERY_ALL_PACKAGES"))
    }

    @Test
    fun `application observes preference and Pro changes for weekly scheduling`() {
        val source = File("src/main/java/com/runcheck/RuncheckApp.kt").readText()

        assertTrue(source.contains("weeklyReportScheduler"))
        assertTrue(source.contains("weeklyReportEnabled"))
        assertTrue(source.contains("proAccessReady"))
        assertTrue(source.contains("distinctUntilChanged"))
    }

    @Test
    fun `timezone receiver uses the explicit reschedule path`() {
        val receiver = File("src/main/java/com/runcheck/service/monitor/BootReceiver.kt").readText()

        assertTrue(receiver.contains("ACTION_TIMEZONE_CHANGED"))
        assertTrue(receiver.contains("rescheduleForTimezoneChange"))
    }

    @Test
    fun `settings expose a Pro-aware weekly report toggle`() {
        val screen = File("src/main/java/com/runcheck/ui/settings/SettingsSections.kt").readText()

        assertTrue(screen.contains("settings_weekly_report"))
        assertTrue(screen.contains("weeklyReportEnabled"))
    }

    @Test
    fun `weekly report notification deep links to its protected route`() {
        val helper = File("src/main/java/com/runcheck/service/monitor/NotificationHelper.kt").readText()

        assertTrue(helper.contains("NAVIGATE_WEEKLY_REPORT = \"weekly_report\""))
        assertTrue(helper.contains("createContentIntent("))
        assertTrue(helper.contains("NAVIGATE_WEEKLY_REPORT,"))
    }
}
