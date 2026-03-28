package com.runcheck.ui.components.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.theme.BottomSheetShape
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    content: InfoSheetContent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = BottomSheetShape,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.lg)
                    .padding(bottom = MaterialTheme.spacing.xl),
        ) {
            Text(
                text = stringResource(content.title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            Text(
                text = stringResource(content.explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                    Text(
                        text = stringResource(R.string.info_normal_range_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Text(
                        text = stringResource(content.normalRange),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Text(
                text = stringResource(R.string.info_why_it_matters_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text = stringResource(content.whyItMatters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            content.deeperDetail?.let { detailRes ->
                var expanded by rememberSaveable { mutableStateOf(false) }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text =
                            stringResource(
                                if (expanded) R.string.info_show_less else R.string.info_learn_more,
                            ),
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = if (reducedMotion) EnterTransition.None else expandVertically(),
                    exit = if (reducedMotion) ExitTransition.None else shrinkVertically(),
                ) {
                    Text(
                        text = stringResource(detailRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                    )
                }
            }
        }
    }
}
