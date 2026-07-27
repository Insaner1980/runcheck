package com.runcheck.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Centralized animation tokens for consistent motion throughout the app.
 * Component-specific timings (e.g., TrendChart sweep phases) remain local
 * to their components — only shared/repeated values belong here.
 */
object MotionTokens {
    // ── Easings ─────────────────────────────────────────────────────────────────

    val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    val EmphasizedEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    val DecelerateEasing = CubicBezierEasing(0f, 0f, 0f, 1f)

    // Compatibility aliases for components that migrate in later phases.
    val EaseOut = StandardEasing
    val SweepEasing = StandardEasing

    // ── Durations (ms) ──────────────────────────────────────────────────────────

    const val INSTANT = 100
    const val FAST = 180
    const val MEDIUM = 320
    const val SLOW = 520
    const val DELIBERATE = 900
    const val COUNTER = 700
    const val RESULT_STAGGER = 80
    const val LIST_ITEM_STAGGER = 40
    const val CHART_FILL_DELAY = 200

    // Compatibility aliases and component-specific timings.
    const val SHORT = FAST

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
    const val SPEED_RESULT = COUNTER

    // ── Prebuilt specs ──────────────────────────────────────────────────────────

    fun <T> tweenShort() = tween<T>(durationMillis = SHORT)

    fun <T> tweenMedium() = tween<T>(durationMillis = MEDIUM)

    fun <T> tweenSweep() = tween<T>(durationMillis = SWEEP, easing = SweepEasing)

    fun <T> tweenRing() = tween<T>(durationMillis = RING, easing = EaseOut)

    fun <T> gaugeSpring(): SpringSpec<T> =
        spring(
            dampingRatio = 0.72f,
            stiffness = 180f,
        )

    fun <T> chipSpring(): SpringSpec<T> =
        spring(
            dampingRatio = 0.55f,
            stiffness = 420f,
        )

    fun <T> counterTween(): TweenSpec<T> =
        tween(
            durationMillis = COUNTER,
            easing = DecelerateEasing,
        )

    fun <T> speedValueSpring(): SpringSpec<T> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 300f,
        )
}
