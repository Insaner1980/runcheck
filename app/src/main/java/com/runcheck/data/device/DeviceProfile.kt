package com.runcheck.data.device

import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.SignConvention

data class DeviceProfile(
    val manufacturer: String = "",
    val model: String = "",
    val apiLevel: Int = 0,
    val currentNowReliable: Boolean = false,
    val currentNowUnit: CurrentUnit = CurrentUnit.MILLIAMPS,
    val currentNowSignConvention: SignConvention = SignConvention.POSITIVE_CHARGING,
    val cycleCountAvailable: Boolean = false,
    val thermalZonesAvailable: List<String> = emptyList(),
    val storageHealthAvailable: Boolean = true,
) {
    val deviceId: String
        get() = "${manufacturer}_${model}_$apiLevel".lowercase()
}
