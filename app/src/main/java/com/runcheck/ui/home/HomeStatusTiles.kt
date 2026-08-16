package com.runcheck.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.HealthStatus
import com.runcheck.domain.model.SignalQuality
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.formatTemperatureValue
import com.runcheck.ui.common.healthStatusLabel
import com.runcheck.ui.common.temperatureUnitRes
import com.runcheck.ui.theme.StatusColors
import com.runcheck.ui.theme.TileBattery
import com.runcheck.ui.theme.TileNetwork
import com.runcheck.ui.theme.TileStorage
import com.runcheck.ui.theme.TileThermal
import com.runcheck.ui.theme.homeStatusTileTypeScale
import com.runcheck.ui.theme.statusColor
import com.runcheck.ui.theme.statusColorForSignalQuality
import com.runcheck.ui.theme.statusColorForStoragePercent
import com.runcheck.ui.theme.statusColorForTemperature
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.uiTokens

internal enum class HomeStatusTileCategory(
    val tieBreakOrder: Int,
) {
    BATTERY(0),
    NETWORK(1),
    THERMAL(2),
    STORAGE(3),
}

@Immutable
internal data class HomeStatusTileSlots(
    val slotA: HomeStatusTileCategory,
    val slotB: HomeStatusTileCategory,
    val slotC: HomeStatusTileCategory,
    val slotD: HomeStatusTileCategory,
)

internal fun assignHomeStatusTileSlots(
    batteryStatus: HealthStatus,
    networkStatus: HealthStatus,
    thermalStatus: HealthStatus,
    storageStatus: HealthStatus,
): HomeStatusTileSlots {
    val sortedCategories =
        listOf(
            HomeStatusTileCategory.BATTERY to batteryStatus,
            HomeStatusTileCategory.NETWORK to networkStatus,
            HomeStatusTileCategory.THERMAL to thermalStatus,
            HomeStatusTileCategory.STORAGE to storageStatus,
        ).sortedWith(
            compareBy<Pair<HomeStatusTileCategory, HealthStatus>>(
                { it.second.severityRank },
                { it.first.tieBreakOrder },
            ),
        )

    return HomeStatusTileSlots(
        slotA = sortedCategories[0].first,
        slotB = sortedCategories[1].first,
        slotC = sortedCategories[2].first,
        slotD = sortedCategories[3].first,
    )
}

private val HealthStatus.severityRank: Int
    get() =
        when (this) {
            HealthStatus.CRITICAL -> 0
            HealthStatus.POOR -> 1
            HealthStatus.FAIR -> 2
            HealthStatus.HEALTHY -> 3
        }

@Immutable
private data class HomeStatusTileStatuses(
    val battery: HealthStatus,
    val network: HealthStatus,
    val thermal: HealthStatus,
    val storage: HealthStatus,
) {
    fun forCategory(category: HomeStatusTileCategory): HealthStatus =
        when (category) {
            HomeStatusTileCategory.BATTERY -> battery
            HomeStatusTileCategory.NETWORK -> network
            HomeStatusTileCategory.THERMAL -> thermal
            HomeStatusTileCategory.STORAGE -> storage
        }
}

@Composable
private fun homeStatusTileStatuses(state: HomeUiState.Success): HomeStatusTileStatuses {
    val statusColors = MaterialTheme.statusColors
    return HomeStatusTileStatuses(
        battery = HealthScore.statusFromScore(state.healthScore.batteryScore),
        network =
            statusColorForSignalQuality(state.networkState.signalQuality)
                .toHealthStatus(statusColors),
        thermal =
            statusColorForTemperature(state.thermalState.batteryTempC)
                .toHealthStatus(statusColors),
        storage =
            statusColorForStoragePercent(
                state.storageState.usagePercent
                    .toInt()
                    .coerceIn(0, 100),
            ).toHealthStatus(statusColors),
    )
}

private fun Color.toHealthStatus(statusColors: StatusColors): HealthStatus =
    when (this) {
        statusColors.healthy -> HealthStatus.HEALTHY
        statusColors.fair -> HealthStatus.FAIR
        statusColors.poor -> HealthStatus.POOR
        statusColors.critical -> HealthStatus.CRITICAL
        else -> error("Expected an existing status color token")
    }

