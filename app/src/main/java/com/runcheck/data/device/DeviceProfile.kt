package com.runcheck.data.device

import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.SignConvention

data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val apiLevel: Int,
    val currentNowReliable: Boolean,
    val currentNowUnit: CurrentUnit,
    val currentNowSignConvention: SignConvention,
    val cycleCountAvailable: Boolean,
    val thermalZonesAvailable: List<String>,
    val storageHealthAvailable: Boolean,
) {
    val deviceId: String
        get() = "${manufacturer}_${model}_$apiLevel".lowercase()
}
