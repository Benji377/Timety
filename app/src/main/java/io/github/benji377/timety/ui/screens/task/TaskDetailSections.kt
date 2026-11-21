package io.github.benji377.timety.ui.screens.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.task.Priority
import io.github.benji377.timety.data.model.task.TaskPlace
import io.github.benji377.timety.data.model.task.TaskSize
import io.github.benji377.timety.ui.components.common.FieldLabel
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField
import io.github.benji377.timety.ui.components.common.StyledExpansionTile
import io.github.benji377.timety.ui.components.common.detailFieldColors
import io.github.benji377.timety.ui.components.task.ReminderEntry
import io.github.benji377.timety.ui.components.task.ReminderField
import io.github.benji377.timety.ui.components.task.reminderPresetLabel
import io.github.benji377.timety.ui.screens.LocationPickerScreen
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.InfoColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.theme.forText
import io.github.benji377.timety.ui.utils.AppUtils
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.location.LocationApi
import io.github.benji377.timety.util.task.Due
import io.github.benji377.timety.util.task.ReminderPreset
import io.github.benji377.timety.util.task.ReminderUtils
import java.time.Instant

/**
 * Segmented selector where the selected segment expands to show its label; the rest stay
 * icon-only. The outer track's [AppTheme.borderCard] frame, and the selected segment's own solid
 * fill plus hairline border, stay at full contrast in both edit and view mode - read-only is not a
 * reason to soften a border; only the track's background and tap-gating change with [isEditing].
 */
@Composable
internal fun <T> AccordionSelector(
    values: List<T>,
    selectedValue: T,
    isEditing: Boolean,
    iconBuilder: @Composable (T, Boolean) -> Unit,
    labelBuilder: (T) -> String,
    onSelected: (T) -> Unit,
    activeBgColor: Color = TaskColor,
    activeTextColor: Color = Color.White,
) {
    val borderColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.segmentedControlHeight)
            .clip(AppTheme.brPill)
            // The editable/read-only distinction is carried entirely by this fill: an editable
            // control sits on the brighter `surface`, a read-only one recedes to `surfaceVariant`.
            .background(
                if (isEditing) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(AppTheme.borderCard, borderColor, AppTheme.brPill)
    ) {
        values.forEachIndexed { index, value ->
            val isSelected = value == selectedValue
            val weight by animateFloatAsState(
                targetValue = if (isSelected) 3f else 1f,
                label = "accordionSegment"
            )
            val isLast = index == values.lastIndex
            // A divider next to the selected segment would double up with that segment's own
            // border, so only draw it between two unselected neighbors.
            val nextIsSelected = !isLast && values[index + 1] == selectedValue
            val drawsDivider = !isLast && !isSelected && !nextIsSelected
            Row(
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .let { m ->
                        if (drawsDivider) {
                            m.drawBehind {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(size.width, 0f),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = AppTheme.borderHairline.toPx()
                                )
                            }
                        } else m
                    }
                    .selectable(selected = isSelected, enabled = isEditing, role = Role.RadioButton) {
                        onSelected(value)
                    }
                    // Unselected segments show only an icon, so they need the label spoken.
                    .then(
                        if (isSelected) Modifier
                        else Modifier.semantics { contentDescription = labelBuilder(value) }
                    )
                    .padding(AppTheme.segmentedSelectorInset)
                    .then(
                        if (isSelected) {
                            Modifier
                                .clip(AppTheme.brMedium)
                                .background(activeBgColor, AppTheme.brMedium)
                                .border(AppTheme.borderHairline, borderColor, AppTheme.brMedium)
                        } else Modifier
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconBuilder(value, isSelected)
                AnimatedVisibility(visible = isSelected) {
                    Text(
                        text = labelBuilder(value),
                        modifier = Modifier.padding(start = AppTheme.spaceSmall),
                        fontWeight = FontWeight.Bold,
                        color = activeTextColor,
                        fontSize = AppTheme.fsSegmentLabel,
                        letterSpacing = AppTheme.lsNarrow
                    )
                }
            }
        }
    }
}

