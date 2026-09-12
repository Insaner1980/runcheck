package com.runcheck.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.runcheck.R
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.HealthStatus
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SignalQuality
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.formatTemperatureValue
import com.runcheck.ui.common.temperatureUnitRes
import com.runcheck.ui.theme.HomeCream
import com.runcheck.ui.theme.HomeGraphite
import com.runcheck.ui.theme.HomeInk
import com.runcheck.ui.theme.HomePeach
import com.runcheck.ui.theme.HomeStone
import com.runcheck.ui.theme.LARGE_CONTENT_FONT_SCALE
import com.runcheck.ui.theme.StatusColors
import com.runcheck.ui.theme.homeStatusTileTypeScale
import com.runcheck.ui.theme.statusColor
import com.runcheck.ui.theme.statusColorForSignalQuality
import com.runcheck.ui.theme.statusColorForStoragePercent
import com.runcheck.ui.theme.statusColorForTemperature
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.uiTokens

private const val HOME_BATTERY_TILE_WEIGHT = 0.55f
private const val HOME_THERMAL_TILE_WEIGHT = 0.45f
private const val HOME_STORAGE_TILE_WEIGHT = 0.63f
private const val HOME_NETWORK_TILE_WEIGHT = 0.37f
private val HOME_STATUS_TILE_OVERLAP = 12.dp
private val HOME_SINGLE_COLUMN_MIN_WIDTH = 320.dp

internal fun homeUsesSingleColumn(
    maxWidth: Dp,
    fontScale: Float,
): Boolean = fontScale >= LARGE_CONTENT_FONT_SCALE || maxWidth / fontScale < HOME_SINGLE_COLUMN_MIN_WIDTH

