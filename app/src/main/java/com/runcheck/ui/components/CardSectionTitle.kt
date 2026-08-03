package com.runcheck.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CardSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) = SectionHeader(text = text, modifier = modifier)
