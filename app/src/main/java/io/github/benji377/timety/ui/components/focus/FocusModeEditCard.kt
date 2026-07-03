package io.github.benji377.timety.ui.components.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.PhaseType
import io.github.benji377.timety.data.model.focus.SessionPhaseEntity
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.theme.WifiOffColor

/**
 * A card showing a focus mode: read-only overview (name, timeline) with edit/delete actions for
 * custom modes, or an inline editor (name field + phase chip timeline) when editing/creating.
 * Mirrors `widgets/focus/focus_mode_edit_card.dart` (`ModeEditCard`).
 */
@Composable
fun FocusModeEditCard(
    mode: FocusModeEntity,
    phases: List<SessionPhaseEntity>,
    isNewMode: Boolean = false,
    maxNodeMins: Int = AppTheme.maxNodeMins,
    onCancelNew: (() -> Unit)? = null,
    onSaveNew: (() -> Unit)? = null,
    onSave: (FocusModeEntity, List<SessionPhaseEntity>) -> Unit,
    onDelete: (FocusModeEntity) -> Unit,
) {
    var isEditing by remember(mode.id) { mutableStateOf(isNewMode) }
    var name by remember(mode.id) { mutableStateOf(if (isNewMode) "" else mode.name) }
    var tempPhases by remember(mode.id) { mutableStateOf(phases) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var phaseDialogOpen by remember { mutableStateOf(false) }
    var editingPhaseIndex by remember { mutableStateOf<Int?>(null) }

    // Keep the working copy in sync with freshly-loaded phases (async Room Flow) as long as the
    // user isn't mid-edit, mirroring Flutter's `didUpdateWidget` reset-on-mode-change behavior.
    LaunchedEffect(phases) {
        if (!isEditing) tempPhases = phases
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEditing) 4.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(AppTheme.spaceLarge)) {
            if (isEditing) {
                EditorView(
                    name = name,
                    onNameChange = { name = it },
                    tempPhases = tempPhases,
                    maxNodeMins = maxNodeMins,
                    onPhaseTapped = { index ->
                        editingPhaseIndex = index
                        phaseDialogOpen = true
                    },
                    onAddPhaseTapped = {
                        editingPhaseIndex = null
                        phaseDialogOpen = true
                    },
                    onCancel = {
                        if (isNewMode) {
                            onCancelNew?.invoke()
                        } else {
                            name = mode.name
                            tempPhases = phases
                            isEditing = false
                        }
                    },
                    onSave = {
                        if (name.trim().isNotEmpty() && tempPhases.isNotEmpty()) {
                            onSave(mode.copy(name = name.trim()), tempPhases)
                            if (isNewMode) {
                                onSaveNew?.invoke()
                            } else {
                                isEditing = false
                            }
                        }
                    },
                )
            } else {
                OverviewView(
                    mode = mode,
                    phases = phases,
                    onEdit = { isEditing = true },
                    onDeleteRequested = { showDeleteConfirm = true },
                )
            }
        }
    }

    ConfirmationDialog(
        visible = showDeleteConfirm,
        title = stringResource(R.string.focusModeDialogDeleteTitle),
        content = stringResource(R.string.focusModeDialogDeleteContent),
        confirmLabel = stringResource(R.string.commonLabelDelete),
        confirmColor = ErrorColor,
        onConfirm = {
            onDelete(mode)
            showDeleteConfirm = false
        },
        onDismiss = { showDeleteConfirm = false },
    )

    if (phaseDialogOpen) {
        val index = editingPhaseIndex
        PhaseEditorDialog(
            initialPhase = index?.let { tempPhases.getOrNull(it) },
            maxNodeMins = maxNodeMins,
            onDismiss = { phaseDialogOpen = false },
            onSave = { type, mins ->
                val newPhase = SessionPhaseEntity(
                    modeId = mode.id,
                    type = type,
                    durationMinutes = mins,
                    orderIndex = index ?: tempPhases.size,
                )
                tempPhases = if (index == null) {
                    tempPhases + newPhase
                } else {
                    tempPhases.toMutableList().apply { set(index, newPhase) }
                }
                phaseDialogOpen = false
            },
            onDelete = index?.let {
                {
                    tempPhases = tempPhases.toMutableList().apply { removeAt(it) }
                    phaseDialogOpen = false
                }
            },
        )
    }
}

@Composable
private fun OverviewView(
    mode: FocusModeEntity,
    phases: List<SessionPhaseEntity>,
    onEdit: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(localizedFocusModeName(mode), fontWeight = FontWeight.Bold, fontSize = AppTheme.fsHeadingSmall)
            Spacer(modifier = Modifier.height(AppTheme.spaceXSmall))
            Text(
                text = if (mode.isSystem) stringResource(R.string.focusModeLabelTypeSystem) else stringResource(R.string.focusModeLabelTypeCustom),
                color = if (mode.isSystem) MaterialTheme.colorScheme.onSurfaceVariant else TaskColor,
                fontSize = AppTheme.fsBodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
        if (mode.isSystem) {
            Box(modifier = Modifier.padding(AppTheme.spaceSmall)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.Gray)
            }
        } else {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.focusModeLabelEdit), tint = TaskColor)
                }
                IconButton(onClick = onDeleteRequested) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.commonLabelDelete), tint = ErrorColor)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
    ModeTimeline(phases = phases, currentPhaseIndex = 0, isRunning = false)
}

@Composable
private fun EditorView(
    name: String,
    onNameChange: (String) -> Unit,
    tempPhases: List<SessionPhaseEntity>,
    maxNodeMins: Int,
    onPhaseTapped: (Int) -> Unit,
    onAddPhaseTapped: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.focusModeLabelEditing),
            fontWeight = FontWeight.Bold,
            fontSize = AppTheme.fsBodyLarge,
            color = TaskColor,
        )
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.commonLabelCancel))
        }
    }
    Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Mode Name") },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(AppTheme.spaceXLarge))

    Text(stringResource(R.string.focusModeLabelTimeline), fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
    Text(stringResource(R.string.focusModeLabelTimelineDesc), fontSize = AppTheme.fsBodySmall, color = Color.Gray)
    Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

    val flexLabel = stringResource(R.string.focusModeFlex)
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        tempPhases.forEachIndexed { index, phase ->
            Row {
                PhaseChip(phase = phase, flexLabel = flexLabel, onClick = { onPhaseTapped(index) })
                Box(modifier = Modifier.width(24.dp).height(2.dp).background(Color.LightGray))
            }
        }
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(onClick = onAddPhaseTapped),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = WifiOffColor)
        }
    }

    Spacer(modifier = Modifier.height(AppTheme.spaceXLarge))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.commonLabelCancel)) }
        Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
        ElevatedButton(onClick = onSave) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(AppTheme.iconSizeSmall))
            Spacer(modifier = Modifier.width(AppTheme.spaceXSmall))
            Text(stringResource(R.string.commonLabelSave))
        }
    }
}

@Composable
private fun PhaseChip(phase: SessionPhaseEntity, flexLabel: String, onClick: () -> Unit) {
    val isFocus = phase.type == PhaseType.FOCUS
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (isFocus) FocusColor else WarningColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${if (phase.durationMinutes == -1) flexLabel else phase.durationMinutes.toString()}\nm",
            textAlign = TextAlign.Center,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = AppTheme.fsCaption,
        )
    }
}
