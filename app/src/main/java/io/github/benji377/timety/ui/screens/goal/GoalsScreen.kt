package io.github.benji377.timety.ui.screens.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.goal.GoalWithEntries
import io.github.benji377.timety.ui.components.common.detailFieldColors
import io.github.benji377.timety.ui.components.common.ExpansionSection
import io.github.benji377.timety.ui.components.common.TimetyDateTimePickerDialog
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.GoalColor
import io.github.benji377.timety.ui.theme.SuccessColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.GoalViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.goal.GoalUtils
import io.github.benji377.timety.util.habit.HabitIcons
import java.time.Instant
import java.time.ZoneId
import io.github.benji377.timety.ui.components.common.TimetyButton as Button
import io.github.benji377.timety.ui.components.common.TimetyOutlinedTextField as OutlinedTextField


/**
 * Lists the user's goals: active ones as cards with an expected-vs-actual pace indicator and a
 * quick log button, finished ones in a collapsed Completed section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGoalDetail: (String?) -> Unit,
    viewModel: GoalViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val goals by viewModel.goalsWithEntries.collectAsState()
    val (completed, active) = goals.partition { it.goal.completedAt != null }
    var entryDialogFor by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.goalsTitle),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = AppTheme.space3XLarge),
        ) {
            item {
                Button(
                    onClick = { onNavigateToGoalDetail(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.spaceLarge),
                    colors = ButtonDefaults.buttonColors(containerColor = GoalColor),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(AppTheme.spaceSmall))
                    Text(stringResource(R.string.goalsLabelAdd))
                }
            }

            if (goals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.space3XLarge),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.goalsEmpty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(active, key = { it.goal.id }) { goalWithEntries ->
                GoalCard(
                    goalWithEntries = goalWithEntries,
                    onTap = { onNavigateToGoalDetail(goalWithEntries.goal.id) },
                    onLogProgress = { entryDialogFor = goalWithEntries.goal.id },
                    modifier = Modifier.padding(
                        horizontal = AppTheme.spaceLarge,
                        vertical = AppTheme.spaceSmall,
                    ),
                )
            }

            if (completed.isNotEmpty()) {
                item {
                    ExpansionSection(
                        title = "${stringResource(R.string.goalsSectionCompleted)} (${completed.size})",
                        color = SuccessColor,
                        initiallyExpanded = false,
                    ) {
                        Column {
                            completed.forEach { goalWithEntries ->
                                GoalCard(
                                    goalWithEntries = goalWithEntries,
                                    onTap = { onNavigateToGoalDetail(goalWithEntries.goal.id) },
                                    onLogProgress = null,
                                    modifier = Modifier.padding(vertical = AppTheme.spaceSmall),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    entryDialogFor?.let { goalId ->
        val goal = goals.find { it.goal.id == goalId }?.goal
        if (goal != null) {
            GoalEntryDialog(
                unitLabel = goal.unitLabel,
                onConfirm = { value, timestamp ->
                    viewModel.addEntry(goalId, value, timestamp)
                    entryDialogFor = null
                },
                onDismiss = { entryDialogFor = null },
            )
        }
    }
}


/**
 * One goal as a card: icon swatch, name, `12 / 30 km` readout, a progress bar with a tick mark at
 * the pace-expected position, and the ahead/behind and days-left lines. Completed goals swap the
 * pace details for the completion date and drop the log button.
 */
@Composable
private fun GoalCard(
    goalWithEntries: GoalWithEntries,
    onTap: () -> Unit,
    onLogProgress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val goal = goalWithEntries.goal
    val goalColor = Color(goal.colorValue)
    val progress = goalWithEntries.progress
    val isCompleted = goal.completedAt != null
    val dateFmt = LocalDateFormatSettings.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = AppTheme.brNeo,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spaceLarge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(goalColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = HabitIcons.iconAt(goal.iconCodePoint),
                    contentDescription = null,
                    tint = goalColor,
                )
            }
            Spacer(Modifier.width(AppTheme.spaceLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.goalProgressLabel,
                        progress, goal.targetValue, goal.unitLabel,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(AppTheme.spaceSmall))
                PacedProgressBar(
                    progressFraction = if (goal.targetValue > 0) {
                        progress.toFloat() / goal.targetValue
                    } else 1f,
                    // The tick is pace guidance for active goals only.
                    expectedFraction = if (isCompleted) null else GoalUtils.expectedFraction(goal),
                    color = if (isCompleted) SuccessColor else goalColor,
                )
                Spacer(Modifier.height(AppTheme.spaceSmall))
                val completedAt = goal.completedAt
                if (completedAt != null) {
                    Text(
                        text = stringResource(
                            R.string.goalCompletedOn,
                            AppDateFormatUtils.formatDate(completedAt, dateFmt.dateFormatCode),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessColor,
                    )
                } else {
                    PaceSummaryRow(goalWithEntries)
                }
            }
            if (onLogProgress != null) {
                Spacer(Modifier.width(AppTheme.spaceMedium))
                FilledTonalIconButton(
                    onClick = onLogProgress,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = goalColor.copy(alpha = 0.15f),
                        contentColor = goalColor,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.goalsLabelLogProgress),
                    )
                }
            }
        }
    }
}


