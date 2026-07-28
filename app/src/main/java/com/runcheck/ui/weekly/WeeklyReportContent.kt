package com.runcheck.ui.weekly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.model.WeeklyReport
import com.runcheck.domain.model.WeeklyReportAvailability
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.RuncheckProgressSpinner
import com.runcheck.ui.components.SecondaryActionLink
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.StatBlock
import com.runcheck.ui.components.StatusTone
import com.runcheck.ui.theme.runcheckCardBorder
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun WeeklyReportContent(
    report: WeeklyReport,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        item {
            WeeklyReportHero(report)
        }
        item {
            ReportSection(
                title = stringResource(R.string.weekly_report_battery),
                lines =
                    when (batteryProjection(report.battery)) {
                        WeeklyReportMetricProjection.Unavailable -> {
                            listOf(stringResource(R.string.weekly_report_metric_unavailable))
                        }

                        WeeklyReportMetricProjection.Available -> {
                            listOf(
                                report.battery.averageDischargePercentPerHour?.let {
                                    stringResource(R.string.weekly_report_discharge_rate, it)
                                } ?: stringResource(R.string.weekly_report_metric_unavailable),
                                stringResource(
                                    R.string.weekly_report_charge_changes,
                                    report.battery.chargePercentChange,
                                    report.battery.dischargePercentChange,
                                ),
                                report.battery.healthPercentChange?.let {
                                    stringResource(R.string.weekly_report_health_change, it)
                                } ?: stringResource(R.string.weekly_report_health_unavailable),
                            )
                        }
                    },
            )
        }
        item {
            ReportSection(
                title = stringResource(R.string.weekly_report_storage),
                lines =
                    listOf(
                        report.storage.availableBytesChange?.let {
                            stringResource(R.string.weekly_report_storage_change_mb, it / (1024 * 1024))
                        } ?: stringResource(R.string.weekly_report_metric_unavailable),
                    ),
            )
        }
        item {
            ReportSection(
                title = stringResource(R.string.weekly_report_thermal),
                lines =
                    when (thermalProjection(report.thermal)) {
                        WeeklyReportMetricProjection.Unavailable -> {
                            listOf(stringResource(R.string.weekly_report_metric_unavailable))
                        }

                        WeeklyReportMetricProjection.Available -> {
                            listOf(
                                stringResource(
                                    R.string.weekly_report_throttling_events,
                                    report.thermal.throttlingEventCount,
                                ),
                                report.thermal.highestThermalStatus?.let {
                                    stringResource(R.string.weekly_report_highest_thermal_status, it)
                                } ?: stringResource(R.string.weekly_report_metric_unavailable),
                            )
                        }
                    },
            )
        }
        item {
            ReportSection(
                title = stringResource(R.string.weekly_report_speed),
                lines =
                    if (report.speed.testCount == 0) {
                        listOf(stringResource(R.string.weekly_report_no_speed_tests))
                    } else {
                        listOf(
                            stringResource(R.string.weekly_report_speed_count, report.speed.testCount),
                            stringResource(
                                R.string.weekly_report_speed_medians,
                                requireNotNull(report.speed.medianDownloadMbps),
                                requireNotNull(report.speed.medianUploadMbps),
                                requireNotNull(report.speed.medianLatencyMs),
                            ),
                        )
                    },
            )
        }
        item {
            SectionHeader(text = stringResource(R.string.weekly_report_apps))
        }
        if (report.topApps.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.weekly_report_no_app_usage),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(report.topApps, key = { it.packageName }) { app ->
                val foregroundLine =
                    stringResource(
                        R.string.weekly_report_foreground_minutes,
                        (app.foregroundTimeMs / 60_000.0).roundToInt(),
                    )
                val endpointAttributedLine =
                    if (app.availability == WeeklyReportAvailability.ESTIMATED) {
                        stringResource(R.string.weekly_report_app_usage_endpoint_attributed)
                    } else {
                        null
                    }
                ReportSection(
                    title = app.appLabel?.takeIf(String::isNotBlank) ?: app.packageName,
                    lines = listOfNotNull(foregroundLine, endpointAttributedLine),
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
fun WeeklyReportSummaryContent(
    state: WeeklyReportUiState,
    onOpenReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        border = runcheckCardBorder(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            when (state) {
                WeeklyReportUiState.Loading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        RuncheckProgressSpinner(
                            contentDescription = stringResource(R.string.a11y_loading),
                        )
                        Text(
                            text = stringResource(R.string.weekly_report_summary_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                WeeklyReportUiState.Locked -> {
                    Text(
                        text = stringResource(R.string.weekly_report_summary_locked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is WeeklyReportUiState.Error -> {
                    Text(
                        text = state.message.resolve(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                is WeeklyReportUiState.Success -> {
                    val summary = weeklyReportSummaryProjection(state.report)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        StatBlock(
                            label = stringResource(R.string.weekly_report_summary_days),
                            value = summary.monitoredDays.toString(),
                            unit = stringResource(R.string.weekly_report_summary_days_unit),
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = stringResource(R.string.weekly_report_summary_samples),
                            value = summary.sampleCount.toString(),
                            unit = stringResource(R.string.weekly_report_summary_samples_unit),
                            modifier = Modifier.weight(1f),
                        )
                        StatBlock(
                            label = stringResource(R.string.weekly_report_summary_tests),
                            value = summary.speedTestCount.toString(),
                            unit = stringResource(R.string.weekly_report_summary_tests_unit),
                            modifier = Modifier.weight(1f),
                            status = availabilityText(summary.availability),
                            statusTone =
                                when (summary.availability) {
                                    WeeklyReportAvailability.AVAILABLE -> StatusTone.HEALTHY
                                    WeeklyReportAvailability.ESTIMATED -> StatusTone.FAIR
                                    WeeklyReportAvailability.UNAVAILABLE -> StatusTone.UNAVAILABLE
                                },
                        )
                    }
                }
            }

            SecondaryActionLink(
                label = stringResource(R.string.weekly_report_summary_action),
                onClick = onOpenReport,
            )
        }
    }
}

@Composable
private fun WeeklyReportHero(report: WeeklyReport) {
    val start =
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(report.period.startInclusive))
    val end =
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date.from(report.period.endExclusive))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = runcheckCardColors(),
        border = runcheckCardBorder(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.weekly_report_period_range, start, end),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text =
                    stringResource(
                        R.string.weekly_report_coverage,
                        report.coverage.monitoredDays,
                        report.coverage.sampleCount,
                    ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = availabilityText(report.coverage.availability),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ReportSection(
    title: String,
    lines: List<String>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        border = runcheckCardBorder(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun availabilityText(availability: WeeklyReportAvailability): String =
    stringResource(
        when (availability) {
            WeeklyReportAvailability.AVAILABLE -> R.string.weekly_report_availability_available
            WeeklyReportAvailability.ESTIMATED -> R.string.weekly_report_availability_estimated
            WeeklyReportAvailability.UNAVAILABLE -> R.string.weekly_report_availability_unavailable
        },
    )
