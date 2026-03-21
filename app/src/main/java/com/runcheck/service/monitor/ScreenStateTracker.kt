package com.runcheck.service.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.ScreenUsageStats
import com.runcheck.domain.model.SleepAnalysis
import com.runcheck.domain.repository.ScreenStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks screen on/off state and deep sleep vs held awake during screen-off.
 * State is persisted so tracking survives process recreation.
 */
@Singleton
class ScreenStateTracker @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ScreenStateRepository {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()
    private var idleModeReceiverRegistered = false
    private val idleModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED) {
                onDeviceIdleModeChanged()
            }
        }
    }

    override fun initialize() {
        synchronized(lock) {
            registerIdleModeReceiverIfNeeded()
            synchronizeState(now = System.currentTimeMillis(), persist = true)
        }
    }

    override fun onScreenTurnedOn() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val state = synchronizeState(now, persist = false)
            persistState(applyScreenStateChange(state, isScreenOn = true, now = now))
        }
    }

    override fun onScreenTurnedOff() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val state = synchronizeState(now, persist = false)
            persistState(applyScreenStateChange(state, isScreenOn = false, now = now))
        }
    }

    override fun onPowerConnected() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val state = synchronizeState(now, persist = false)
            persistState(resetAll(state, now, getCurrentChargingStatus()))
        }
    }

    override fun onPowerDisconnected() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val state = synchronizeState(now, persist = false)
            persistState(resetAll(state, now, getCurrentChargingStatus()))
        }
    }

    override fun onDeviceIdleModeChanged() {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val state = synchronizeState(now, persist = false)
            persistState(applyIdleStateChange(state, now, getCurrentIdleState()))
        }
    }

    override fun updateChargingStatus(chargingStatus: ChargingStatus) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val state = loadStateOrCreate(now)
            val synced = syncChargingStatus(state, now, chargingStatus)
            if (synced != state) {
                persistState(synced)
            }
        }
    }

    override fun getScreenUsageStats(): ScreenUsageStats? {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val state = synchronizeState(now, persist = true)
            val currentLevel = getCurrentBatteryLevel() ?: return null
            val snapshot = state.snapshot(now = now, currentLevel = currentLevel)

            if (snapshot.screenOnDurationMs == 0L && snapshot.screenOffDurationMs == 0L) {
                return null
            }

            val onRatePerHour = if (snapshot.screenOnDurationMs > 60_000) {
                snapshot.screenOnDrainPct / (snapshot.screenOnDurationMs / 3_600_000f)
            } else {
                null
            }
            val offRatePerHour = if (snapshot.screenOffDurationMs > 60_000) {
                snapshot.screenOffDrainPct / (snapshot.screenOffDurationMs / 3_600_000f)
            } else {
                null
            }

            return ScreenUsageStats(
                screenOnDurationMs = snapshot.screenOnDurationMs,
                screenOffDurationMs = snapshot.screenOffDurationMs,
                screenOnDrainPct = snapshot.screenOnDrainPct,
                screenOffDrainPct = snapshot.screenOffDrainPct,
                screenOnDrainRate = onRatePerHour,
                screenOffDrainRate = offRatePerHour
            )
        }
    }

    override fun getSleepAnalysis(): SleepAnalysis? {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val state = synchronizeState(now, persist = true)
            val snapshot = state.snapshot(now = now, currentLevel = getCurrentBatteryLevel())
            val totalSleepTrackedMs = snapshot.deepSleepDurationMs + snapshot.heldAwakeDurationMs
            if (totalSleepTrackedMs < MIN_SLEEP_ANALYSIS_DURATION_MS) {
                return null
            }
            return SleepAnalysis(
                deepSleepDurationMs = snapshot.deepSleepDurationMs,
                heldAwakeDurationMs = snapshot.heldAwakeDurationMs
            )
        }
    }

    private fun getCurrentBatteryLevel(): Int? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return null
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 0..100) level else null
    }

    private fun synchronizeState(now: Long, persist: Boolean): PersistedState {
        var state = loadStateOrCreate(now)
        state = syncChargingStatus(state, now, getCurrentChargingStatus())
        state = syncScreenState(state, now, powerManager.isInteractive)
        state = syncIdleState(state, now, getCurrentIdleState())
        if (persist) {
            persistState(state)
        }
        return state
    }

    private fun loadStateOrCreate(now: Long): PersistedState {
        if (!prefs.contains(KEY_LAST_TRANSITION_TIME)) {
            return createInitialState(now, getCurrentChargingStatus())
        }
        return PersistedState(
            screenOn = prefs.getBoolean(KEY_SCREEN_ON, powerManager.isInteractive),
            lastTransitionTime = prefs.getLong(KEY_LAST_TRANSITION_TIME, now),
            lastTransitionLevel = prefs.getInt(KEY_LAST_TRANSITION_LEVEL, INVALID_LEVEL)
                .takeUnless { it == INVALID_LEVEL },
            screenOnDurationMs = prefs.getLong(KEY_SCREEN_ON_DURATION_MS, 0L),
            screenOffDurationMs = prefs.getLong(KEY_SCREEN_OFF_DURATION_MS, 0L),
            screenOnDrainPct = prefs.getFloat(KEY_SCREEN_ON_DRAIN_PCT, 0f),
            screenOffDrainPct = prefs.getFloat(KEY_SCREEN_OFF_DRAIN_PCT, 0f),
            deepSleepDurationMs = prefs.getLong(KEY_DEEP_SLEEP_DURATION_MS, 0L),
            heldAwakeDurationMs = prefs.getLong(KEY_HELD_AWAKE_DURATION_MS, 0L),
            lastIdleCheckTime = prefs.getLong(KEY_LAST_IDLE_CHECK_TIME, now),
            lastIdleState = prefs.getBoolean(KEY_LAST_IDLE_STATE, !powerManager.isInteractive && getCurrentIdleState()),
            lastChargingStatus = prefs.getString(KEY_LAST_CHARGING_STATUS, null)
                ?.let(::parseChargingStatus)
                ?: getCurrentChargingStatus()
        )
    }

    private fun persistState(state: PersistedState) {
        prefs.edit()
            .putBoolean(KEY_SCREEN_ON, state.screenOn)
            .putLong(KEY_LAST_TRANSITION_TIME, state.lastTransitionTime)
            .putInt(KEY_LAST_TRANSITION_LEVEL, state.lastTransitionLevel ?: INVALID_LEVEL)
            .putLong(KEY_SCREEN_ON_DURATION_MS, state.screenOnDurationMs)
            .putLong(KEY_SCREEN_OFF_DURATION_MS, state.screenOffDurationMs)
            .putFloat(KEY_SCREEN_ON_DRAIN_PCT, state.screenOnDrainPct)
            .putFloat(KEY_SCREEN_OFF_DRAIN_PCT, state.screenOffDrainPct)
            .putLong(KEY_DEEP_SLEEP_DURATION_MS, state.deepSleepDurationMs)
            .putLong(KEY_HELD_AWAKE_DURATION_MS, state.heldAwakeDurationMs)
            .putLong(KEY_LAST_IDLE_CHECK_TIME, state.lastIdleCheckTime)
            .putBoolean(KEY_LAST_IDLE_STATE, state.lastIdleState)
            .putString(KEY_LAST_CHARGING_STATUS, state.lastChargingStatus.name)
            .apply()
    }

    private fun createInitialState(now: Long, chargingStatus: ChargingStatus): PersistedState {
        val screenOn = powerManager.isInteractive
        return PersistedState(
            screenOn = screenOn,
            lastTransitionTime = now,
            lastTransitionLevel = getCurrentBatteryLevel(),
            screenOnDurationMs = 0L,
            screenOffDurationMs = 0L,
            screenOnDrainPct = 0f,
            screenOffDrainPct = 0f,
            deepSleepDurationMs = 0L,
            heldAwakeDurationMs = 0L,
            lastIdleCheckTime = now,
            lastIdleState = !screenOn && getCurrentIdleState(),
            lastChargingStatus = chargingStatus
        )
    }

    private fun syncChargingStatus(
        state: PersistedState,
        now: Long,
        chargingStatus: ChargingStatus
    ): PersistedState {
        return if (state.lastChargingStatus != chargingStatus) {
            resetAll(state, now, chargingStatus)
        } else {
            state
        }
    }

    private fun syncScreenState(state: PersistedState, now: Long, isScreenOn: Boolean): PersistedState {
        return if (state.screenOn != isScreenOn) {
            applyScreenStateChange(state, isScreenOn, now)
        } else {
            state
        }
    }

    private fun syncIdleState(state: PersistedState, now: Long, isIdle: Boolean): PersistedState {
        return if (!state.screenOn && state.lastIdleState != isIdle) {
            state.copy(
                lastIdleCheckTime = now,
                lastIdleState = isIdle
            )
        } else {
            state
        }
    }

    private fun applyScreenStateChange(
        state: PersistedState,
        isScreenOn: Boolean,
        now: Long
    ): PersistedState {
        val elapsed = (now - state.lastTransitionTime).coerceAtLeast(0L)
        val currentLevel = getCurrentBatteryLevel()
        val drain = state.lastTransitionLevel?.let { startLevel ->
            currentLevel?.let { (startLevel - it).coerceAtLeast(0).toFloat() }
        } ?: 0f

        var next = if (state.screenOn) {
            state.copy(
                screenOnDurationMs = state.screenOnDurationMs + elapsed,
                screenOnDrainPct = state.screenOnDrainPct + drain
            )
        } else {
            val flushed = flushIdleTime(state, now)
            flushed.copy(
                screenOffDurationMs = flushed.screenOffDurationMs + elapsed,
                screenOffDrainPct = flushed.screenOffDrainPct + drain
            )
        }

        next = next.copy(
            screenOn = isScreenOn,
            lastTransitionTime = now,
            lastTransitionLevel = currentLevel,
            lastIdleCheckTime = now,
            lastIdleState = !isScreenOn && getCurrentIdleState()
        )
        return next
    }

    private fun applyIdleStateChange(
        state: PersistedState,
        now: Long,
        isIdle: Boolean
    ): PersistedState {
        if (state.screenOn) {
            return state.copy(lastIdleCheckTime = now, lastIdleState = false)
        }
        return flushIdleTime(state, now).copy(
            lastIdleCheckTime = now,
            lastIdleState = isIdle
        )
    }

    private fun flushIdleTime(state: PersistedState, now: Long): PersistedState {
        if (state.screenOn) return state
        val elapsed = (now - state.lastIdleCheckTime).coerceAtLeast(0L)
        if (elapsed == 0L) return state
        return if (state.lastIdleState) {
            state.copy(deepSleepDurationMs = state.deepSleepDurationMs + elapsed)
        } else {
            state.copy(heldAwakeDurationMs = state.heldAwakeDurationMs + elapsed)
        }
    }

    private fun resetAll(
        state: PersistedState,
        now: Long,
        chargingStatus: ChargingStatus
    ): PersistedState {
        val screenOn = powerManager.isInteractive
        return state.copy(
            screenOn = screenOn,
            lastTransitionTime = now,
            lastTransitionLevel = getCurrentBatteryLevel(),
            screenOnDurationMs = 0L,
            screenOffDurationMs = 0L,
            screenOnDrainPct = 0f,
            screenOffDrainPct = 0f,
            deepSleepDurationMs = 0L,
            heldAwakeDurationMs = 0L,
            lastIdleCheckTime = now,
            lastIdleState = !screenOn && getCurrentIdleState(),
            lastChargingStatus = chargingStatus
        )
    }

    private fun getCurrentChargingStatus(): ChargingStatus {
        val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargingStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> ChargingStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingStatus.NOT_CHARGING
            else -> ChargingStatus.NOT_CHARGING
        }
    }

    private fun getCurrentIdleState(): Boolean = powerManager.isDeviceIdleMode

    private fun registerIdleModeReceiverIfNeeded() {
        if (idleModeReceiverRegistered) return
        ContextCompat.registerReceiver(
            context,
            idleModeReceiver,
            IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        idleModeReceiverRegistered = true
    }

    private fun parseChargingStatus(value: String): ChargingStatus {
        return ChargingStatus.entries.firstOrNull { it.name == value } ?: ChargingStatus.NOT_CHARGING
    }

    private data class PersistedState(
        val screenOn: Boolean,
        val lastTransitionTime: Long,
        val lastTransitionLevel: Int?,
        val screenOnDurationMs: Long,
        val screenOffDurationMs: Long,
        val screenOnDrainPct: Float,
        val screenOffDrainPct: Float,
        val deepSleepDurationMs: Long,
        val heldAwakeDurationMs: Long,
        val lastIdleCheckTime: Long,
        val lastIdleState: Boolean,
        val lastChargingStatus: ChargingStatus
    ) {
        fun snapshot(now: Long, currentLevel: Int?): Snapshot {
            val elapsed = (now - lastTransitionTime).coerceAtLeast(0L)
            val currentDrain = lastTransitionLevel?.let { startLevel ->
                currentLevel?.let { (startLevel - it).coerceAtLeast(0).toFloat() }
            } ?: 0f
            val screenOnDuration = screenOnDurationMs + if (screenOn) elapsed else 0L
            val screenOffDuration = screenOffDurationMs + if (!screenOn) elapsed else 0L
            val screenOnDrain = screenOnDrainPct + if (screenOn) currentDrain else 0f
            val screenOffDrain = screenOffDrainPct + if (!screenOn) currentDrain else 0f
            val idleElapsed = if (!screenOn) (now - lastIdleCheckTime).coerceAtLeast(0L) else 0L
            val deepSleepDuration = deepSleepDurationMs + if (!screenOn && lastIdleState) idleElapsed else 0L
            val heldAwakeDuration = heldAwakeDurationMs + if (!screenOn && !lastIdleState) idleElapsed else 0L
            return Snapshot(
                screenOnDurationMs = screenOnDuration,
                screenOffDurationMs = screenOffDuration,
                screenOnDrainPct = screenOnDrain,
                screenOffDrainPct = screenOffDrain,
                deepSleepDurationMs = deepSleepDuration,
                heldAwakeDurationMs = heldAwakeDuration
            )
        }
    }

    private data class Snapshot(
        val screenOnDurationMs: Long,
        val screenOffDurationMs: Long,
        val screenOnDrainPct: Float,
        val screenOffDrainPct: Float,
        val deepSleepDurationMs: Long,
        val heldAwakeDurationMs: Long
    )

    private companion object {
        private const val MIN_SLEEP_ANALYSIS_DURATION_MS = 60_000L
        private const val PREFS_NAME = "screen_state_tracker"
        private const val INVALID_LEVEL = -1
        private const val KEY_SCREEN_ON = "screen_on"
        private const val KEY_LAST_TRANSITION_TIME = "last_transition_time"
        private const val KEY_LAST_TRANSITION_LEVEL = "last_transition_level"
        private const val KEY_SCREEN_ON_DURATION_MS = "screen_on_duration_ms"
        private const val KEY_SCREEN_OFF_DURATION_MS = "screen_off_duration_ms"
        private const val KEY_SCREEN_ON_DRAIN_PCT = "screen_on_drain_pct"
        private const val KEY_SCREEN_OFF_DRAIN_PCT = "screen_off_drain_pct"
        private const val KEY_DEEP_SLEEP_DURATION_MS = "deep_sleep_duration_ms"
        private const val KEY_HELD_AWAKE_DURATION_MS = "held_awake_duration_ms"
        private const val KEY_LAST_IDLE_CHECK_TIME = "last_idle_check_time"
        private const val KEY_LAST_IDLE_STATE = "last_idle_state"
        private const val KEY_LAST_CHARGING_STATUS = "last_charging_status"
    }
}
