package io.github.benji377.timety.ui.components.focus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusModeEntity
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.NeoDateTimePickerDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import io.github.benji377.timety.ui.components.common.NeoButton as Button
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField as OutlinedTextField

/** Default length a backfilled session is proposed with, matching one classic pomodoro. */
private const val DEFAULT_BACKFILL_MINUTES = 25L

/**
 * Dialog for logging or editing a focus session: pick a mode, a tag, and a start/end date and
 * time, then call [onSave]. Rejects an end time before the start time.
 *
 * The same dialog covers the three surfaces that need it. Passing [fixedTargetLabel] replaces the
 * tag picker with a read-only target row, for sessions already attributed to a task or habit;
 * passing [onDelete] adds a Delete action, for sessions that are already logged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditorDialog(
    title: String,
    modes: List<FocusModeEntity>,
    tags: List<FocusTagEntity>,
    onDismiss: () -> Unit,
    onSave: (mode: FocusModeEntity, startTime: Instant, endTime: Instant, tagId: String?) -> Unit,
    icon: ImageVector = Icons.Filled.History,
    initialModeId: String? = null,
    initialSelectedTagId: String? = null,
    initialStartTime: Instant? = null,
    initialEndTime: Instant? = null,
    fixedTargetLabel: String? = null,
    onDelete: (() -> Unit)? = null,
) {
    // The pick is held as an id, not an entity: the modes list is a live Room flow, so a
    // re-emission while the dialog is open must not reset (or outdate) the user's selection.
    var selectedModeId by remember(initialModeId) { mutableStateOf(initialModeId) }
    val selectedMode = modes.firstOrNull { it.id == selectedModeId } ?: modes.firstOrNull()
    var selectedTagId by remember { mutableStateOf(initialSelectedTagId) }
    // Proposed times are truncated to the minute: the pickers only offer whole minutes, so a
    // proposal carrying seconds would make an untouched field and an edited one disagree by up
    // to a minute in the logged duration.
    val now = remember { Instant.now().truncatedTo(ChronoUnit.MINUTES) }
    var startDateTime by remember {
        mutableStateOf(initialStartTime ?: now.minusSeconds(DEFAULT_BACKFILL_MINUTES * 60))
    }
    var endDateTime by remember { mutableStateOf(initialEndTime ?: now) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var modeExpanded by remember { mutableStateOf(false) }
    var tagExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val endBeforeStartError = stringResource(R.string.dialogTimeMachineErrorEndBeforeStart)
    val zone = ZoneId.systemDefault()
    val locale = LocalLocale.current.platformLocale
    val dateTimeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale).withZone(zone)
    }

    // Date+time picker flow shared by both start/end fields: pick a date, then a time.
    var pickerTarget by remember { mutableStateOf<SessionTimeField?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = TaskColor)
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                Text(title)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = it }) {
                    OutlinedTextField(
                        value = selectedMode?.let { localizedFocusModeName(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.dialogTimeMachineMode)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false }) {
                        modes.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(localizedFocusModeName(mode)) },
                                onClick = { selectedModeId = mode.id; modeExpanded = false },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                if (fixedTargetLabel != null) {
                    OutlinedTextField(
                        value = fixedTargetLabel,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(stringResource(R.string.focusSessionEditorTarget)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    val noTagLabel = stringResource(R.string.dialogTimeMachineNoTag)
                    ExposedDropdownMenuBox(
                        expanded = tagExpanded,
                        onExpandedChange = { tagExpanded = it }) {
                        OutlinedTextField(
                            value = tags.firstOrNull { it.id == selectedTagId }?.name ?: noTagLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.dialogTimeMachineTag)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = tagExpanded,
                            onDismissRequest = { tagExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(noTagLabel) },
                                onClick = { selectedTagId = null; tagExpanded = false })
                            tags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag.name) },
                                    onClick = { selectedTagId = tag.id; tagExpanded = false })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                TimeRow(
                    label = stringResource(R.string.dialogTimeMachineStartTime),
                    value = dateTimeFormatter.format(startDateTime),
                    onClick = { pickerTarget = SessionTimeField.START },
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                TimeRow(
                    label = stringResource(R.string.dialogTimeMachineEndTime),
                    value = dateTimeFormatter.format(endDateTime),
                    onClick = { pickerTarget = SessionTimeField.END },
                )

                errorText?.let {
                    Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                    Text(it, color = ErrorColor, fontSize = AppTheme.fsBodySmall)
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = { showDeleteConfirmation = true }) {
                        Text(stringResource(R.string.commonLabelDelete), color = ErrorColor)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val mode = selectedMode ?: return@Button
                if (!endDateTime.isAfter(startDateTime)) {
                    errorText = endBeforeStartError
                    return@Button
                }
                onSave(mode, startDateTime, endDateTime, selectedTagId)
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
    )

    pickerTarget?.let { target ->
        val referenceInstant = if (target == SessionTimeField.START) startDateTime else endDateTime
        val referenceTime = referenceInstant.atZone(zone)
        NeoDateTimePickerDialog(
            initialDateMillis = referenceInstant.toEpochMilli(),
            initialHour = referenceTime.hour,
            initialMinute = referenceTime.minute,
            onConfirm = { date, hour, minute ->
                val instant = date.atTime(hour, minute).atZone(zone).toInstant()
                if (target == SessionTimeField.START) startDateTime = instant
                else endDateTime = instant
                errorText = null
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
    }

    ConfirmationDialog(
        visible = showDeleteConfirmation,
        title = stringResource(R.string.focusSessionDeleteTitle),
        content = stringResource(R.string.focusSessionDeleteContent),
        confirmLabel = stringResource(R.string.commonLabelDelete),
        confirmColor = ErrorColor,
        onConfirm = {
            showDeleteConfirmation = false
            onDelete?.invoke()
        },
        onDismiss = { showDeleteConfirmation = false },
    )
}

/**
 * Wires [SessionEditorDialog] up as the editor for an already logged [session]: saving retimes it,
 * deleting removes it, and both report back through the ambient snackbar. [onDismiss] is called
 * for every outcome, so the caller only has to clear the session it opened the editor with.
 */