/** Expandable details of the place picked for the task, read from the stored snapshot. */
@Composable
internal fun PlaceDetailsSection(place: TaskPlace) {
    StyledExpansionTile(
        title = stringResource(R.string.taskDetailsLocationDetails),
        titleColor = TaskColor,
    ) {
        Column(modifier = Modifier.padding(horizontal = AppTheme.spaceLarge)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = TaskColor,
                    modifier = Modifier.size(AppTheme.iconSizeMedium)
                )
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                Text(place.name, fontWeight = FontWeight.Bold)
            }
            if (place.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AppTheme.spaceXSmall))
                Text(
                    place.details,
                    fontSize = AppTheme.fsBodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (place.country.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AppTheme.spaceXSmall))
                Text(
                    place.country,
                    fontSize = AppTheme.fsBodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val latitude = place.latitude
            val longitude = place.longitude
            if (latitude != null && longitude != null) {
                Spacer(modifier = Modifier.height(AppTheme.spaceXSmall))
                Text(
                    String.format(java.util.Locale.ROOT, "%.5f, %.5f", latitude, longitude),
                    fontSize = AppTheme.fsBodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Priority and effort size, each a row of selectable segments. */
@Composable
internal fun PriorityAndEffortFields(
    priority: Priority,
    size: TaskSize,
    isEditing: Boolean,
    onPriorityChange: (Priority) -> Unit,
    onSizeChange: (TaskSize) -> Unit,
) {
    val priorityLabels = Priority.entries.associateWith { stringResource(AppUtils.priorityLabel(it)) }
    val sizeLabels = TaskSize.entries.associateWith { stringResource(AppUtils.sizeLabel(it)) }
    Column {
        FieldLabel(stringResource(R.string.taskDetailsLabelPriority))
        AccordionSelector(
            values = Priority.entries,
            selectedValue = priority,
            isEditing = isEditing,
            onSelected = onPriorityChange,
            iconBuilder = { p, isSelected ->
                // Selection shows in the segment's fill and border, so the icon only changes
                // where it would clash with a selected segment's fill.
                AppUtils.PriorityIcon(priority = p, tint = if (isSelected) Color.White else null)
            },
            labelBuilder = { p -> priorityLabels.getValue(p) }
        )

        Spacer(Modifier.height(AppTheme.spaceLarge))
        FieldLabel(stringResource(R.string.taskDetailsLabelEffort))
        AccordionSelector(
            values = TaskSize.entries,
            selectedValue = size,
            isEditing = isEditing,
            onSelected = onSizeChange,
            iconBuilder = { s, isSelected ->
                Text(
                    text = AppUtils.getSizeEmoji(s),
                    fontSize = AppTheme.fsBodyLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            labelBuilder = { s -> sizeLabels.getValue(s) }
        )
    }
}

/**
 * The due date and its optional time as two read-only fields that open their pickers. The time
 * field appears once a date is set; without a time the task is due all day. A null [onClearDate]
 * makes the date required.
 */
@Composable
internal fun DueDateTimeFields(
    due: Due?,
    isEditing: Boolean,
    dateLabel: String,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    onClearTime: () -> Unit,
    onClearDate: (() -> Unit)?,
    dateError: String? = null,
) {
    val dateFmt = LocalDateFormatSettings.current
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceMedium)) {
        PickerField(
            value = due?.let { AppDateFormatUtils.formatDate(it.at, dateFmt.dateFormatCode) } ?: "",
            label = dateLabel,
            icon = Icons.Filled.Event,
            isEditing = isEditing,
            onPick = onPickDate,
            onClear = onClearDate?.takeIf { due != null },
            errorText = dateError,
            modifier = Modifier.weight(1f),
        )
        if (due != null) {
            PickerField(
                value = if (due.hasTime) {
                    AppDateFormatUtils.formatTime(due.at, dateFmt.use24HourFormat)
                } else {
                    stringResource(R.string.taskDetailsLabelAllDay)
                },
                label = stringResource(R.string.taskDetailsLabelDueTime),
                icon = Icons.Filled.Schedule,
                isEditing = isEditing,
                onPick = onPickTime,
                onClear = onClearTime.takeIf { due.hasTime },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** A read-only field that opens a picker, with a clear button when [onClear] is set. */
@Composable
private fun PickerField(
    value: String,
    label: String,
    icon: ImageVector,
    isEditing: Boolean,
    onPick: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    Box(
        modifier = modifier
            .clickable(enabled = isEditing, role = Role.Button, onClick = onPick)
            .semantics { contentDescription = label }
    ) {
        NeoOutlinedTextField(
            value = value,
            onValueChange = {},
            enabled = false,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, maxLines = 1) },
            errorText = errorText,
            leadingIcon = { Icon(icon, null) },
            trailingIcon = {
                when {
                    !isEditing -> Unit
                    onClear != null -> IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.commonClear))
                    }
                    else -> Icon(Icons.Filled.Edit, null)
                }
            },
            // A picker field is never typed into, so it is disabled and needs the error look spelled out.
            colors = if (errorText == null) detailFieldColors(isEditing) else detailFieldColors(isEditing).copy(
                disabledIndicatorColor = MaterialTheme.colorScheme.error,
                disabledLabelColor = MaterialTheme.colorScheme.error,
            ),
        )
    }
}

/** The task's reminders: presets relative to [due] plus custom times. */
@Composable
internal fun TaskReminderField(
    reminders: List<Instant>,
    due: Due?,
    isEditing: Boolean,
    onRemindersChange: (List<Instant>) -> Unit,
    onAddCustom: () -> Unit,
) {
    val dateFmt = LocalDateFormatSettings.current
    val now = Instant.now()
    val formatReminder = { time: Instant ->
        "${AppDateFormatUtils.formatDate(time, dateFmt.dateFormatCode)} - " +
                AppDateFormatUtils.formatTime(time, dateFmt.use24HourFormat)
    }
    val hasTime = due?.hasTime ?: true
    val entries = reminders.sorted().map { reminder ->
        val preset = due?.let { ReminderUtils.presetOf(it, reminder) }
        ReminderEntry(
            key = reminder.toEpochMilli(),
            preset = preset,
            label = if (preset != null) reminderPresetLabel(preset, hasTime) else formatReminder(reminder),
            time = if (preset != null) formatReminder(reminder) else null,
        )
    }
    val warning = when {
        due == null -> stringResource(R.string.reminderWarnNoDue)
        reminders.size >= ReminderUtils.MAX_REMINDERS ->
            stringResource(R.string.reminderWarnLimit, ReminderUtils.MAX_REMINDERS)
        else -> null
    }
    ReminderField(
        entries = entries,
        isEditing = isEditing,
        sheetSubtitle = when {
            due == null -> stringResource(R.string.reminderSheetNoDue)
            due.hasTime -> stringResource(R.string.reminderSheetDue, formatReminder(due.at))
            else -> stringResource(
                R.string.reminderSheetDueAllDay,
                AppDateFormatUtils.formatDate(due.at, dateFmt.dateFormatCode),
                AppDateFormatUtils.formatTime(due.reminderAnchor(), dateFmt.use24HourFormat),
            )
        },
        warning = warning,
        presets = due?.presets ?: ReminderPreset.entries,
        hasTime = hasTime,
        canAddPreset = { preset ->
            due != null && ReminderUtils.isPresetAvailable(reminders, due, preset, now)
        },
        onTogglePreset = { preset ->
            due?.let { onRemindersChange(ReminderUtils.toggle(reminders, it, preset)) }
        },
        onRemoveCustom = { entry -> onRemindersChange(reminders.filterNot { it.toEpochMilli() == entry.key }) },
        onAddCustom = onAddCustom,
        customEnabled = reminders.size < ReminderUtils.MAX_REMINDERS,
    )
}

/**
 * The location as free text with a search for a place. [onTextChange] fires for typed text,
 * [onPlacePicked] for a place chosen in the search.
 */
@Composable
internal fun LocationField(
    location: String,
    place: TaskPlace?,
    isEditing: Boolean,
    onTextChange: (String) -> Unit,
    onPlacePicked: (TaskPlace) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column {
        NeoOutlinedTextField(
            value = location,
            onValueChange = onTextChange,
            enabled = isEditing,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.taskDetailsLabelLocation)) },
            leadingIcon = { Icon(Icons.Outlined.Map, null) },
            // A bare IconButton: inside the field's own border, a second one would nest.
            trailingIcon = {
                if (isEditing) {
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.locationPickerTitle))
                    }
                }
            }
        )

        // Only the place search needs the network, so being offline is a small inline note.
        if (isEditing && !LocationApi.isOnline(LocalContext.current)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = AppTheme.spaceSmall)
            ) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = WarningColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(AppTheme.spaceSmall))
                Text(stringResource(R.string.taskDetailsLocationOffline), fontSize = AppTheme.fsBodySmall, color = WarningColor.forText())
            }
        }

        if (!isEditing && place != null) {
            PlaceDetailsSection(place = place)
        }
    }

    if (showPicker) {
        Dialog(
            onDismissRequest = { showPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LocationPickerScreen(
                initialQuery = location,
                onLocationSelected = {
                    onPlacePicked(it)
                    showPicker = false
                },
                onBack = { showPicker = false }
            )
        }
    }
}

