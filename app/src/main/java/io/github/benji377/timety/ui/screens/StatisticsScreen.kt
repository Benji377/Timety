package io.github.benji377.timety.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.ui.components.common.TimetyTopBar
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.components.stats.StatCard
import io.github.benji377.timety.ui.screens.focus.FocusStatsScreen
import io.github.benji377.timety.ui.screens.habit.HabitStatsScreen
import io.github.benji377.timety.ui.screens.task.TaskStatsScreen
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.WarningColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.AppViewModelProvider
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt
import java.time.format.TextStyle as JavaTextStyle
import androidx.compose.material3.IconButton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.statsTabOverview),
        stringResource(R.string.statsTabTasks),
        stringResource(R.string.statsTabFocus),
        stringResource(R.string.statsTabHabits)
    )

    val activeColor = when (selectedTabIndex) {
        0 -> WarningColor
        1 -> TaskColor
        2 -> FocusColor
        3 -> HabitColor
        else -> MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TimetyTopBar(
                title = stringResource(R.string.statsTitle),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.commonBack)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .fillMaxSize()
                                // TabRow places the indicator above the tabs; without this the
                                // opaque pill covers the selected tab's label.
                                .zIndex(-1f)
                                .padding(4.dp)
                                .shadow(4.dp, RoundedCornerShape(25.dp))
                                .clip(RoundedCornerShape(25.dp))
                                .background(activeColor)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> OverviewStatsScreen()
                    1 -> TaskStatsScreen()
                    2 -> FocusStatsScreen()
                    3 -> HabitStatsScreen()
                }
            }
        }
    }
}


@Composable
private fun OverviewStatsScreen(
    taskViewModel: TaskViewModel = viewModel(factory = AppViewModelProvider.Factory),
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val dailyGoalMins by settingsViewModel.dailyGoalMins.collectAsState()
    // Collected so the KPI/chart recompute whenever new sessions are logged.
    val sessions by focusViewModel.allSessions.collectAsState()

    val zone = remember { ZoneId.systemDefault() }
    val now = remember { LocalDate.now() }

    val tasksPerDay = remember(tasks) {
        val map = mutableMapOf<LocalDate, Int>()
        tasks.forEach { t ->
            val completedAt = t.task.completedAt
            if (t.task.isCompleted && completedAt != null) {
                val day = completedAt.atZone(zone).toLocalDate()
                map[day] = (map[day] ?: 0) + 1
            }
        }
        map
    }

    val tasksCompletedToday = tasksPerDay[now] ?: 0
    val focusMinsToday = remember(sessions) { focusViewModel.getMinutesFocusedOnDay(now, zone) }
    val focusTarget = if (dailyGoalMins > 0) dailyGoalMins else 1
    val goalPercent =
        ((focusMinsToday.toDouble() / focusTarget.toDouble()).coerceIn(0.0, 1.0) * 100).toInt()

    val last7Days = remember(now) { (0..6).map { now.minusDays((6 - it).toLong()) } }
    val dailyFocus = remember(last7Days, sessions) {
        last7Days.map { day -> focusViewModel.getMinutesFocusedOnDay(day, zone) }
    }
    val dailyTasks = remember(last7Days, tasksPerDay) {
        last7Days.map { day -> tasksPerDay[day] ?: 0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.statsLabelSummary),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    title = stringResource(R.string.statsLabelTasksDone),
                    value = "$tasksCompletedToday",
                    icon = Icons.Filled.TaskAlt,
                    color = TaskColor,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.statsLabelFocus),
                    value = "${focusMinsToday}m",
                    icon = Icons.Filled.Timer,
                    color = FocusColor,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))

            StatCard(
                title = stringResource(R.string.statsLabelFocusGoal),
                value = "$goalPercent%",
                icon = Icons.Filled.TrackChanges,
                color = WarningColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = stringResource(R.string.statsLabelProductivity),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.statsLabelSynergy),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(FocusColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.statsLabelFocusMins),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(24.dp))
                LegendDot(TaskColor)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.statsLabelTasksDone),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(24.dp))

            SynergyChart(
                last7Days = last7Days,
                dailyFocus = dailyFocus,
                dailyTasks = dailyTasks,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Icon(
        imageVector = Icons.Filled.Circle,
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .height(12.dp)
            .width(12.dp)
    )
}


