package com.runcheck.ui.chart

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.decodePersistedConnectionType
import com.runcheck.domain.model.is5gNetworkSubtype
import com.runcheck.ui.common.connectionDisplayLabel

enum class NetworkSignalFamily {
    WIFI,
    FIVE_G,
    CELLULAR,
    UNKNOWN,
}

data class NetworkSignalContext(
    val connectionType: ConnectionType?,
    val networkSubtype: String?,
) {
    val family: NetworkSignalFamily
        get() =
            when (connectionType) {
                ConnectionType.WIFI -> {
                    NetworkSignalFamily.WIFI
                }

                ConnectionType.CELLULAR -> {
                    if (is5gNetworkSubtype(networkSubtype)) NetworkSignalFamily.FIVE_G else NetworkSignalFamily.CELLULAR
                }

                else -> {
                    NetworkSignalFamily.UNKNOWN
                }
            }
}

private fun NetworkReading.signalContext(): NetworkSignalContext? =
    if (signalDbm == null || type == ConnectionType.NONE.name) {
        null
    } else {
        NetworkSignalContext(decodePersistedConnectionType(type), networkSubtype)
    }

internal fun isContinuousNetworkSignal(
    previous: NetworkReading,
    current: NetworkReading,
): Boolean {
    val previousFamily = previous.signalContext()?.family ?: return false
    val currentFamily = current.signalContext()?.family ?: return false
    return previousFamily != NetworkSignalFamily.UNKNOWN && previousFamily == currentFamily
}

internal data class NetworkSignalSeries(
    val points: List<Pair<Long, Float>>,
    val contexts: List<NetworkSignalContext>,
    val families: Set<NetworkSignalFamily>,
    val lineBreakIndices: Set<Int>,
)

internal fun buildNetworkSignalSeries(
    history: List<NetworkReading>,
    maxPoints: Int,
): NetworkSignalSeries {
    val points = mutableListOf<Pair<Long, Float>>()
    val contexts = mutableListOf<NetworkSignalContext>()
    val segments = mutableListOf<Int>()
    var segment = 0
    history.forEachIndexed { index, reading ->
        if (index > 0 && !isContinuousNetworkSignal(history[index - 1], reading)) segment++
        val context = reading.signalContext() ?: return@forEachIndexed
        val dbm = reading.signalDbm ?: return@forEachIndexed
        points.add(reading.timestamp to dbm.toFloat())
        contexts.add(context)
        segments.add(segment)
    }
    val sampled = points.downsamplePairs(maxPoints)
    // downsamplePairs retains the original pair instances in order. Identity, not timestamp/value
    // equality, keeps duplicate observations attached to their own context and original segment.
    var cursor = 0
    val retainedIndices =
        sampled.map { point ->
            while (points[cursor] !== point) cursor++
            cursor++
            cursor - 1
        }
    val breaks =
        retainedIndices
            .zipWithNext()
            .mapIndexedNotNull { index, (previous, current) ->
                (index + 1).takeIf { segments[previous] != segments[current] }
            }.toSet()
    return NetworkSignalSeries(
        points = sampled,
        contexts = retainedIndices.map { contexts[it] },
        families = contexts.map { it.family }.toSet(),
        lineBreakIndices = breaks,
    )
}

@Composable
fun networkSignalContextLabels(contexts: List<NetworkSignalContext>): List<String> =
    contexts.map { context ->
        context.connectionType?.let { connectionDisplayLabel(it, null, context.networkSubtype) }
            ?: stringResource(R.string.fallback_unknown)
    }

internal fun networkSignalHistoryContextResource(families: Set<NetworkSignalFamily>): Int? =
    when {
        families.isEmpty() -> {
            null
        }

        families.size > 1 -> {
            R.string.network_signal_history_mixed
        }

        else -> {
            when (families.single()) {
                NetworkSignalFamily.WIFI -> R.string.network_signal_history_wifi
                NetworkSignalFamily.FIVE_G -> R.string.network_signal_history_5g
                NetworkSignalFamily.CELLULAR -> R.string.network_signal_history_cellular
                NetworkSignalFamily.UNKNOWN -> R.string.network_signal_history_unknown
            }
        }
    }

@Composable
fun networkSignalHistoryContextLabel(families: Set<NetworkSignalFamily>): String? =
    networkSignalHistoryContextResource(families)?.let { stringResource(it) }