@Composable
fun EditSessionDialog(
    session: FocusSessionEntity,
    focusViewModel: FocusViewModel,
    onDismiss: () -> Unit,
) {
    val modes by focusViewModel.allModes.collectAsState()
    val tags by focusViewModel.allTags.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val updatedMessage = stringResource(R.string.focusSessionUpdatedSnackbar)
    val deletedMessage = stringResource(R.string.focusSessionDeletedSnackbar)
    val emptyTargetLabel = stringResource(R.string.focusTargetEmpty)

    SessionEditorDialog(
        title = stringResource(R.string.focusSessionEditTitle),
        icon = Icons.Filled.Edit,
        modes = modes,
        tags = tags,
        initialModeId = session.modeId,
        initialSelectedTagId = session.tagId,
        initialStartTime = session.startTime,
        // A session left open-ended has no end to prefill, so the editor proposes "now" -
        // exactly the correction the user opened it for.
        initialEndTime = session.endTime,
        // Task- and habit-linked sessions keep their target; only their timing is editable.
        fixedTargetLabel = if (session.targetType == FocusTargetType.TAG) {
            null
        } else {
            session.targetLabel?.trim()?.takeIf { it.isNotEmpty() } ?: emptyTargetLabel
        },
        onDelete = {
            focusViewModel.deleteSession(session)
            onDismiss()
            scope.launch { snackbarHostState.showSnackbar(deletedMessage) }
        },
        onDismiss = onDismiss,
        onSave = { mode, start, end, tagId ->
            focusViewModel.updateSession(
                session,
                mode,
                start,
                end,
                tags.firstOrNull { it.id == tagId },
            )
            onDismiss()
            scope.launch { snackbarHostState.showSnackbar(updatedMessage) }
        },
    )
}

private enum class SessionTimeField { START, END }

@Composable
private fun TimeRow(label: String, value: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spaceMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    fontSize = AppTheme.fsLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(value, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Filled.EditCalendar, contentDescription = null)
        }
    }
}