@Composable
internal fun HomeStatusTiles(
    state: HomeUiState.Success,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.uiTokens
    val typeScale = MaterialTheme.homeStatusTileTypeScale
    val density = LocalDensity.current
    val tileMinHeight =
        with(density) {
            maxOf(
                tokens.homeStatusTileHeight,
                tokens.homeStatusTileValueTop +
                    typeScale.value.lineHeight.toDp() +
                    tokens.homeStatusTileStatusGap +
                    typeScale.status.lineHeight.toDp(),
            )
        }
    val statuses = homeStatusTileStatuses(state)
    val slots =
        assignHomeStatusTileSlots(
            batteryStatus = statuses.battery,
            networkStatus = statuses.network,
            thermalStatus = statuses.thermal,
            storageStatus = statuses.storage,
        )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.homeStatusTileGap),
    ) {
        HomeStatusTile(
            category = slots.slotA,
            state = state,
            status = statuses.forCategory(slots.slotA),
            onClick =
                slots.slotA.navigationCallback(
                    onNavigateToBattery = onNavigateToBattery,
                    onNavigateToNetwork = onNavigateToNetwork,
                    onNavigateToThermal = onNavigateToThermal,
                    onNavigateToStorage = onNavigateToStorage,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = tileMinHeight),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = tileMinHeight),
            horizontalArrangement = Arrangement.spacedBy(tokens.homeStatusTileGap),
        ) {
            HomeStatusTile(
                category = slots.slotB,
                state = state,
                status = statuses.forCategory(slots.slotB),
                onClick =
                    slots.slotB.navigationCallback(
                        onNavigateToBattery = onNavigateToBattery,
                        onNavigateToNetwork = onNavigateToNetwork,
                        onNavigateToThermal = onNavigateToThermal,
                        onNavigateToStorage = onNavigateToStorage,
                    ),
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            )

            HomeStatusTile(
                category = slots.slotC,
                state = state,
                status = statuses.forCategory(slots.slotC),
                onClick =
                    slots.slotC.navigationCallback(
                        onNavigateToBattery = onNavigateToBattery,
                        onNavigateToNetwork = onNavigateToNetwork,
                        onNavigateToThermal = onNavigateToThermal,
                        onNavigateToStorage = onNavigateToStorage,
                    ),
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            )
        }

        HomeStatusTile(
            category = slots.slotD,
            state = state,
            status = statuses.forCategory(slots.slotD),
            onClick =
                slots.slotD.navigationCallback(
                    onNavigateToBattery = onNavigateToBattery,
                    onNavigateToNetwork = onNavigateToNetwork,
                    onNavigateToThermal = onNavigateToThermal,
                    onNavigateToStorage = onNavigateToStorage,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = tileMinHeight),
        )
    }
}

private fun HomeStatusTileCategory.navigationCallback(
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
): () -> Unit =
    when (this) {
        HomeStatusTileCategory.BATTERY -> onNavigateToBattery
        HomeStatusTileCategory.NETWORK -> onNavigateToNetwork
        HomeStatusTileCategory.THERMAL -> onNavigateToThermal
        HomeStatusTileCategory.STORAGE -> onNavigateToStorage
    }

@Composable
private fun HomeStatusTile(
    category: HomeStatusTileCategory,
    state: HomeUiState.Success,
    status: HealthStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.uiTokens
    val colors = homeStatusTileColors(category = category, status = status)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(tokens.homeStatusTileCornerRadius),
        color = colors.background,
        contentColor = colors.value,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = tokens.homeStatusTilePaddingHorizontal,
                        end = tokens.homeStatusTilePaddingHorizontal,
                    ),
        ) {
            Text(
                text = category.label(),
                style = MaterialTheme.homeStatusTileTypeScale.category,
                color = colors.category,
                modifier = Modifier.padding(top = tokens.homeStatusTileCategoryTop),
            )

            StandardTileContent(
                content = category.content(state = state, status = status),
                colors = colors,
                modifier = Modifier.padding(top = tokens.homeStatusTileValueTop),
            )
        }
    }
}

@Immutable
private data class HomeStatusTileColors(
    val background: Color,
    val category: Color,
    val value: Color,
    val status: Color,
)

@Composable
private fun homeStatusTileColors(
    category: HomeStatusTileCategory,
    status: HealthStatus,
): HomeStatusTileColors {
    val colorScheme = MaterialTheme.colorScheme
    return if (status == HealthStatus.HEALTHY) {
        HomeStatusTileColors(
            background = category.categoryColor(),
            category = colorScheme.background,
            value = colorScheme.background,
            status = colorScheme.background,
        )
    } else {
        HomeStatusTileColors(
            background = colorScheme.surfaceContainer,
            category = colorScheme.onSurfaceVariant,
            value = colorScheme.onSurface,
            status = statusColor(status),
        )
    }
}

private fun HomeStatusTileCategory.categoryColor(): Color =
    when (this) {
        HomeStatusTileCategory.BATTERY -> TileBattery
        HomeStatusTileCategory.NETWORK -> TileNetwork
        HomeStatusTileCategory.THERMAL -> TileThermal
        HomeStatusTileCategory.STORAGE -> TileStorage
    }

@Composable
private fun HomeStatusTileCategory.label(): String =
    stringResource(
        when (this) {
            HomeStatusTileCategory.BATTERY -> R.string.home_battery_card
            HomeStatusTileCategory.NETWORK -> R.string.home_network_card
            HomeStatusTileCategory.THERMAL -> R.string.home_thermal_card
            HomeStatusTileCategory.STORAGE -> R.string.home_storage_card
        },
    )

