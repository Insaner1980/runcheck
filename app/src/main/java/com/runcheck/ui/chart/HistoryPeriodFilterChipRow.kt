package com.runcheck.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.spacing
import kotlin.math.roundToInt

@Composable
fun HistoryPeriodFilterChipRow(
    selected: HistoryPeriod,
    onSelect: (HistoryPeriod) -> Unit,
    includeSinceUnplug: Boolean = false,
) {
    val options =
        HistoryPeriod.entries.filter {
            includeSinceUnplug || it != HistoryPeriod.SINCE_UNPLUG
        }
    HistoryPeriodSelectorRow(
        options = options,
        selected = selected,
        onSelect = onSelect,
        labelFor = { historyPeriodLabel(it) },
    )
}

@Composable
fun <T> HistoryPeriodSelectorRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelFor: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val reducedMotion = MaterialTheme.reducedMotion
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val density = LocalDensity.current
    val labels = options.map { option -> labelFor(option) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val policy =
            historyPeriodSelectorPolicy(
                optionLabels = labels,
                selectedIndex = selectedIndex,
                viewportWidthDp = maxWidth.value.roundToInt(),
                fontScale = density.fontScale,
                reducedMotion = reducedMotion,
            )
        LaunchedEffect(
            policy.selectedItemScrollTarget,
            policy.animateSelectedItemScroll,
            options.size,
        ) {
            if (options.isEmpty()) return@LaunchedEffect
            if (policy.animateSelectedItemScroll) {
                listState.animateScrollToItem(policy.selectedItemScrollTarget)
            } else {
                listState.scrollToItem(policy.selectedItemScrollTarget)
            }
        }
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        collectionInfo = CollectionInfo(rowCount = 1, columnCount = options.size)
                    },
        ) {
            itemsIndexed(
                items = options,
                key = { index, _ -> index },
            ) { index, option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = {
                        Text(
                            text = labels[index],
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false,
                        )
                    },
                    modifier =
                        Modifier.semantics {
                            collectionItemInfo =
                                CollectionItemInfo(
                                    rowIndex = 0,
                                    rowSpan = 1,
                                    columnIndex = index,
                                    columnSpan = 1,
                                )
                        },
                )
            }
        }

        if (listState.canScrollBackward) {
            HistoryPeriodEdgeFade(
                colors =
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        Color.Transparent,
                    ),
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        if (listState.canScrollForward) {
            HistoryPeriodEdgeFade(
                colors =
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface,
                    ),
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun HistoryPeriodEdgeFade(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .width(MaterialTheme.spacing.lg)
                .background(Brush.horizontalGradient(colors)),
    )
}
