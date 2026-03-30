package com.runcheck.di

import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.rules.AppBatteryImpactRule
import com.runcheck.domain.insights.rules.BatteryDegradationTrendRule
import com.runcheck.domain.insights.rules.ChargerPerformanceRule
import com.runcheck.domain.insights.rules.HeatAcceleratedBatteryWearRule
import com.runcheck.domain.insights.rules.HeavyAppUsageRule
import com.runcheck.domain.insights.rules.NetworkDrivenBatteryDrainRule
import com.runcheck.domain.insights.rules.NetworkSignalPatternRule
import com.runcheck.domain.insights.rules.RecurringThermalThrottlingRule
import com.runcheck.domain.insights.rules.StoragePressureImpactRule
import com.runcheck.domain.insights.rules.StoragePressureProjectionRule
import com.runcheck.domain.insights.rules.ThermalPatternDetectionRule
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface InsightsModule {
    @Binds
    @IntoSet
    fun bindBatteryDegradationTrendRule(rule: BatteryDegradationTrendRule): InsightRule

    @Binds
    @IntoSet
    fun bindAppBatteryImpactRule(rule: AppBatteryImpactRule): InsightRule

    @Binds
    @IntoSet
    fun bindChargerPerformanceRule(rule: ChargerPerformanceRule): InsightRule

    @Binds
    @IntoSet
    fun bindStoragePressureProjectionRule(rule: StoragePressureProjectionRule): InsightRule

    @Binds
    @IntoSet
    fun bindRecurringThermalThrottlingRule(rule: RecurringThermalThrottlingRule): InsightRule

    @Binds
    @IntoSet
    fun bindHeavyAppUsageRule(rule: HeavyAppUsageRule): InsightRule

    @Binds
    @IntoSet
    fun bindNetworkSignalPatternRule(rule: NetworkSignalPatternRule): InsightRule

    @Binds
    @IntoSet
    fun bindNetworkDrivenBatteryDrainRule(rule: NetworkDrivenBatteryDrainRule): InsightRule

    @Binds
    @IntoSet
    fun bindHeatAcceleratedBatteryWearRule(rule: HeatAcceleratedBatteryWearRule): InsightRule

    @Binds
    @IntoSet
    fun bindStoragePressureImpactRule(rule: StoragePressureImpactRule): InsightRule

    @Binds
    @IntoSet
    fun bindThermalPatternDetectionRule(rule: ThermalPatternDetectionRule): InsightRule
}