@Immutable
private data class StatusTileContent(
    val value: String,
    val suffix: String?,
    val status: String,
)

@Composable
private fun HomeStatusTileCategory.content(
    state: HomeUiState.Success,
    status: HealthStatus,
): StatusTileContent =
    when (this) {
        HomeStatusTileCategory.NETWORK -> {
            StatusTileContent(
                value =
                    state.networkState.signalDbm?.toString()
                        ?: stringResource(R.string.placeholder_dash),
                suffix = stringResource(R.string.unit_dbm),
                status = networkSignalStatusLabel(state.networkState.signalQuality),
            )
        }

        HomeStatusTileCategory.THERMAL -> {
            StatusTileContent(
                value =
                    formatTemperatureValue(
                        state.thermalState.batteryTempC,
                        state.temperatureUnit,
                    ),
                suffix = stringResource(temperatureUnitRes(state.temperatureUnit)),
                status = healthStatusLabel(status),
            )
        }

        HomeStatusTileCategory.STORAGE -> {
            val formattedSize =
                formatStorageSize(
                    LocalContext.current,
                    state.storageState.availableBytes,
                ).splitValueAndSuffix()
            StatusTileContent(
                value = formattedSize.value,
                suffix = formattedSize.suffix,
                status = storageStatusLabel(status),
            )
        }

        HomeStatusTileCategory.BATTERY -> {
            StatusTileContent(
                value = state.batteryState.level.toString(),
                suffix = stringResource(R.string.unit_percent),
                status = batteryHealthStatusLabel(status),
            )
        }
    }

@Composable
private fun StandardTileContent(
    content: StatusTileContent,
    colors: HomeStatusTileColors,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.uiTokens
    val typeScale = MaterialTheme.homeStatusTileTypeScale

    Column(modifier = modifier) {
        TileValueLine(
            value = content.value,
            suffix = content.suffix,
            valueStyle = typeScale.value,
            suffixStyle = typeScale.suffix,
            textColor = colors.value,
        )

        Spacer(
            modifier = Modifier.height(tokens.homeStatusTileStatusGap),
        )

        Text(
            text = content.status,
            style = typeScale.status,
            color = colors.status,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun batteryHealthStatusLabel(status: HealthStatus): String =
    stringResource(
        when (status) {
            HealthStatus.HEALTHY -> R.string.home_battery_status_healthy
            HealthStatus.FAIR -> R.string.home_battery_status_fair
            HealthStatus.POOR -> R.string.home_battery_status_poor
            HealthStatus.CRITICAL -> R.string.home_battery_status_critical
        },
    )

@Composable
private fun TileValueLine(
    value: String,
    suffix: String?,
    valueStyle: TextStyle,
    suffixStyle: TextStyle,
    textColor: Color,
) {
    val tokens = MaterialTheme.uiTokens

    Row(
        horizontalArrangement = Arrangement.spacedBy(tokens.homeStatusTileValueSuffixGap),
    ) {
        Text(
            text = value,
            style = valueStyle,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .alignByBaseline(),
        )
        if (suffix != null) {
            Text(
                text = suffix,
                style = suffixStyle,
                color = textColor,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun networkSignalStatusLabel(signalQuality: SignalQuality): String =
    stringResource(
        when (signalQuality) {
            SignalQuality.EXCELLENT -> R.string.home_network_signal_excellent
            SignalQuality.GOOD -> R.string.home_network_signal_good
            SignalQuality.FAIR -> R.string.home_network_signal_fair
            SignalQuality.POOR -> R.string.home_network_signal_poor
            SignalQuality.NO_SIGNAL -> R.string.home_network_signal_none
        },
    )

@Composable
private fun storageStatusLabel(status: HealthStatus): String =
    stringResource(
        when (status) {
            HealthStatus.HEALTHY -> R.string.home_storage_status_healthy
            HealthStatus.FAIR -> R.string.home_storage_status_fair
            HealthStatus.POOR -> R.string.home_storage_status_poor
            HealthStatus.CRITICAL -> R.string.home_storage_status_critical
        },
    )

@Immutable
private data class ValueAndSuffix(
    val value: String,
    val suffix: String?,
)

private fun String.splitValueAndSuffix(): ValueAndSuffix {
    val separatorIndex = indexOfLast { it.isWhitespace() || it == '\u00A0' }
    return if (separatorIndex > 0 && separatorIndex < lastIndex) {
        ValueAndSuffix(
            value = substring(0, separatorIndex).trim(),
            suffix = substring(separatorIndex + 1).trim(),
        )
    } else {
        ValueAndSuffix(value = this, suffix = null)
    }
}
