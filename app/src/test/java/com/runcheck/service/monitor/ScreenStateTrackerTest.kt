package com.runcheck.service.monitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.PowerManager
import com.runcheck.domain.model.ChargingStatus
import io.mockk.every
import io.mockk.mockk
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScreenStateTrackerTest {

    private val prefs = InMemorySharedPreferences()
    private val context: Context = mockk(relaxed = true)
    private val powerManager: PowerManager = mockk(relaxed = true)
    private val batteryManager: BatteryManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        val batteryIntent: Intent = mockk(relaxed = true)

        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
        every { context.getSharedPreferences("screen_state_tracker", Context.MODE_PRIVATE) } returns prefs
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) } returns
            BatteryManager.BATTERY_STATUS_DISCHARGING
        every { context.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { powerManager.isInteractive } returns false
        every { powerManager.isDeviceIdleMode } returns false
        every { batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 60
    }

    @Test
    fun `persisted screen usage survives tracker recreation`() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean("screen_on", false)
            .putLong("last_transition_time", now)
            .putInt("last_transition_level", 60)
            .putLong("screen_on_duration_ms", 0L)
            .putLong("screen_off_duration_ms", 90 * 60_000L)
            .putFloat("screen_on_drain_pct", 0f)
            .putFloat("screen_off_drain_pct", 3f)
            .putLong("deep_sleep_duration_ms", 0L)
            .putLong("held_awake_duration_ms", 0L)
            .putLong("last_idle_check_time", now)
            .putBoolean("last_idle_state", false)
            .putString("last_charging_status", ChargingStatus.DISCHARGING.name)
            .commit()

        val tracker = ScreenStateTracker(context)

        val stats = tracker.getScreenUsageStats()

        assertNotNull(stats)
        requireNotNull(stats)
        assertTrue(stats.screenOffDurationMs >= 90 * 60_000L)
        assertEquals(3f, stats.screenOffDrainPct, 0.01f)
        assertTrue(stats.screenOffDrainRate != null)
        assertTrue(abs(requireNotNull(stats.screenOffDrainRate) - 2f) < 0.05f)
    }

    @Test
    fun `charging status change clears persisted usage and sleep data`() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean("screen_on", false)
            .putLong("last_transition_time", now)
            .putInt("last_transition_level", 60)
            .putLong("screen_on_duration_ms", 0L)
            .putLong("screen_off_duration_ms", 45 * 60_000L)
            .putFloat("screen_on_drain_pct", 0f)
            .putFloat("screen_off_drain_pct", 2f)
            .putLong("deep_sleep_duration_ms", 20 * 60_000L)
            .putLong("held_awake_duration_ms", 25 * 60_000L)
            .putLong("last_idle_check_time", now)
            .putBoolean("last_idle_state", false)
            .putString("last_charging_status", ChargingStatus.DISCHARGING.name)
            .commit()

        val tracker = ScreenStateTracker(context)

        tracker.updateChargingStatus(ChargingStatus.CHARGING)

        assertNull(tracker.getScreenUsageStats())
        assertNull(tracker.getSleepAnalysis())
    }

    @Test
    fun `cold start idle reconciliation avoids backfilling unknown held awake time`() {
        val now = System.currentTimeMillis() - 60 * 60_000L
        prefs.edit()
            .putBoolean("screen_on", false)
            .putLong("last_transition_time", now)
            .putInt("last_transition_level", 60)
            .putLong("screen_on_duration_ms", 0L)
            .putLong("screen_off_duration_ms", 0L)
            .putFloat("screen_on_drain_pct", 0f)
            .putFloat("screen_off_drain_pct", 0f)
            .putLong("deep_sleep_duration_ms", 0L)
            .putLong("held_awake_duration_ms", 0L)
            .putLong("last_idle_check_time", now)
            .putBoolean("last_idle_state", false)
            .putString("last_charging_status", ChargingStatus.DISCHARGING.name)
            .commit()
        every { powerManager.isDeviceIdleMode } returns true

        val tracker = ScreenStateTracker(context)

        assertNull(tracker.getSleepAnalysis())
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? =
            values[key] as? String ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (values[key] as? Set<String>)?.toMutableSet() ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor(values)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private class Editor(
            private val target: MutableMap<String, Any?>
        ) : SharedPreferences.Editor {
            private val updates = linkedMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = applyUpdate(key, value)

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
                applyUpdate(key, values?.toSet())

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = applyUpdate(key, value)

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = applyUpdate(key, value)

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = applyUpdate(key, value)

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = applyUpdate(key, value)

            override fun remove(key: String?): SharedPreferences.Editor = applyUpdate(key, null)

            override fun clear(): SharedPreferences.Editor {
                clearRequested = true
                return this
            }

            override fun commit(): Boolean {
                if (clearRequested) {
                    target.clear()
                }
                for ((key, value) in updates) {
                    if (value == null) {
                        target.remove(key)
                    } else {
                        target[key] = value
                    }
                }
                return true
            }

            override fun apply() {
                commit()
            }

            private fun applyUpdate(key: String?, value: Any?): SharedPreferences.Editor {
                requireNotNull(key)
                updates[key] = value
                return this
            }
        }
    }
}
