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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import io.github.benji377.timety.data.model.focus.FocusTargetSelection
import io.github.benji377.timety.data.model.focus.icon
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.NeoAlertDialog
import io.github.benji377.timety.ui.components.common.NeoDateTimePickerDialog
import io.github.benji377.timety.ui.components.common.NeoDurationPickerDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import io.github.benji377.timety.ui.components.common.NeoButton as Button
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField as OutlinedTextField

/** Default length a backfilled session is proposed with, matching one classic pomodoro. */
private const val DEFAULT_BACKFILL_MINUTES = 25

/** A logged session is bounded to a single day's worth of focus: at least a minute, at most 24h. */
private const val MIN_SESSION_MINUTES = 1
private const val MAX_SESSION_MINUTES = 24 * 60

/**
 * Dialog for logging or editing a focus session: pick a mode, a target, a start date and time, and
 * a duration, then call [onSave] with the end derived as start + duration. The duration is capped
 * at 24 hours, which is why there is no end-time field - an editable end let a mistyped date log a
 * multi-day session that broke the statistics page. A session may still span midnight (e.g. 10pm
 * to 2am); it just cannot run longer than a day.
 *
 * The same dialog covers the three surfaces that need it. Passing [fixedTargetLabel] replaces the
 * target picker with a read-only row, for a session logged straight against a just-completed
 * task; passing [onDelete] adds a Delete action, for sessions that are already logged.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditorDialog(
    title: String,
    modes: List<FocusModeEntity>,
    onDismiss: () -> Unit,
    onSave: (
        mode: FocusModeEntity,
        startTime: Instant,
        endTime: Instant,
        target: FocusTargetSelection?,
    ) -> Unit,
    icon: ImageVector = Icons.Filled.History,
    initialModeId: String? = null,
    initialTarget: FocusTargetSelection? = null,
    initialStartTime: Instant? = null,
    initialEndTime: Instant? = null,
    fixedTargetLabel: String? = null,
    onDelete: (() -> Unit)? = null,
) {
    // The pick is held as an id, not an entity: the modes list is a live Room flow, so a
    // re-emission while the dialog is open must not reset (or outdate) the user's selection.
    var selectedModeId by remember(initialModeId) { mutableStateOf(initialModeId) }
    val selectedMode = modes.firstOrNull { it.id == selectedModeId } ?: modes.firstOrNull()
    var selectedTarget by remember { mutableStateOf(initialTarget) }
    // Proposed times are truncated to the minute: the pickers only offer whole minutes, so a
    // proposal carrying seconds would make an untouched field and an edited one disagree by up
    // to a minute in the logged duration.
    val now = remember { Instant.now().truncatedTo(ChronoUnit.MINUTES) }
    var startDateTime by remember {
        mutableStateOf(initialStartTime ?: now.minusSeconds(DEFAULT_BACKFILL_MINUTES * 60L))
    }
    // The length is picked as a duration, not an end instant, and clamped to [1, 24h]. When
    // editing, derive it from the stored range; an open-ended session (no end) falls back to the
    // span up to now, which is the correction the editor was opened for.
    var durationMinutes by remember {
        mutableStateOf(
            if (initialStartTime != null) {
                Duration.between(initialStartTime, initialEndTime ?: now).toMinutes()
                    .toInt().coerceIn(MIN_SESSION_MINUTES, MAX_SESSION_MINUTES)
            } else {
                DEFAULT_BACKFILL_MINUTES
            }
        )
    }
    val endDateTime = startDateTime.plus(durationMinutes.toLong(), ChronoUnit.MINUTES)

    var modeExpanded by remember { mutableStateOf(false) }
    var showTargetSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val zone = ZoneId.systemDefault()
    val locale = LocalLocale.current.platformLocale
    val dateTimeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale).withZone(zone)
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }

    NeoAlertDialog(
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
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                    TargetRow(target = selectedTarget, onClick = { showTargetSheet = true })
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

                TimeRow(
                    label = stringResource(R.string.dialogTimeMachineStartTime),
                    value = dateTimeFormatter.format(startDateTime),
                    icon = Icons.Filled.EditCalendar,
                    onClick = { showStartPicker = true },
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                TimeRow(
                    label = stringResource(R.string.dialogTimeMachineDuration),
                    value = AppDateFormatUtils.formatMinutesCompact(durationMinutes),
                    icon = Icons.Filled.Schedule,
                    onClick = { showDurationPicker = true },
                )

                // The derived end is shown so a session that crosses midnight is unambiguous.
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                Text(
                    stringResource(
                        R.string.dialogTimeMachineEnds,
                        dateTimeFormatter.format(endDateTime),
                    ),
                    fontSize = AppTheme.fsLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                onSave(mode, startDateTime, endDateTime, selectedTarget)
            }) { Text(stringResource(R.string.commonLabelSave)) }
        },
    )

    if (showTargetSheet) {
        TargetSelectorBottomSheet(
            onDismissRequest = { showTargetSheet = false },
            selected = selectedTarget,
            onTargetSelected = { selectedTarget = it },
        )
    }

    if (showStartPicker) {
        val startTime = startDateTime.atZone(zone)
        NeoDateTimePickerDialog(
            initialDateMillis = startDateTime.toEpochMilli(),
            initialHour = startTime.hour,
            initialMinute = startTime.minute,
            onConfirm = { date, hour, minute ->
                startDateTime = date.atTime(hour, minute).atZone(zone).toInstant()
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false },
        )
    }

    if (showDurationPicker) {
        NeoDurationPickerDialog(
            initialMinutes = durationMinutes,
            minMinutes = MIN_SESSION_MINUTES,
            maxMinutes = MAX_SESSION_MINUTES,
            title = { Text(stringResource(R.string.dialogTimeMachineDuration)) },
            onConfirm = { durationMinutes = it; showDurationPicker = false },
            onDismiss = { showDurationPicker = false },
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

    SessionEditorDialog(
        title = stringResource(R.string.focusSessionEditTitle),
        icon = Icons.Filled.Edit,
        modes = modes,
        initialModeId = session.modeId,
        initialTarget = remember(session) { FocusTargetSelection.fromSession(session, tags) },
        initialStartTime = session.startTime,
        // A session left open-ended has no end to prefill; the editor then proposes a duration
        // running up to now - exactly the correction the user opened it for.
        initialEndTime = session.endTime,
        onDelete = {
            focusViewModel.deleteSession(session)
            onDismiss()
            scope.launch { snackbarHostState.showSnackbar(deletedMessage) }
        },
        onDismiss = onDismiss,
        onSave = { mode, start, end, target ->
            focusViewModel.updateSession(session, mode, start, end, target)
            onDismiss()
            scope.launch { snackbarHostState.showSnackbar(updatedMessage) }
        },
    )
}

/** Tappable row showing the session's target; opens the target picker. */
@Composable
private fun TargetRow(target: FocusTargetSelection?, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spaceMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (target != null) {
                Icon(target.type.icon, contentDescription = null, tint = target.color)
                Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.focusSessionEditorTarget),
                    fontSize = AppTheme.fsLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    target?.label?.trim()?.takeIf { it.isNotEmpty() }
                        ?: stringResource(R.string.focusTargetEmpty),
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
    }
}

@Composable
private fun TimeRow(label: String, value: String, icon: ImageVector, onClick: () -> Unit) {
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
            Icon(icon, contentDescription = null)
        }
    }
}
