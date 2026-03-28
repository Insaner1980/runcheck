package com.runcheck.domain.model

data class MeasuredValue<T>(
    val value: T,
    val confidence: Confidence,
)
