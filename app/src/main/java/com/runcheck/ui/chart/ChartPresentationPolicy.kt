package com.runcheck.ui.chart

import com.runcheck.ui.components.ChartQualityZone
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

data class ChartViewport(
    val minValue: Float,
    val maxValue: Float,
    val ticks: List<Float>,
    val visibleZones: List<ChartQualityZone>,
)

sealed interface ChartPrimaryState {
    data object Loading : ChartPrimaryState

    data class Error(
        val message: String,
    ) : ChartPrimaryState

    data object Locked : ChartPrimaryState

    data object InsufficientData : ChartPrimaryState

    data object Data : ChartPrimaryState
}

data class HistoryPeriodSelectorPolicy(
    val isScrollable: Boolean,
    val selectedItemScrollTarget: Int,
    val animateSelectedItemScroll: Boolean,
    val selectedItemPosition: Int,
    val optionCount: Int,
    val announcesSelectedState: Boolean,
    val allowsLabelTruncation: Boolean,
)

fun calculateChartViewport(
    data: List<Float>,
    explicitTicks: List<Float>,
    qualityZones: List<ChartQualityZone>,
    availableHeightPx: Float,
    minimumLabelSpacingPx: Float,
): ChartViewport? {
    val finiteData = data.filter(Float::isFinite)
    if (finiteData.isEmpty()) return null

    val finiteTicks = explicitTicks.filter(Float::isFinite)
    val scaleValues = finiteData + finiteTicks
    var minValue = scaleValues.min()
    var maxValue = scaleValues.max()
    if (minValue == maxValue) {
        val symmetricPadding = maxOf(abs(minValue) * SINGLE_VALUE_PADDING_FRACTION, MINIMUM_SINGLE_VALUE_PADDING)
        minValue -= symmetricPadding
        maxValue += symmetricPadding
    } else {
        val padding = (maxValue - minValue) * VIEWPORT_PADDING_FRACTION
        minValue -= padding
        maxValue += padding
    }

    val retainedTicks =
        selectVisibleTicks(
            ticks = finiteTicks,
            minValue = minValue,
            maxValue = maxValue,
            availableHeightPx = availableHeightPx,
            minimumLabelSpacingPx = minimumLabelSpacingPx,
        )
    val visibleZones =
        qualityZones.mapNotNull { zone ->
            val zoneMin = minOf(zone.minValue, zone.maxValue)
            val zoneMax = maxOf(zone.minValue, zone.maxValue)
            if (!zoneMin.isFinite() || !zoneMax.isFinite()) return@mapNotNull null
            val clippedMin = maxOf(zoneMin, minValue)
            val clippedMax = minOf(zoneMax, maxValue)
            if (clippedMax <= clippedMin) {
                null
            } else {
                zone.copy(minValue = clippedMin, maxValue = clippedMax)
            }
        }

    return ChartViewport(
        minValue = minValue,
        maxValue = maxValue,
        ticks = retainedTicks,
        visibleZones = visibleZones,
    )
}

fun resolveChartPrimaryState(
    isLoading: Boolean,
    error: String?,
    isLocked: Boolean,
    dataPointCount: Int,
    minimumDataPointCount: Int,
): ChartPrimaryState =
    when {
        isLoading -> ChartPrimaryState.Loading
        error != null -> ChartPrimaryState.Error(error)
        isLocked -> ChartPrimaryState.Locked
        dataPointCount < minimumDataPointCount -> ChartPrimaryState.InsufficientData
        else -> ChartPrimaryState.Data
    }

fun historyPeriodSelectorPolicy(
    optionLabels: List<String>,
    selectedIndex: Int,
    viewportWidthDp: Int,
    fontScale: Float,
    reducedMotion: Boolean,
): HistoryPeriodSelectorPolicy {
    val optionCount = optionLabels.size
    val safeSelectedIndex = selectedIndex.coerceIn(0, (optionCount - 1).coerceAtLeast(0))
    val estimatedContentWidthDp =
        optionLabels.sumOf { label ->
            maxOf(
                MINIMUM_PERIOD_CHIP_WIDTH_DP,
                (label.length * ESTIMATED_CHARACTER_WIDTH_DP * fontScale.coerceAtLeast(1f)).roundToInt(),
            )
        } + PERIOD_CHIP_SPACING_DP * (optionCount - 1).coerceAtLeast(0)
    val isScrollable =
        optionCount > MAXIMUM_NON_SCROLLING_PERIOD_COUNT ||
            estimatedContentWidthDp > viewportWidthDp.coerceAtLeast(0)

    return HistoryPeriodSelectorPolicy(
        isScrollable = isScrollable,
        selectedItemScrollTarget = safeSelectedIndex,
        animateSelectedItemScroll = !reducedMotion,
        selectedItemPosition = if (optionCount == 0) 0 else safeSelectedIndex + 1,
        optionCount = optionCount,
        announcesSelectedState = optionCount > 0,
        allowsLabelTruncation = false,
    )
}

private fun selectVisibleTicks(
    ticks: List<Float>,
    minValue: Float,
    maxValue: Float,
    availableHeightPx: Float,
    minimumLabelSpacingPx: Float,
): List<Float> {
    val candidates =
        ticks
            .filter { it in minValue..maxValue }
            .distinct()
            .sorted()
    if (candidates.isEmpty()) return emptyList()

    val safeHeight = availableHeightPx.coerceAtLeast(0f)
    val safeSpacing = minimumLabelSpacingPx.coerceAtLeast(1f)
    val heightLimitedCount =
        (floor(safeHeight / safeSpacing).toInt() + 1)
            .coerceIn(1, MAXIMUM_Y_LABEL_COUNT)
    val sampled =
        if (candidates.size <= heightLimitedCount) {
            candidates
        } else if (heightLimitedCount == 1) {
            listOf(candidates[candidates.lastIndex / 2])
        } else {
            List(heightLimitedCount) { index ->
                val candidateIndex =
                    (index.toFloat() * candidates.lastIndex / (heightLimitedCount - 1))
                        .roundToInt()
                candidates[candidateIndex]
            }.distinct()
        }

    val range = maxValue - minValue
    if (range <= 0f || safeHeight <= 0f) return sampled.take(1)
    return buildList {
        sampled.forEach { tick ->
            val positionPx = (tick - minValue) / range * safeHeight
            val previousPositionPx =
                lastOrNull()?.let { previous ->
                    (previous - minValue) / range * safeHeight
                }
            if (previousPositionPx == null || positionPx - previousPositionPx >= safeSpacing) {
                add(tick)
            }
        }
    }
}

private const val VIEWPORT_PADDING_FRACTION = 0.075f
private const val SINGLE_VALUE_PADDING_FRACTION = 0.05f
private const val MINIMUM_SINGLE_VALUE_PADDING = 1f
private const val MAXIMUM_Y_LABEL_COUNT = 4
private const val MINIMUM_PERIOD_CHIP_WIDTH_DP = 64
private const val PERIOD_CHIP_SPACING_DP = 12
private const val ESTIMATED_CHARACTER_WIDTH_DP = 8
private const val MAXIMUM_NON_SCROLLING_PERIOD_COUNT = 4
