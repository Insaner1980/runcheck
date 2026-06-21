package com.runcheck.ui.chart

import androidx.compose.runtime.Composable
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.ui.common.EnumFilterChipRow

@Composable
fun HistoryPeriodFilterChipRow(
    selected: HistoryPeriod,
    onSelect: (HistoryPeriod) -> Unit,
    includeSinceUnplug: Boolean = false,
) {
    EnumFilterChipRow(
        values =
            HistoryPeriod.entries.filter {
                includeSinceUnplug || it != HistoryPeriod.SINCE_UNPLUG
            },
        selected = selected,
        onSelect = onSelect,
        labelFor = { historyPeriodLabel(it) },
    )
}
