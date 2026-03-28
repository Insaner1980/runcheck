package com.runcheck.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * Centralized animation tokens for consistent motion throughout the app.
 * Component-specific timings (e.g., TrendChart sweep phases) remain local
 * to their components — only shared/repeated values belong here.
 */
object MotionTokens {
    // ── Easings ─────────────────────────────────────────────────────────────────

    /** Standard Material decelerate easing for entrances and progress indicators. */
    val EaseOut = FastOutSlowInEasing

    /** Smooth sweep easing used by chart oscilloscope animations. */
    val SweepEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    // ── Durations (ms) ──────────────────────────────────────────────────────────

    /** Quick micro-interactions: number ticks, emphasis. */
    const val SHORT = 200

    /** Navigation slide/fade transitions. */
    const val MEDIUM = 300

    /** Chart sweeps, segmented bar fills. */
    const val SWEEP = 800

    /** Ring/gauge fill animations. */
    const val RING = 1200

    /** Continuous indicator scrolls, heat strip loops. */
    const val CONTINUOUS = 2000

    /** LiveChart smooth scroll interpolation. */
    const val SCROLL = 150

    // ── Fullscreen chart transitions ────────────────────────────────────────────

    const val FULLSCREEN_ENTER_SCALE = 260
    const val FULLSCREEN_ENTER_FADE = 220
    const val FULLSCREEN_EXIT = 180

    // ── SpeedTest gauge ─────────────────────────────────────────────────────────

    const val SPEED_GAUGE = 1700
    const val SPEED_SWEEP = 1800
    const val SPEED_RESULT = 700

    // ── Prebuilt specs ──────────────────────────────────────────────────────────

    fun <T> tweenShort() = tween<T>(durationMillis = SHORT)

    fun <T> tweenMedium() = tween<T>(durationMillis = MEDIUM)

    fun <T> tweenSweep() = tween<T>(durationMillis = SWEEP, easing = SweepEasing)

    fun <T> tweenRing() = tween<T>(durationMillis = RING, easing = EaseOut)
}
