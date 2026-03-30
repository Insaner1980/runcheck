package com.runcheck.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.runcheck.R
import com.runcheck.ui.components.info.InfoIcon
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing

@Composable
fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    showDivider: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    copyable: Boolean = false,
    onInfoClick: (() -> Unit)? = null,
) {
    val truncate = maxLines < Int.MAX_VALUE

    val clickModifier =
        if (copyable) {
            val context = LocalContext.current
            val copiedMessage = stringResource(R.string.copied_to_clipboard)
            val clickLabel = stringResource(R.string.a11y_copy_to_clipboard)
            remember(label, value) {
                Modifier.clickable(onClickLabel = clickLabel) {
                    copyToClipboard(context, label, value)
                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Modifier
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(clickModifier)
                .semantics(mergeDescendants = true) {},
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onInfoClick != null) {
                    InfoIcon(onClick = onInfoClick)
                }
            }
            Text(
                text = value,
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontFamily = MaterialTheme.numericFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = valueColor,
                maxLines = maxLines,
                overflow = if (truncate) TextOverflow.Ellipsis else TextOverflow.Clip,
                textAlign = if (truncate) TextAlign.End else TextAlign.Unspecified,
                modifier = if (truncate) Modifier.weight(1f, fill = false) else Modifier,
            )
        }

        if (showDivider) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            )
        }
    }
}

private fun copyToClipboard(
    context: Context,
    label: String,
    value: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
