package com.runcheck.data.battery

import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import kotlinx.coroutines.flow.Flow

interface BatteryDataSource {
    fun getCurrentNow(): Flow<MeasuredValue<Int>>

    fun getVoltage(): Flow<Int>

    fun getTemperature(): Flow<Float>

    fun getHealth(): Flow<BatteryHealth>

    fun getCycleCount(): Flow<Int?>

    fun getHealthPercent(): Flow<Int?>

    fun getChargingStatus(): Flow<ChargingStatus>

    fun getPlugType(): Flow<PlugType>

    fun getLevel(): Flow<Int>

    fun getTechnology(): Flow<String>

    fun getChargeCounter(): Flow<Int?>
}
