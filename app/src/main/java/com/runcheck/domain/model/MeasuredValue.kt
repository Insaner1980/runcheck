package com.devicepulse.domain.model

data class MeasuredValue<T>(
    val value: T,
    val confidence: Confidence
)
