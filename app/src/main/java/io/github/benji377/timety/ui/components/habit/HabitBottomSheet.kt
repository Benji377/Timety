package io.github.benji377.timety.ui.components.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitCompletionEntity
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.ui.components.common.ConfirmationDialog
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.SuccessColor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Unified calendar + history bottom sheet for a habit. Mirrors
 * `HabitBottomSheetBuilders.showUnifiedHistorySheet` / `_UnifiedCalendarSheet` in
 * `widgets/habit/habit_bottom_sheet.dart`: stats row, an interactive monthly calendar
 * (tap a day to mark/unmark a completion), and a reverse-chronological timeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitBottomSheet(
    habitWithCompletions: HabitWithCompletions,
    onDismissRequest: () -> Unit,
    onDateSelected: (Instant) -> Unit,
    onDateDeselected: (LocalDate) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.90f)) {
            UnifiedCalendarSheetContent(
                habitWithCompletions = habitWithCompletions,
                onDateSelected = onDateSelected,
                onDateDeselected = onDateDeselected,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnifiedCalendarSheetContent(
    habitWithCompletions: HabitWithCompletions,
    onDateSelected: (Instant) -> Unit,
    onDateDeselected: (LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(today)) }

    val zone = ZoneId.systemDefault()
    val completions = remember(habitWithCompletions) {
        habitWithCompletions.completions.sortedByDescending { it.completionDate }
    }
    val completionDates = remember(completions) {
        completions.map { it.completionDate.atZone(zone).toLocalDate() }.toSet()
    }

    val nowInstant = remember { Instant.now() }
    val last30 = nowInstant.minusSeconds(30L * 24 * 3600)
    val last90 = nowInstant.minusSeconds(90L * 24 * 3600)
    val count30 = completions.count { it.completionDate.isAfter(last30) }
    val count90 = completions.count { it.completionDate.isAfter(last90) }
    val countAll = completions.size

    var dateForRemoval by remember { mutableStateOf<LocalDate?>(null) }
    var dateForAdd by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()) {
        Text(
            text = stringResource(R.string.habitHistoryTitle),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(AppTheme.spaceLarge),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spaceLarge),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HistoryStatCard(stringResource(R.string.habitHistoryStatLast30), count30.toString())
            HistoryStatCard(stringResource(R.string.habitHistoryStatLast90), count90.toString())
            HistoryStatCard(stringResource(R.string.habitHistoryStatTotal), countAll.toString())
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = AppTheme.spaceXLarge))

        // --- Calendar header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spaceLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = null)
            }
            Text(
                text = io.github.benji377.timety.util.datetime.AppDateFormatUtils.formatMonthYear(displayedMonth.atDay(1)),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.spaceSmall))

        // --- Weekday header ---
        val weekdayLabels = listOf(
            stringResource(R.string.commonWeekdayMon),
            stringResource(R.string.commonWeekdayTue),
            stringResource(R.string.commonWeekdayWed),
            stringResource(R.string.commonWeekdayThu),
            stringResource(R.string.commonWeekdayFri),
            stringResource(R.string.commonWeekdaySat),
            stringResource(R.string.commonWeekdaySun),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall),
        ) {
            weekdayLabels.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = AppTheme.fsBodySmall,
                    color = Color.Gray,
                )
            }
        }

        // --- Calendar grid ---
        val daysInMonth = displayedMonth.lengthOfMonth()
        val firstDayOfWeek = displayedMonth.atDay(1).dayOfWeek.value // 1=Mon..7=Sun
        val emptySlotsPrefix = firstDayOfWeek - 1
        val cells: List<LocalDate?> = buildList {
            repeat(emptySlotsPrefix) { add(null) }
            for (day in 1..daysInMonth) add(displayedMonth.atDay(day))
        }
        val rows = cells.chunked(7)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spaceLarge),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
        ) {
            rows.forEach { rowCells ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
                ) {
                    rowCells.forEach { date ->
                        Box(modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.1f)) {
                            if (date == null) {
                                // empty slot
                            } else {
                                val isToday = date == today
                                val isCompleted = completionDates.contains(date)
                                val isFuture = date.isAfter(today)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .background(
                                            color = if (isFuture) Color.Transparent
                                            else MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                                alpha = 0.5f
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .then(
                                            if (isToday) Modifier.border(
                                                2.dp,
                                                HabitColor,
                                                RoundedCornerShape(12.dp)
                                            )
                                            else Modifier
                                        )
                                        .clickable(enabled = !isFuture) {
                                            if (isCompleted) dateForRemoval = date else dateForAdd =
                                                date
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isFuture) MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.3f
                                        )
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (isCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 4.dp)
                                                .size(6.dp)
                                                .background(HabitColor, CircleShape),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
        HorizontalDivider()

        // --- Timeline ---
        if (completions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.habitHistoryEmpty), color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    top = AppTheme.spaceLarge,
                    bottom = AppTheme.space2XLarge
                ),
            ) {
                items(completions, key = { it.id }) { completion ->
                    val isLast = completion == completions.last()
                    TimelineItem(
                        completion = completion,
                        isLast = isLast,
                        onDelete = {
                            onDateDeselected(completion.completionDate.atZone(zone).toLocalDate())
                        },
                    )
                }
            }
        }
    }

    // Remove-completion confirmation dialog
    val removalDate = dateForRemoval
    if (removalDate != null) {
        val formattedDate = io.github.benji377.timety.util.datetime.AppDateFormatUtils.formatDate(
            removalDate,
            io.github.benji377.timety.ui.utils.LocalDateFormatSettings.current.dateFormatCode
        )
        ConfirmationDialog(
            visible = true,
            title = stringResource(R.string.habitHistoryRemoveTitle),
            content = stringResource(R.string.habitHistoryRemoveCompletion, formattedDate),
            confirmLabel = stringResource(R.string.commonLabelRemove),
            confirmColor = ErrorColor,
            onConfirm = {
                onDateDeselected(removalDate)
                dateForRemoval = null
            },
            onDismiss = { dateForRemoval = null },
        )
    }

    // Add-completion time picker
    val addDate = dateForAdd
    if (addDate != null) {
        val now = LocalTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = now.hour,
            initialMinute = now.minute,
            is24Hour = io.github.benji377.timety.ui.utils.LocalDateFormatSettings.current.use24HourFormat,
        )
        AlertDialog(
            onDismissRequest = { dateForAdd = null },
            title = { Text(stringResource(R.string.habitHistoryTimePrompt).uppercase()) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val instant = addDate
                        .atTime(timePickerState.hour, timePickerState.minute)
                        .atZone(zone)
                        .toInstant()
                    onDateSelected(instant)
                    dateForAdd = null
                }) {
                    Text(stringResource(R.string.commonLabelConfirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { dateForAdd = null }) {
                    Text(stringResource(R.string.commonLabelCancel))
                }
            },
        )
    }
}

@Composable
private fun HistoryStatCard(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = SuccessColor,
        )
        Text(text = label, fontSize = AppTheme.fsBodySmall, color = Color.Gray)
    }
}

@Composable
private fun TimelineItem(
    completion: HabitCompletionEntity,
    isLast: Boolean,
    onDelete: () -> Unit,
) {
    val dfs = io.github.benji377.timety.ui.utils.LocalDateFormatSettings.current
    val dateLabel = io.github.benji377.timety.util.datetime.AppDateFormatUtils.formatDate(
        completion.completionDate, dfs.dateFormatCode
    )
    val timeLabel = io.github.benji377.timety.util.datetime.AppDateFormatUtils.formatTime(
        completion.completionDate, dfs.use24HourFormat
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = AppTheme.spaceLarge),
    ) {
        // Left: timeline graphics (dot + connecting line)
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(HabitColor.copy(alpha = AppTheme.opacityLight)),
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(12.dp)
                    .background(HabitColor, CircleShape),
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = AppTheme.spaceXLarge, top = AppTheme.spaceSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dateLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.habitHistoryCompletedAt, timeLabel),
                    color = Color.Gray,
                    fontSize = 13.sp,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.habitHistoryRemoveTooltip),
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
