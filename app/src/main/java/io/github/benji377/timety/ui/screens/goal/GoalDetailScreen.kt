package io.github.benji377.timety.ui.screens.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.goal.GoalEntity
import io.github.benji377.timety.data.model.goal.GoalEntryEntity
import io.github.benji377.timety.ui.components.common.ColorPickerDialog
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.components.common.detailFieldColors
import io.github.benji377.timety.ui.components.common.DetailTopBarActions
import io.github.benji377.timety.ui.components.common.IconPickerDialog
import io.github.benji377.timety.ui.components.common.PickerField
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.GoalColor
import io.github.benji377.timety.ui.theme.PickerPalette
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.GoalViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitIcons
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


/**
 * Creates, views, or edits a goal. With a null [goalId] the screen starts in creation mode;
 * otherwise it opens in read-only view mode until the user taps edit. Existing goals also show
 * the full progress log with per-entry delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String? = null,
    onNavigateBack: () -> Unit,
    goalViewModel: GoalViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val goalsWithEntries by goalViewModel.goalsWithEntries.collectAsState()
    val existingGoalWithEntries = goalId?.let { id -> goalsWithEntries.find { it.goal.id == id } }
    val existingGoal = existingGoalWithEntries?.goal
    val isNewGoal = goalId == null

    var isEditing by remember { mutableStateOf(isNewGoal) }

    // Form state.
    var name by remember(existingGoal?.id) { mutableStateOf(existingGoal?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var description by remember(existingGoal?.id) {
        mutableStateOf(
            existingGoal?.description ?: ""
        )
    }
    var targetValueText by remember(existingGoal?.id) {
        mutableStateOf(existingGoal?.targetValue?.toString() ?: "")
    }
    var targetError by remember { mutableStateOf(false) }
    var unitLabel by remember(existingGoal?.id) { mutableStateOf(existingGoal?.unitLabel ?: "") }
    var unitError by remember { mutableStateOf(false) }
    var targetDate by remember(existingGoal?.id) {
        mutableStateOf(
            existingGoal?.targetDate
                ?: LocalDate.now().plusDays(30).atTime(LocalTime.of(23, 59))
                    .atZone(ZoneId.systemDefault()).toInstant()
        )
    }
    var selectedIconIndex by remember(existingGoal?.id) { mutableStateOf(existingGoal?.iconCodePoint) }
    var selectedColor by remember(existingGoal?.id) {
        mutableStateOf(existingGoal?.colorValue?.let { Color(it) } ?: GoalColor)
    }

    var showIconPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingEntryDelete by remember { mutableStateOf<GoalEntryEntity?>(null) }

    val dateFmt = LocalDateFormatSettings.current

    val appBarTitle = when {
        isNewGoal -> stringResource(R.string.goalDetailTitleNew)
        isEditing -> stringResource(R.string.goalDetailTitleEdit)
        else -> stringResource(R.string.goalDetailTitleView)
    }

    fun saveGoal() {
        val trimmedName = name.trim()
        val trimmedUnit = unitLabel.trim()
        val targetValue = targetValueText.toIntOrNull() ?: 0
        nameError = trimmedName.isEmpty()
        targetError = targetValue < 1
        unitError = trimmedUnit.isEmpty()
        if (nameError || targetError || unitError) return

        val goalToSave = GoalEntity(
            id = goalId ?: UUID.randomUUID().toString(),
            name = trimmedName,
            description = description.trim(),
            colorValue = selectedColor.toArgb(),
            iconCodePoint = selectedIconIndex,
            targetValue = targetValue,
            unitLabel = trimmedUnit,
            targetDate = targetDate,
            createdAt = existingGoal?.createdAt ?: Instant.now(),
            // Preserved on edit; updateGoal re-syncs it in case the target changed.
            completedAt = existingGoal?.completedAt,
        )

        if (isNewGoal) {
            goalViewModel.addGoal(goalToSave)
            onNavigateBack()
        } else {
            goalViewModel.updateGoal(goalToSave)
            isEditing = false
        }
    }

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = appBarTitle,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    DetailTopBarActions(
                        isViewing = !isEditing && !isNewGoal,
                        onDelete = { showDeleteDialog = true },
                        onEdit = { isEditing = true },
                        onSave = { saveGoal() },
                    )
                }
            )
        },
    ) { paddingValues ->
        ConfirmationDialog(
            visible = showDeleteDialog,
            title = stringResource(R.string.goalDeleteTitle),
            content = stringResource(R.string.goalDeleteContent),
            confirmLabel = stringResource(R.string.commonLabelRemove),
            confirmColor = ErrorColor,
            onConfirm = {
                showDeleteDialog = false
                existingGoal?.let { goalViewModel.deleteGoal(it) }
                onNavigateBack()
            },
            onDismiss = { showDeleteDialog = false },
        )

        ConfirmationDialog(
            visible = pendingEntryDelete != null,
            title = stringResource(R.string.goalEntryDeleteTitle),
            content = stringResource(R.string.goalEntryDeleteContent),
            confirmLabel = stringResource(R.string.commonLabelRemove),
            confirmColor = ErrorColor,
            onConfirm = {
                pendingEntryDelete?.let { goalViewModel.deleteEntry(it) }
                pendingEntryDelete = null
            },
            onDismiss = { pendingEntryDelete = null },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall)
        ) {
            // Goal name.
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; if (it.isNotBlank()) nameError = false },
                    enabled = isEditing,
                    label = { Text(stringResource(R.string.goalDetailLabelName) + " *") },
                    placeholder = { Text(stringResource(R.string.goalDetailLabelNameHint)) },
                    leadingIcon = { Icon(Icons.Filled.Flag, null, tint = selectedColor) },
                    isError = nameError,
                    supportingText = {
                        if (nameError) Text(
                            stringResource(R.string.goalDetailLabelNameRequest),
                            color = ErrorColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Target value and unit.
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = targetValueText,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() }) {
                                targetValueText = input
                                if ((input.toIntOrNull() ?: 0) >= 1) targetError = false
                            }
                        },
                        enabled = isEditing,
                        label = { Text(stringResource(R.string.goalDetailLabelTarget) + " *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = targetError,
                        supportingText = {
                            if (targetError) Text(
                                stringResource(R.string.goalDetailLabelTargetRequest),
                                color = ErrorColor
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
                    OutlinedTextField(
                        value = unitLabel,
                        onValueChange = { unitLabel = it; if (it.isNotBlank()) unitError = false },
                        enabled = isEditing,
                        label = { Text(stringResource(R.string.goalDetailLabelUnit) + " *") },
                        placeholder = { Text(stringResource(R.string.goalDetailLabelUnitHint)) },
                        singleLine = true,
                        isError = unitError,
                        supportingText = {
                            if (unitError) Text(
                                stringResource(R.string.goalDetailLabelUnitRequest),
                                color = ErrorColor
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Deadline.
            item {
                Box(modifier = Modifier.clickable(enabled = isEditing) { showDatePicker = true }) {
                    OutlinedTextField(
                        value = AppDateFormatUtils.formatDate(targetDate, dateFmt.dateFormatCode),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(stringResource(R.string.goalDetailLabelDeadline)) },
                        leadingIcon = { Icon(Icons.Filled.Event, null) },
                        trailingIcon = { if (isEditing) Icon(Icons.Filled.Edit, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = detailFieldColors(isEditing),
                    )
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Appearance: icon and color.
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    PickerField(
                        label = stringResource(R.string.goalDetailLabelIcon),
                        enabled = isEditing,
                        onClick = { showIconPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = HabitIcons.iconAt(selectedIconIndex),
                            contentDescription = null,
                            tint = selectedColor,
                        )
                    }
                    Spacer(modifier = Modifier.width(AppTheme.spaceLarge))
                    PickerField(
                        label = stringResource(R.string.goalDetailLabelColor),
                        enabled = isEditing,
                        onClick = { showColorPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = selectedColor,
                                    shape = CircleShape,
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Description.
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    enabled = isEditing,
                    label = { Text(stringResource(R.string.goalDetailLabelDescription)) },
                    placeholder = { Text(stringResource(R.string.goalDetailLabelDescriptionHint)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            }

            // Progress log, newest first (existing goals only).
            if (existingGoalWithEntries != null) {
                item {
                    Text(
                        stringResource(R.string.goalDetailEntriesTitle),
                        fontWeight = AppTheme.fwBold
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                }
                val sortedEntries =
                    existingGoalWithEntries.entries.sortedByDescending { it.timestamp }
                if (sortedEntries.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.goalDetailEntriesEmpty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(AppTheme.space3XLarge))
                    }
                } else {
                    items(sortedEntries.size, key = { sortedEntries[it].id }) { index ->
                        EntryRow(
                            entry = sortedEntries[index],
                            unitLabel = existingGoalWithEntries.goal.unitLabel,
                            onDelete = { pendingEntryDelete = sortedEntries[index] },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(AppTheme.space3XLarge)) }
                }
            }
        }

        if (showIconPicker) {
            IconPickerDialog(
                title = stringResource(R.string.goalDetailLabelIcon),
                selectedIconIndex = selectedIconIndex,
                accentColor = selectedColor,
                onSelect = {
                    selectedIconIndex = it
                    showIconPicker = false
                },
                onDismiss = { showIconPicker = false },
            )
        }

        if (showColorPicker) {
            ColorPickerDialog(
                title = stringResource(R.string.goalDetailLabelColorPicker),
                colors = GOAL_DETAIL_COLORS,
                selectedColor = selectedColor,
                onSelect = {
                    selectedColor = it
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false },
            )
        }

        // Deadline picker dialog. The deadline is a whole day, stored as its end so pacing and
        // days-left treat the chosen date as still "in time".
        if (showDatePicker) {
            val state =
                rememberDatePickerState(initialSelectedDateMillis = targetDate.toEpochMilli())
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { millis ->
                            targetDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC).toLocalDate()
                                .atTime(LocalTime.of(23, 59))
                                .atZone(ZoneId.systemDefault()).toInstant()
                        }
                        showDatePicker = false
                    }) { Text(stringResource(R.string.commonLabelConfirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.commonLabelCancel))
                    }
                },
            ) {
                DatePicker(state = state)
            }
        }
    }
}


private val GOAL_DETAIL_COLORS = listOf(GoalColor) + PickerPalette


/** One logged increment: `+5 km`, its timestamp, and a delete affordance. */
@Composable
private fun EntryRow(
    entry: GoalEntryEntity,
    unitLabel: String,
    onDelete: () -> Unit,
) {
    val dateFmt = LocalDateFormatSettings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AppTheme.iconSizeSmall),
        )
        Spacer(Modifier.width(AppTheme.spaceMedium))
        Text(
            text = "${entry.value} $unitLabel",
            fontWeight = AppTheme.fwBold,
        )
        Spacer(Modifier.width(AppTheme.spaceMedium))
        Text(
            text = AppDateFormatUtils.formatDateTime(
                entry.timestamp,
                dateFmt.dateFormatCode,
                dateFmt.use24HourFormat,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.commonLabelRemove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
