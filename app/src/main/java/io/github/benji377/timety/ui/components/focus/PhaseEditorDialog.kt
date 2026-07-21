package io.github.benji377.timety.ui.components.focus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.components.common.NeoButton as Button
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField as OutlinedTextField


/**
 * Dialog for adding or editing a session phase: its type (focus or rest) and duration in
 * minutes, capped at [maxNodeMins]. Pass [initialPhase] to edit an existing phase, or null to
 * create a new one. [onDelete] is only offered when editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseEditorDialog(
    initialPhase: SessionPhaseEntity?,
    maxNodeMins: Int = AppTheme.MAX_NODE_MINS,
    onDismiss: () -> Unit,
    onSave: (PhaseType, Int) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val isNew = initialPhase == null
    var selectedType by remember { mutableStateOf(initialPhase?.type ?: PhaseType.FOCUS) }
    var timeText by remember { mutableStateOf(initialPhase?.durationMinutes?.toString() ?: "25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) stringResource(R.string.focusPhasesTitleAdd) else stringResource(R.string.focusPhasesTitleEdit)) },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = selectedType == PhaseType.FOCUS,
                        onClick = { selectedType = PhaseType.FOCUS },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Filled.CenterFocusStrong, contentDescription = null) },
                    ) {
                        Text(stringResource(R.string.focusLabelDefault))
                    }
                    SegmentedButton(
                        selected = selectedType == PhaseType.REST,
                        onClick = { selectedType = PhaseType.REST },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Filled.Coffee, contentDescription = null) },
                    ) {
                        Text(stringResource(R.string.focusLabelRest))
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.space2XLarge))
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text(stringResource(R.string.focusPhaseLabelDuration)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(stringResource(R.string.focusPhaseLabelDurationSuffix)) },
                )
            }
        },
        dismissButton = {
            Row {
                if (!isNew && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.commonLabelDelete), color = ErrorColor)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val mins = timeText.toIntOrNull()
                if (mins != null && mins > 0) {
                    val clamped = if (mins > maxNodeMins) maxNodeMins else mins
                    onSave(selectedType, clamped)
                }
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
    )
}