internal enum class HomeStatusTileCategory { BATTERY, THERMAL, STORAGE, NETWORK }

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
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.uiTokens
    val statuses = homeStatusTileStatuses(state)
    BoxWithConstraints(modifier = modifier) {
        val fontScale = LocalDensity.current.fontScale
        val singleColumn = homeUsesSingleColumn(maxWidth, fontScale)
        val tile: @Composable (HomeStatusTileCategory, Modifier, Shape?) -> Unit = { category, tileModifier, shape ->
            HomeStatusTile(
                category = category,
                state = state,
                status = statuses.forCategory(category),
                onClick =
                    category.navigationCallback(
                        onNavigateToBattery,
                        onNavigateToNetwork,
                        onNavigateToThermal,
                        onNavigateToStorage,
                    ),
                shape = shape ?: RoundedCornerShape(tokens.homeStatusTileCornerRadius),
                compact = compact,
                modifier = tileModifier,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(tokens.homeStatusTileGap)) {
            if (singleColumn) {
                HomeStatusTileCategory.entries.forEach { category ->
                    tile(category, Modifier.fillMaxWidth(), null)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(-HOME_STATUS_TILE_OVERLAP),
                ) {
                    tile(
                        HomeStatusTileCategory.BATTERY,
                        Modifier.weight(HOME_BATTERY_TILE_WEIGHT).fillMaxHeight().zIndex(1f),
                        HomeTileShape(HomeTileEdge.BATTERY),
                    )
                    tile(
                        HomeStatusTileCategory.THERMAL,
                        Modifier.weight(HOME_THERMAL_TILE_WEIGHT).fillMaxHeight(),
                        HomeTileShape(HomeTileEdge.THERMAL),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(tokens.homeStatusTileGap),
                ) {
                    tile(
                        HomeStatusTileCategory.STORAGE,
                        Modifier.weight(HOME_STORAGE_TILE_WEIGHT).fillMaxHeight(),
                        null,
                    )
                    tile(
                        HomeStatusTileCategory.NETWORK,
                        Modifier.weight(HOME_NETWORK_TILE_WEIGHT).fillMaxHeight(),
                        null,
                    )
                }
            }
        }
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
    shape: Shape,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.uiTokens
    val typeScale = MaterialTheme.homeStatusTileTypeScale
    val background =
        when (category) {
            HomeStatusTileCategory.BATTERY -> HomeCream
            HomeStatusTileCategory.THERMAL -> HomePeach
            HomeStatusTileCategory.STORAGE -> HomeGraphite
            HomeStatusTileCategory.NETWORK -> HomeStone
        }
    val foreground = if (category == HomeStatusTileCategory.STORAGE) HomeCream else HomeInk
    val content = category.content(state, status)
    val badgeColor =
        if (category == HomeStatusTileCategory.NETWORK && !state.networkState.isConnected) {
            HomeCream
        } else {
            statusColor(status)
        }
    Surface(
        onClick = onClick,
        modifier =
            modifier.heightIn(
                min =
                    if (compact) {
                        tokens.homeCompactStatusTileHeight
                    } else {
                        tokens.homeStatusTileHeight
                    },
            ),
        shape = shape,
        color = background,
        contentColor = foreground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(homeStatusTilePadding(category, shape, compact)),
            verticalArrangement = Arrangement.spacedBy(tokens.homeStatusTileStatusGap),
        ) {
            Text(text = category.label(), style = typeScale.category)
            if (category == HomeStatusTileCategory.NETWORK) {
                Spacer(Modifier.height(tokens.homeStatusTileNetworkValueTopGap))
            }
            TileValueLine(
                value = content.value,
                suffix = content.suffix,
                valueStyle = category.valueStyle(),
                suffixStyle = typeScale.suffix,
                textColor = foreground,
            )
            if (category == HomeStatusTileCategory.STORAGE) {
                val context = LocalContext.current
                val storageUsageLabel = stringResource(R.string.a11y_storage_usage_progress)
                Text(
                    text =
                        stringResource(
                            R.string.home_storage_capacity,
                            formatStorageSize(context, state.storageState.usedBytes),
                            formatStorageSize(context, state.storageState.totalBytes),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )
                LinearProgressIndicator(
                    progress = { (state.storageState.usagePercent / 100f).coerceIn(0f, 1f) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .semantics { contentDescription = storageUsageLabel },
                    color = HomeCream,
                    trackColor = HomeCream.copy(alpha = 0.25f),
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
            Text(
                text = content.status,
                style = typeScale.status,
                color = HomeInk,
                modifier =
                    Modifier
                        .background(
                            badgeColor,
                            RoundedCornerShape(50),
                        ).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun homeStatusTilePadding(
    category: HomeStatusTileCategory,
    shape: Shape,
    compact: Boolean,
): PaddingValues {
    val tokens = MaterialTheme.uiTokens
    val curved = shape is HomeTileShape
    val verticalPadding =
        if (compact) tokens.homeCompactStatusTileVerticalPadding else tokens.homeStatusTileCategoryTop
    return PaddingValues(
        start =
            tokens.homeStatusTilePaddingHorizontal +
                if (curved && category == HomeStatusTileCategory.THERMAL) HOME_TILE_EDGE_BEND else 0.dp,
        end =
            tokens.homeStatusTilePaddingHorizontal +
                if (curved && category == HomeStatusTileCategory.BATTERY) HOME_TILE_EDGE_BEND else 0.dp,
        top = verticalPadding,
        bottom = verticalPadding,
    )
}

@Composable
private fun HomeStatusTileCategory.valueStyle(): TextStyle {
    val typeScale = MaterialTheme.homeStatusTileTypeScale
    return when (this) {
        HomeStatusTileCategory.BATTERY -> typeScale.batteryValue
        HomeStatusTileCategory.NETWORK -> typeScale.networkValue
        else -> typeScale.standardValue
    }
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
                status = stringResource(networkSignalStatusLabelRes(state.networkState)),
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
                status =
                    stringResource(
                        when (status) {
                            HealthStatus.HEALTHY -> R.string.status_healthy
                            HealthStatus.FAIR -> R.string.home_thermal_warm
                            HealthStatus.POOR -> R.string.home_thermal_hot
                            HealthStatus.CRITICAL -> R.string.home_thermal_critical
                        },
                    ),
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
                suffix = stringResource(R.string.home_storage_free, formattedSize.suffix.orEmpty()),
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
            modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
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

internal fun networkSignalStatusLabelRes(networkState: NetworkState): Int =
    if (!networkState.isConnected) {
        R.string.score_unrated
    } else {
        when (networkState.signalQuality) {
            SignalQuality.EXCELLENT -> R.string.home_network_signal_excellent
            SignalQuality.GOOD -> R.string.home_network_signal_good
            SignalQuality.FAIR -> R.string.home_network_signal_fair
            SignalQuality.POOR -> R.string.home_network_signal_poor
            SignalQuality.NO_SIGNAL -> R.string.home_network_signal_none
        }
    }

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
