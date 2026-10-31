package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme

/**
 * Launch prompt for "Alarms & reminders" access. Three answers: open the system page, ask again
 * next launch ([onDismiss]), or never ask again ([onNever]). The main action gets its own full-width
 * line, and the other two wrap when a translation makes them too wide for one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExactAlarmPromptDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    onNever: () -> Unit,
) {
    NeoDialog(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.exactAlarmPromptTitle),
            fontSize = AppTheme.fsHeadingSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(AppTheme.spaceMedium))
        Text(
            stringResource(R.string.exactAlarmPromptBody),
            fontSize = AppTheme.fsBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
        NeoButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exactAlarmPromptConfirm))
        }
        Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NeoTextButton(onClick = onNever, text = stringResource(R.string.exactAlarmPromptNever))
            NeoTextButton(onClick = onDismiss, text = stringResource(R.string.exactAlarmPromptDismiss))
        }
    }
}
