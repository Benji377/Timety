package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.benji377.timety.data.FocusSession
import com.github.benji377.timety.data.Task
import com.github.benji377.timety.ui.components.TaskCard
import com.github.benji377.timety.utils.DateUtils
import com.github.benji377.timety.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDayClick: (Long) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonthMillis by viewModel.currentMonth.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val selectedDayTasks by viewModel.selectedDayTasks.collectAsState()
    val selectedDaySessions by viewModel.selectedDaySessions.collectAsState()
    val selectedDayFocusTime by viewModel.selectedDayFocusTime.collectAsState()

    val calendar = Calendar.getInstance().apply { timeInMillis = currentMonthMillis }
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearText = monthYearFormat.format(calendar.time)

    Column(modifier = Modifier.fillMaxSize()) {
        // Month Carousel Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.previousMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
            }
            Text(
                text = monthYearText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
        }

        // Calendar Grid
        CalendarGrid(
            currentMonthMillis = currentMonthMillis,
            selectedDate = selectedDate,
            allTasks = allTasks,
            allSessions = allSessions,
            onDateSelected = { viewModel.selectDate(it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Day Details
        Box(modifier = Modifier.weight(1f)) {
            if (selectedDayTasks.isEmpty() && selectedDayFocusTime == 0L) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No activity for this day", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (selectedDayFocusTime > 0) {
                        item {
                            Text(
                                "Focus Sessions",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(selectedDaySessions) { session ->
                            FocusSessionItem(session)
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .clickable { onDayClick(selectedDate) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Total Focus Time",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "${selectedDayFocusTime / 60000} minutes",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }

                    if (selectedDayTasks.isNotEmpty()) {
                        item {
                            Text(
                                "Tasks",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(selectedDayTasks) { task ->
                            TaskCard(
                                task = task,
                                onCheckedChange = { /* viewModel.toggleTaskStatus(task) */ },
                                onClick = { /* onTaskClick(task.id) */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonthMillis: Long,
    selectedDate: Long,
    allTasks: List<Task>,
    allSessions: List<FocusSession>,
    onDateSelected: (Long) -> Unit
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Σ")
    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentMonthMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // Adjust for Monday start (Monday=2 in Java Calendar)
    val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Calendar.getInstance().apply {
        timeInMillis = currentMonthMillis
        add(Calendar.MONTH, -1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)) {
        // Day Labels
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (day == "Σ") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Rows
        val firstDayCal = Calendar.getInstance().apply {
            timeInMillis = currentMonthMillis
            set(Calendar.DAY_OF_MONTH, 1)
            // Go to the first day of the grid (possibly previous month)
            add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)
        }

        val gridCalendar = Calendar.getInstance().apply {
            timeInMillis = firstDayCal.timeInMillis
        }

        for (row in 0..5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var rowFocusMins = 0L
                var rowTasks = 0

                for (col in 0..6) {
                    val dayInMillis = gridCalendar.timeInMillis
                    val displayDay = gridCalendar.get(Calendar.DAY_OF_MONTH)
                    val isCurrentMonth = gridCalendar.get(Calendar.MONTH) == Calendar.getInstance()
                        .apply { timeInMillis = currentMonthMillis }.get(Calendar.MONTH)

                    val isSelected = DateUtils.isSameDay(dayInMillis, selectedDate)
                    val dayTasks = allTasks.filter {
                        it.dueDate != null && DateUtils.isSameDay(
                            it.dueDate,
                            dayInMillis
                        )
                    }.size
                    val dayFocusMins =
                        allSessions.filter { DateUtils.isSameDay(it.startTime, dayInMillis) }
                            .sumOf { it.duration } / 60000

                    rowTasks += dayTasks
                    rowFocusMins += dayFocusMins

                    DayCell(
                        day = displayDay,
                        isCurrentMonth = isCurrentMonth,
                        isSelected = isSelected,
                        taskCount = dayTasks,
                        focusMins = dayFocusMins.toInt(),
                        modifier = Modifier.weight(1f),
                        onClick = { onDateSelected(dayInMillis) }
                    )

                    gridCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                // Weekly Summary Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .padding(2.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (rowTasks > 0) {
                        Text(
                            text = "T: $rowTasks",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (rowFocusMins > 0) {
                        Text(
                            text = "${rowFocusMins}m",
                            color = Color(0xFF4CAF50),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Check if we finished the month
            val nextRowFirstDay = Calendar.getInstance().apply {
                timeInMillis = gridCalendar.timeInMillis
            }
            if (nextRowFirstDay.get(Calendar.MONTH) != Calendar.getInstance()
                    .apply { timeInMillis = currentMonthMillis }.get(Calendar.MONTH) && row >= 4
            ) {
                break
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    taskCount: Int,
    focusMins: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(60.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.4f
            ),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (taskCount > 0) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.Blue)
                )
            }
            if (focusMins > 0) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }
        if (taskCount > 0 || focusMins > 0) {
            Text(
                text = "${if (taskCount > 0) taskCount else ""}${if (taskCount > 0 && focusMins > 0) "/" else ""}${if (focusMins > 0) focusMins else ""}",
                fontSize = 8.sp,
                lineHeight = 8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FocusSessionItem(session: FocusSession) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startTime = timeFormat.format(Date(session.startTime))
    val endTime = timeFormat.format(Date(session.endTime))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${session.duration / 60000} min session",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$startTime - $endTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (session.rating != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = session.rating.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
