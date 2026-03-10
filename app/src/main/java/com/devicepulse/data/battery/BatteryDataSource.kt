package com.devicepulse.data.battery

import com.devicepulse.domain.model.BatteryHealth
import com.devicepulse.domain.model.ChargingStatus
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.MeasuredValue
import com.devicepulse.domain.model.PlugType
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
}
