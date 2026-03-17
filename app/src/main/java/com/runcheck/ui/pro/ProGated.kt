package com.runcheck.ui.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.pro.ProFeature
import com.runcheck.pro.ProState

@Composable
fun ProGated(
    feature: ProFeature,
    proState: ProState,
    onNavigateToProUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    lockedContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (proState.hasFeature(feature)) {
        content()
    } else if (lockedContent != null) {
        lockedContent()
    } else {
        ProLockedOverlay(
            onNavigateToProUpgrade = onNavigateToProUpgrade,
            modifier = modifier
        )
    }
}

@Composable
fun ProLockedOverlay(
    onNavigateToProUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(onClick = onNavigateToProUpgrade) {
                Text(stringResource(R.string.pro_unlock_button))
            }
        }
    }
}