/** Checklist row; [showDragHandle] hints that the row can be held and dragged to reorder. */
@Composable
internal fun SubtaskRow(
    title: String,
    isCompleted: Boolean,
    isEditing: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
    showDragHandle: Boolean = false,
    isDragged: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.brMedium)
            .background(
                if (isDragged) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
            )
    ) {
        if (showDragHandle) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Checkbox(
            checked = isCompleted,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = SuccessColor,
                uncheckedColor = TaskColor,
                checkmarkColor = Color.White
            )
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
            color = if (isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        if (isEditing) {
            NeoIconButton(
                onClick = onRemove,
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.commonLabelRemove),
                contentColor = ErrorColor,
                size = AppTheme.iconSizeXLarge,
            )
        }
    }
}

/** Text field for a new checklist item; [onAdd] gets the trimmed title, never a blank one. */
@Composable
internal fun NewSubtaskField(onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    fun submit() {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            onAdd(trimmed)
            text = ""
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.spaceSmall)
    ) {
        Icon(
            Icons.Filled.SubdirectoryArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(AppTheme.spaceMedium))
        NeoOutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.taskDetailsLabelSubtaskHint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            // A bare IconButton, like the location search: it sits inside the field's border.
            trailingIcon = {
                IconButton(onClick = { submit() }) {
                    Icon(Icons.Filled.AddCircle, contentDescription = stringResource(R.string.commonLabelAdd), tint = InfoColor)
                }
            }
        )
    }
}