@Composable
private fun SynergyChart(
    last7Days: List<LocalDate>,
    dailyFocus: List<Int>,
    dailyTasks: List<Int>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val maxFocus =
        remember(dailyFocus) { (dailyFocus.maxOrNull() ?: 0).toDouble().coerceAtLeast(1.0) }
    val maxTasks =
        remember(dailyTasks) { (dailyTasks.maxOrNull() ?: 0).toDouble().coerceAtLeast(1.0) }
    val focusValues = remember(dailyFocus, maxFocus) { dailyFocus.map { (it / maxFocus) * 10.0 } }
    val taskValues = remember(dailyTasks, maxTasks) { dailyTasks.map { (it / maxTasks) * 10.0 } }

    val leftReservedPx = with(density) { 30.dp.toPx() }
    val bottomReservedPx = with(density) { 40.dp.toPx() }

    val todayColor = MaterialTheme.colorScheme.primary
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(canvasSize) {
                    if (canvasSize.width == 0) return@pointerInput
                    val plotWidthPx = canvasSize.width - leftReservedPx
                    if (plotWidthPx <= 0f) return@pointerInput

                    fun updateSelection(x: Float) {
                        val fraction = ((x - leftReservedPx) / plotWidthPx).coerceIn(0f, 1f)
                        selectedIndex = (fraction * 6).roundToInt().coerceIn(0, 6)
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        updateSelection(down.position.x)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null && change.pressed) {
                                updateSelection(change.position.x)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        selectedIndex = null
                    }
                }
        ) {
            val plotWidth = size.width - leftReservedPx
            val plotHeight = size.height - bottomReservedPx
            if (plotWidth <= 0f || plotHeight <= 0f) return@Canvas

            fun pointFor(values: List<Double>, index: Int): Offset {
                val x = leftReservedPx + (index / 6f) * plotWidth
                val y = plotHeight - ((values[index] / 11.0) * plotHeight).toFloat()
                return Offset(x, y)
            }

            // Y-axis labels (0, 5, 10).
            listOf(0, 5, 10).forEach { v ->
                val y = plotHeight - (v / 11f) * plotHeight
                val layout = textMeasurer.measure(
                    text = "$v",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = axisTextColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        leftReservedPx - 8.dp.toPx() - layout.size.width,
                        y - layout.size.height / 2f
                    )
                )
            }

            // X-axis weekday labels; today is bold + primary-colored.
            last7Days.forEachIndexed { index, day ->
                val isToday = index == 6
                val label = day.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                val layout = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = if (isToday) todayColor else axisTextColor,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
                val x = leftReservedPx + (index / 6f) * plotWidth
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x - layout.size.width / 2f, plotHeight + 12.dp.toPx())
                )
            }

            // Series: smoothed line + area fill below it.
            fun drawSeries(values: List<Double>, color: Color) {
                val points = values.indices.map { pointFor(values, it) }
                val linePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val p0 = points[i - 1]
                        val p1 = points[i]
                        val midX = (p0.x + p1.x) / 2f
                        cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, plotHeight)
                    lineTo(points.first().x, plotHeight)
                    close()
                }
                drawPath(areaPath, color = color.copy(alpha = 0.1f))
                drawPath(
                    linePath,
                    color = color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                // Per-point dots, matching fl_chart's default FlDotData in the Flutter app.
                points.forEach { point ->
                    drawCircle(color = color, radius = 4.dp.toPx(), center = point)
                }
            }

            drawSeries(focusValues, FocusColor)
            drawSeries(taskValues, TaskColor)

            // Touched-day indicator dots, enlarged to stand out from the regular dots.
            selectedIndex?.let { idx ->
                drawCircle(
                    color = FocusColor,
                    radius = 6.dp.toPx(),
                    center = pointFor(focusValues, idx)
                )
                drawCircle(
                    color = TaskColor,
                    radius = 6.dp.toPx(),
                    center = pointFor(taskValues, idx)
                )
            }
        }

        selectedIndex?.let { idx ->
            val plotWidth = canvasSize.width - leftReservedPx
            val plotHeight = canvasSize.height - bottomReservedPx
            if (plotWidth > 0f && plotHeight > 0f) {
                val focusY = plotHeight - ((focusValues[idx] / 11.0) * plotHeight).toFloat()
                val taskY = plotHeight - ((taskValues[idx] / 11.0) * plotHeight).toFloat()
                val topY = minOf(focusY, taskY)
                val x = leftReservedPx + (idx / 6f) * plotWidth
                val tooltipWidthPx = with(density) { 120.dp.toPx() }
                val tooltipHeightPx = with(density) { 56.dp.toPx() }
                val offsetX = (x - tooltipWidthPx / 2f)
                    .coerceIn(0f, (canvasSize.width - tooltipWidthPx).coerceAtLeast(0f))
                val offsetY =
                    (topY - tooltipHeightPx - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)

                Column(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .width(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = quantityString(
                            R.plurals.nMinutesCount,
                            dailyFocus[idx],
                            zeroRes = R.string.nMinutesCountZero,
                            dailyFocus[idx]
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusColor
                    )
                    Text(
                        text = quantityString(
                            R.plurals.nTasksCount,
                            dailyTasks[idx],
                            zeroRes = R.string.nTasksCountZero,
                            dailyTasks[idx]
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TaskColor
                    )
                }
            }
        }
    }
}
