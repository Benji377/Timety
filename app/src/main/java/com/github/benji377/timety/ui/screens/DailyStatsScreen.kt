package com.github.benji377.timety.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.benji377.timety.data.DailyEvent
import com.github.benji377.timety.data.FocusSession
import com.github.benji377.timety.viewmodel.StatsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStatsScreen(
    viewModel: StatsViewModel,
    initialDate: Long,
    onBack: () -> Unit
) {
    var currentDateMillis by remember { mutableLongStateOf(initialDate) }
    val allSessions by viewModel.allSessions.collectAsState()
    val user by viewModel.user.collectAsState()

    val dailySessions = remember(allSessions, currentDateMillis) {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDateMillis }
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
        allSessions.filter { it.startTime in startOfDay until endOfDay }
    }

    val dailyEvents by viewModel.getEventsForDay(currentDateMillis)
        .collectAsState(initial = emptyList())

    val totalFocussedMillis = dailySessions.sumOf { it.duration }
    val dailyTargetMillis = user?.dailyFocusTarget ?: (120 * 60 * 1000L)
    val progress = (totalFocussedMillis.toFloat() / dailyTargetMillis.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = {
                            val cal =
                                Calendar.getInstance().apply { timeInMillis = currentDateMillis }
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            currentDateMillis = cal.timeInMillis
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Day"
                            )
                        }
                        Text(
                            text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(
                                Date(
                                    currentDateMillis
                                )
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = {
                            val cal =
                                Calendar.getInstance().apply { timeInMillis = currentDateMillis }
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            currentDateMillis = cal.timeInMillis
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Day"
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Circular24hGraph(sessions = dailySessions)
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${totalFocussedMillis / 60000} / ${dailyTargetMillis / 60000} mins",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val timelineItems = (dailySessions.map { it as Any } + dailyEvents.map { it as Any })
                .sortedBy {
                    if (it is FocusSession) it.startTime else (it as DailyEvent).timestamp
                }

            items(timelineItems) { item ->
                if (item is FocusSession) {
                    TimelineItem(item)
                } else {
                    EventTimelineItem(item as DailyEvent)
                }
            }
        }
    }
}

@Composable
fun EventTimelineItem(event: DailyEvent) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(60.dp)) {
            Text(
                text = timeStr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.type,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
fun Circular24hGraph(sessions: List<FocusSession>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .size(280.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2
            val strokeWidth = 20.dp.toPx()

            // Draw base gray circle
            drawCircle(
                color = secondaryColor,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Draw hour marks
            for (i in 0 until 24) {
                val angleRad = (i * 15f - 90f) * (Math.PI / 180f).toFloat()
                val start = Offset(
                    center.x + (radius - strokeWidth / 2) * cos(angleRad),
                    center.y + (radius - strokeWidth / 2) * sin(angleRad)
                )
                val end = Offset(
                    center.x + (radius + strokeWidth / 2) * cos(angleRad),
                    center.y + (radius + strokeWidth / 2) * sin(angleRad)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = start,
                    end = end,
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Draw sessions
            sessions.forEach { session ->
                val startCal = Calendar.getInstance().apply { timeInMillis = session.startTime }
                val endCal = Calendar.getInstance().apply { timeInMillis = session.endTime }

                val startHour =
                    startCal.get(Calendar.HOUR_OF_DAY) + startCal.get(Calendar.MINUTE) / 60f
                val endHour = endCal.get(Calendar.HOUR_OF_DAY) + endCal.get(Calendar.MINUTE) / 60f

                val startAngle = (startHour * 15f - 90f)
                val sweepAngle = (endHour - startHour) * 15f

                drawArc(
                    color = primaryColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "24h",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text("Focus Distribution", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TimelineItem(session: FocusSession) {
    val startTimeStr =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.startTime))
    val endTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.endTime))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(60.dp)) {
            Text(
                text = startTimeStr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = endTimeStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Focus Session",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${session.duration / 60000} minutes",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!session.note.isNullOrBlank()) {
                    Text(
                        text = session.note,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}
