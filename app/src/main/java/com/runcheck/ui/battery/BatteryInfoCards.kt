package com.runcheck.ui.battery

/**
 * Info card IDs for the Battery detail screen.
 *
 * Card IDs include a version suffix (e.g. "_v1"). When card content is updated
 * in a future release, bump the version so that previously dismissed cards
 * resurface with the new content.
 */
object BatteryInfoCards {
    const val HEALTH_80_PERCENT = "battery_health_80_v1"
    const val DIES_BEFORE_ZERO = "battery_dies_before_zero_v1"
    const val CHARGING_HABITS = "battery_charging_habits_v1"
    const val SCREEN_OFF_DRAIN = "battery_screen_off_drain_v1"
}
