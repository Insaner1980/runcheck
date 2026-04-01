package com.runcheck.ui.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens

@Composable
fun PurchaseThankYouContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.uiTokens
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(tokens.celebrationIcon),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

        Text(
            text = stringResource(R.string.pro_thank_you_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

        Text(
            text = stringResource(R.string.pro_thank_you_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

        Button(
            onClick = onDismiss,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(tokens.primaryButtonHeight),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text = stringResource(R.string.pro_thank_you_continue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
