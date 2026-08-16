package com.runcheck.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.common.LifecycleStartStopEffect

data class ObservedScreenState<S>(
    val uiState: S,
    val isRefreshing: Boolean,
    val loadingDescription: String,
)

@Composable
fun <S> observedScreenState(
    uiState: S,
    isRefreshing: Boolean,
): ObservedScreenState<S> =
    ObservedScreenState(
        uiState = uiState,
        isRefreshing = isRefreshing,
        loadingDescription = stringResource(R.string.a11y_loading),
    )

@Composable
fun ObservedScreenScaffold(
    onStart: () -> Unit,
    onStop: () -> Unit,
    topBar: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    LifecycleStartStopEffect(onStart = onStart, onStop = onStop)
    Column(modifier = modifier.fillMaxSize()) {
        topBar()
        content()
    }
}