/** The "ahead/behind by N unit" and "N days left / overdue" line under an active goal's bar. */
@Composable
private fun PaceSummaryRow(goalWithEntries: GoalWithEntries) {
    val goal = goalWithEntries.goal
    val delta = goalWithEntries.progress - GoalUtils.expectedProgress(goal)
    val daysLeft = GoalUtils.daysLeft(goal).toInt()

    val paceText = when {
        delta > 0 -> stringResource(R.string.goalPaceAhead, delta, goal.unitLabel)
        delta < 0 -> stringResource(R.string.goalPaceBehind, -delta, goal.unitLabel)
        else -> stringResource(R.string.goalPaceOnTrack)
    }
    val paceColor = when {
        delta < 0 -> MaterialTheme.colorScheme.error
        else -> SuccessColor
    }
    val deadlineText = when {
        daysLeft > 0 -> pluralStringResource(R.plurals.goalDaysLeft, daysLeft, daysLeft)
        daysLeft == 0 -> stringResource(R.string.goalDueToday)
        else -> pluralStringResource(R.plurals.goalDaysOverdue, -daysLeft, -daysLeft)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = paceText,
            style = MaterialTheme.typography.bodySmall,
            color = paceColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = deadlineText,
            style = MaterialTheme.typography.bodySmall,
            color = if (daysLeft < 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}


/** Progress bar with an optional vertical tick mark at the pace-expected fraction. */
@Composable
private fun PacedProgressBar(
    progressFraction: Float,
    expectedFraction: Float?,
    color: Color,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { progressFraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = color,
            // A tinted track: the theme's surfaceVariant is nearly the card color, which would
            // leave the unfilled part of the bar invisible.
            trackColor = color.copy(alpha = 0.2f),
            drawStopIndicator = {},
        )
        if (expectedFraction != null && expectedFraction > 0f) {
            // Right-aligned inside a box that spans exactly the expected fraction of the width,
            // which lands the tick at that fraction without measuring pixel positions.
            Box(
                modifier = Modifier
                    .fillMaxWidth(expectedFraction.coerceIn(0f, 1f))
                    .height(10.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
    }
}


/**
 * Dialog for logging a progress increment: an integer amount (default 1) and an editable
 * timestamp defaulting to now, so yesterday's run can still be logged honestly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEntryDialog(
    unitLabel: String,
    onConfirm: (value: Int, timestamp: Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    var valueText by remember { mutableStateOf("1") }
    var timestamp by remember { mutableStateOf(Instant.now()) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    val dateFmt = LocalDateFormatSettings.current
    val value = valueText.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goalEntryDialogTitle)) },
        text = {
            Column {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) valueText = input
                    },
                    label = { Text("${stringResource(R.string.goalEntryDialogLabelValue)} ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(AppTheme.spaceLarge))
                Box(modifier = Modifier.clickable { showDateTimePicker = true }) {
                    OutlinedTextField(
                        value = AppDateFormatUtils.formatDateTime(
                            timestamp,
                            dateFmt.dateFormatCode,
                            dateFmt.use24HourFormat,
                        ),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(stringResource(R.string.goalEntryDialogLabelDate)) },
                        trailingIcon = { Icon(Icons.Filled.Edit, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = detailFieldColors(isEditing = true),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = value >= 1,
                onClick = { onConfirm(value, timestamp) },
            ) { Text(stringResource(R.string.commonLabelConfirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commonLabelCancel)) }
        },
    )

    if (showDateTimePicker) {
        val zoned = timestamp.atZone(ZoneId.systemDefault())
        TimetyDateTimePickerDialog(
            initialDateMillis = timestamp.toEpochMilli(),
            initialHour = zoned.hour,
            initialMinute = zoned.minute,
            onConfirm = { date, hour, minute ->
                timestamp = date.atTime(hour, minute)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                showDateTimePicker = false
            },
            onDismiss = { showDateTimePicker = false },
        )
    }
}
