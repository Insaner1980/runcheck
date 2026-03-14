package com.runcheck.domain.model

data class DeviceProfileInfo(
    val manufacturer: String,
    val model: String,
    val apiLevel: Int,
    val currentNowReliable: Boolean,
    val currentNowUnit: CurrentUnit,
    val currentNowSignConvention: SignConvention,
    val cycleCountAvailable: Boolean,
    val batteryHealthPercentAvailable: Boolean,
    val thermalZonesAvailable: List<String>,
    val storageHealthAvailable: Boolean
) {
    val deviceId: String
        get() = "${manufacturer}_${model}_$apiLevel".lowercase()
}
