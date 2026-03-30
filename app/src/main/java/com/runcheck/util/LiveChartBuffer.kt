package com.runcheck.util

const val LIVE_CHART_MAX_POINTS = 60

fun MutableList<Float>.appendLiveValue(value: Float) {
    add(value)
    if (size > LIVE_CHART_MAX_POINTS) removeAt(0)
}
