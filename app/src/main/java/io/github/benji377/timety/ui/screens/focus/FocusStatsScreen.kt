package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTagEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.ui.components.common.WeekNavigator
import io.github.benji377.timety.ui.components.focus.localizedFocusModeName
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.stats.StatsUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.style.TextAlign
import io.github.benji377.timety.data.model.focus.DistractionUIType
import io.github.benji377.timety.ui.theme.WarningAccent
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.viewmodel.DistractionWithSession


@Composable
fun FocusStatsScreen(
    focusViewModel: FocusViewModel = activityScopedViewModel(),
) {
    val sessions by focusViewModel.allSessions.collectAsState()
    val tags by focusViewModel.allTags.collectAsState()
    val modes by focusViewModel.allModes.collectAsState()
    val distractions by focusViewModel.allDistractions.collectAsState()

    var focusedWeek by remember { mutableStateOf(LocalDate.now()) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var selectedTagFilterId by remember { mutableStateOf<String?>(null) }

    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.focusStatsEmpty))
        }
        return
    }

    val zone = ZoneId.systemDefault()
    val tagById = remember(tags) { tags.associateBy { it.id } }
    val modeById = remember(modes) { modes.associateBy { it.id } }

    val filteredSessions = remember(sessions, selectedTagFilterId) {
        if (selectedTagFilterId == null) sessions else sessions.filter { it.tagId == selectedTagFilterId }
    }

    val startOfWeek = AppDateUtils.startOfWeekMonday(focusedWeek)
    val endOfWeek = startOfWeek.plusDays(6)
    val isCurrentRealWeek = AppDateUtils.isWithinInclusive(LocalDate.now(), startOfWeek, endOfWeek)

    val clockSessions = remember(filteredSessions, selectedDay) {
        filteredSessions.filter {
            AppDateUtils.isSameDay(
                it.startTime.atZone(zone).toLocalDate(),
                selectedDay
            )
        }
    }
    val clockTotalMins = clockSessions.sumOf { it.totalSecondsFocused / 60 }

    val defaultTargetEmpty = stringResource(R.string.focusTargetEmpty)
    val defaultTargetUntagged = stringResource(R.string.focusTargetUntagged)
    val defaultModeLabel = stringResource(R.string.focusLabelDefault)

    fun resolveTargetName(session: FocusSessionEntity): String = when (session.targetType) {
        FocusTargetType.TASK, FocusTargetType.HABIT ->
            session.targetLabel?.trim()?.takeIf { it.isNotEmpty() } ?: defaultTargetEmpty

        FocusTargetType.TAG ->
            if (session.tagId != null) {
                tagById[session.tagId]?.name ?: session.targetLabel ?: defaultTargetUntagged
            } else {
                session.targetLabel?.trim()?.takeIf { it.isNotEmpty() } ?: defaultTargetUntagged
            }
    }

    val selectedDayDistractions = remember(distractions, selectedTagFilterId, selectedDay) {
        distractions.filter { entry ->
            (selectedTagFilterId == null || entry.session.tagId == selectedTagFilterId) &&
                    AppDateUtils.isSameDay(
                        entry.distraction.time.atZone(zone).toLocalDate(),
                        selectedDay
                    )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppTheme.spaceLarge)
    ) {
        item {
            WeekNavigator(
                focusedDate = focusedWeek,
                onShiftWeek = { days ->
                    focusedWeek = focusedWeek.plusDays(days.toLong())
                    selectedDay = AppDateUtils.startOfWeekMonday(focusedWeek)
                },
            )
            Spacer(modifier = Modifier.height(AppTheme.spaceXLarge))

            TagFilterRow(
                sessions = sessions,
                tags = tags,
                selectedTagId = selectedTagFilterId,
                onSelect = { selectedTagFilterId = it },
            )
            if (sessions.any { it.tagId != null }) Spacer(modifier = Modifier.height(AppTheme.spaceXLarge))

            SectionHeader(
                stringResource(R.string.focusStatsSectionClockTitle),
                stringResource(R.string.focusStatsSectionClockSubtitle),
            )
            Spacer(modifier = Modifier.height(AppTheme.spaceLarge))

            DayPillSelector(startOfWeek, selectedDay) { selectedDay = it }
            Spacer(modifier = Modifier.height(AppTheme.space2XLarge))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                    FocusClockChart(
                        sessions = clockSessions,
                        color = FocusColor,
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            AppDateFormatUtils.formatMinutesCompact(clockTotalMins),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            stringResource(
                                R.string.focusStatsSectionClockSessions,
                                clockSessions.size
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppTheme.space3XLarge))

            SectionHeader(
                stringResource(R.string.focusStatsSectionSessionsTitle),
                stringResource(R.string.focusStatsSectionSessionsSubtitle),
            )
            Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            val sortedSessions = remember(clockSessions) {
                clockSessions.sortedByDescending { it.startTime }
            }
            if (sortedSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppTheme.spaceXLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.focusStatsSectionSessionsEmpty))
                }
            } else {
                // A single day can still hold many sessions; keep the section at a fixed
                // max height and let it scroll internally instead of stretching the page.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    itemsIndexed(sortedSessions, key = { _, s -> s.id }) { index, session ->
                        SessionRow(
                            session,
                            resolveTargetName(session),
                            session.modeId.let { id ->
                                modeById[id]?.let { localizedFocusModeName(it) } ?: defaultModeLabel
                            },
                            tagById
                        )
                        if (index != sortedSessions.lastIndex) HorizontalDivider(
                            modifier = Modifier.padding(
                                vertical = AppTheme.spaceMedium
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(AppTheme.space3XLarge))

            SectionHeader(
                stringResource(R.string.focusStatsSectionDistractionsTitle),
                stringResource(R.string.focusStatsSectionDistractionsSubtitle),
            )
            Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            if (selectedDayDistractions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppTheme.spaceXLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.focusStatsSectionDistractionsEmpty))
                }
            } else {
                selectedDayDistractions.forEachIndexed { index, entry ->
                    DistractionRow(entry, resolveTargetName(entry.session))
                    if (index != selectedDayDistractions.lastIndex) HorizontalDivider()
                }
            }
            Spacer(modifier = Modifier.height(AppTheme.space3XLarge))

            SectionHeader(
                stringResource(R.string.focusStatsSectionVolumeTitle),
                stringResource(R.string.focusStatsSectionVolumeSubtitle),
            )
            Spacer(modifier = Modifier.height(AppTheme.spaceLarge))
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            ) {
                VolumeBarChart(filteredSessions, startOfWeek, isCurrentRealWeek, zone)
            }
            Spacer(modifier = Modifier.height(AppTheme.space3XLarge))

            // Deliberately unfiltered: this section is an all-time summary. Task- and
            // habit-linked sessions carry no tagId, so applying the tag filter here would
            // zero out their bars and make the breakdown look broken.
            TargetBreakdownSection(sessions)
            Spacer(modifier = Modifier.height(AppTheme.space3XLarge))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(AppTheme.spaceXSmall))
        Text(
            subtitle,
            fontSize = AppTheme.fsBodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TagFilterRow(
    sessions: List<FocusSessionEntity>,
    tags: List<FocusTagEntity>,
    selectedTagId: String?,
    onSelect: (String?) -> Unit,
) {
    val tagById = remember(tags) { tags.associateBy { it.id } }
    val counts = remember(sessions, tags) {
        val map = LinkedHashMap<String, Int>()
        sessions.forEach { s ->
            val id = s.tagId; if (id != null && tagById.containsKey(id)) map[id] =
            (map[id] ?: 0) + 1
        }
        map
    }
    if (counts.isEmpty()) return

    val usedTags = remember(counts, tags) {
        counts.keys.mapNotNull { tagById[it] }.sortedBy { it.name }
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)
    ) {
        FilterChip(
            selected = selectedTagId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.focusTagsLabelAll)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = FocusColor,
                selectedLabelColor = Color.White
            ),
        )
        usedTags.forEach { tag ->
            FilterChip(
                selected = selectedTagId == tag.id,
                onClick = { onSelect(if (selectedTagId == tag.id) null else tag.id) },
                label = { Text("${tag.name} (${counts[tag.id] ?: 0})") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(tag.colorValue))
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(tag.colorValue).copy(alpha = 0.25f),
                ),
            )
        }
    }
}

@Composable
private fun DayPillSelector(
    startOfWeek: LocalDate,
    selectedDay: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall)
    ) {
        for (i in 0 until 7) {
            val day = startOfWeek.plusDays(i.toLong())
            val isSelected = day == selectedDay
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) FocusColor else Color.Transparent)
                    .then(
                        Modifier.background(
                            color = Color.Transparent,
                        ),
                    )
                    .padding(horizontal = AppTheme.spaceMedium, vertical = AppTheme.spaceSmall)
                    .then(Modifier)
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        Modifier.background(if (isSelected) FocusColor else Color.Transparent)
                    )
                    .then(
                        Modifier.clickable(
                            onClick = { onSelect(day) },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() })
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = AppDateFormatUtils.formatWeekdayDay(
                        day
                    ),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FocusClockChart(
    sessions: List<FocusSessionEntity>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val textMeasurer = rememberTextMeasurer()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f
        val strokeWidth = 24.dp.toPx()

        drawCircle(
            color = trackColor,
            radius = radius - strokeWidth / 2f,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        val markers = listOf("0" to -90.0, "6" to 0.0, "12" to 90.0, "18" to 180.0)
        markers.forEach { (label, deg) ->
            val rad = Math.toRadians(deg)
            val r = radius - strokeWidth - 10.dp.toPx()
            val x = center.x + (r * kotlin.math.cos(rad)).toFloat()
            val y = center.y + (r * kotlin.math.sin(rad)).toFloat()
            val layout =
                textMeasurer.measure(label, style = TextStyle(color = markerColor, fontSize = 10.sp))
            drawText(
                layout,
                topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f)
            )
        }

        sessions.forEach { session ->
            val startZoned = session.startTime.atZone(zone)
            val startHour = startZoned.hour + startZoned.minute / 60.0
            val end = session.endTime ?: Instant.now()
            val endZoned = end.atZone(zone)
            var endHour = endZoned.hour + endZoned.minute / 60.0
            if (startZoned.toLocalDate() != endZoned.toLocalDate()) endHour = 24.0

            val sweepHours = endHour - startHour
            if (sweepHours <= 0) return@forEach

            val startAngleDeg = (startHour / 24.0) * 360.0 - 90.0
            val sweepAngleDeg = (sweepHours / 24.0) * 360.0

            drawArc(
                color = color.copy(alpha = 0.8f),
                startAngle = startAngleDeg.toFloat(),
                sweepAngle = sweepAngleDeg.toFloat(),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size((radius - strokeWidth / 2f) * 2, (radius - strokeWidth / 2f) * 2),
                topLeft = Offset(
                    center.x - (radius - strokeWidth / 2f),
                    center.y - (radius - strokeWidth / 2f)
                ),
            )
        }
    }
}

@Composable
private fun SessionRow(
    session: FocusSessionEntity,
    targetName: String,
    modeName: String,
    tagById: Map<String, FocusTagEntity>,
) {
    val (icon: ImageVector, color: Color) = when (session.targetType) {
        FocusTargetType.TASK -> Icons.Filled.TaskAlt to TaskColor
        FocusTargetType.HABIT -> Icons.Filled.Alarm to HabitColor
        FocusTargetType.TAG -> Icons.Outlined.LocalOffer to (session.tagId?.let {
            tagById[it]?.let { tag ->
                Color(
                    tag.colorValue
                )
            }
        } ?: FocusColor)
    }
    val dfs = LocalDateFormatSettings.current
    val dateStr = AppDateFormatUtils.formatDate(
        session.startTime,
        dfs.dateFormatCode
    )
    val timeStr = AppDateFormatUtils.formatTime(
        session.startTime,
        dfs.use24HourFormat
    )
    val mins = session.totalSecondsFocused / 60

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
        Column {
            Text(targetName, fontWeight = FontWeight.SemiBold, color = color)
            Text(
                "$dateStr | $timeStr | ${AppDateFormatUtils.formatMinutesCompact(mins)} | $modeName",
                fontSize = AppTheme.fsBodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DistractionRow(
    entry: DistractionWithSession,
    targetName: String
) {
    val type =
        DistractionUIType.fromEntityType(entry.distraction.type)
    val use24Hour =
        LocalDateFormatSettings.current.use24HourFormat
    val zone = remember { ZoneId.systemDefault() }

    Row(
        modifier = Modifier.padding(vertical = AppTheme.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WarningAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(type.icon, contentDescription = null, tint = type.color)
        }
        Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
        Column {
            Text(type.getLocalizedName(), fontWeight = FontWeight.SemiBold)
            Text(
                "${
                    AppDateFormatUtils.formatTimeWithSeconds(
                        entry.distraction.time,
                        use24Hour,
                        zone
                    )
                } | $targetName",
                fontSize = AppTheme.fsBodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VolumeBarChart(
    sessions: List<FocusSessionEntity>,
    startOfWeek: LocalDate,
    isCurrentRealWeek: Boolean,
    zone: ZoneId
) {
    val dailyMins = IntArray(7)
    sessions.forEach { session ->
        val day = session.startTime.atZone(zone).toLocalDate()
        val offset = java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, day)
        if (offset in 0..6) dailyMins[offset.toInt()] += session.totalSecondsFocused / 60
    }
    val maxY = StatsUtils.maxValue(dailyMins.toList(), minimum = 60.0)
    val denom = (maxY * 1.2).toFloat().coerceAtLeast(1f)
    val todayIndex = if (isCurrentRealWeek) LocalDate.now().dayOfWeek.value - 1 else -1

    val days = listOf(
        stringResource(R.string.commonWeekdayMon),
        stringResource(R.string.commonWeekdayTue),
        stringResource(R.string.commonWeekdayWed),
        stringResource(R.string.commonWeekdayThu),
        stringResource(R.string.commonWeekdayFri),
        stringResource(R.string.commonWeekdaySat),
        stringResource(R.string.commonWeekdaySun),
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until 7) {
            val isToday = i == todayIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val h = (dailyMins[i] / denom).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(h)
                            .width(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isToday) FocusColor else FocusColor.copy(alpha = 0.25f)),
                    )
                }
                Spacer(modifier = Modifier.height(AppTheme.spaceSmall))
                Text(
                    days[i],
                    fontSize = AppTheme.fsBodySmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) FocusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class TargetTypeStat(
    val label: String,
    val color: Color,
    val icon: ImageVector,
    val minutes: Int
)

@Composable
private fun TargetBreakdownSection(sessions: List<FocusSessionEntity>) {
    val tagsLabel = stringResource(R.string.globalLabelTags)
    val tasksLabel = stringResource(R.string.globalLabelTasks)
    val habitsLabel = stringResource(R.string.globalLabelHabits)

    val totals = remember(sessions) {
        val map = mutableMapOf(
            FocusTargetType.TAG to 0,
            FocusTargetType.TASK to 0,
            FocusTargetType.HABIT to 0
        )
        sessions.forEach { s ->
            map[s.targetType] = (map[s.targetType] ?: 0) + s.totalSecondsFocused / 60
        }
        map
    }

    val stats = listOf(
        TargetTypeStat(
            tagsLabel,
            FocusColor,
            Icons.Outlined.LocalOffer,
            totals[FocusTargetType.TAG] ?: 0
        ),
        TargetTypeStat(
            tasksLabel,
            TaskColor,
            Icons.Filled.TaskAlt,
            totals[FocusTargetType.TASK] ?: 0
        ),
        TargetTypeStat(
            habitsLabel,
            HabitColor,
            Icons.Filled.Alarm,
            totals[FocusTargetType.HABIT] ?: 0
        ),
    )
    val totalMinutes = stats.sumOf { it.minutes }

    Column {
        Row(verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    stringResource(R.string.focusStatsSectionTargetBreakdownTitle),
                    stringResource(R.string.focusStatsSectionTargetBreakdownSubtitle),
                )
            }
            Spacer(modifier = Modifier.width(AppTheme.spaceLarge))
            Text(AppDateFormatUtils.formatMinutesCompact(totalMinutes), fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (totalMinutes == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                )
            } else {
                stats.filter { it.minutes > 0 }.forEach { stat ->
                    Box(
                        modifier = Modifier
                            .weight(stat.minutes.toFloat())
                            .fillMaxHeight()
                            .background(stat.color)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        stats.forEach { stat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppTheme.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(stat.color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    stat.label,
                    modifier = Modifier.weight(1f),
                    fontSize = AppTheme.fsBodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    AppDateFormatUtils.formatMinutesCompact(stat.minutes),
                    fontSize = AppTheme.fsBodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(AppTheme.spaceMedium))
                val pct =
                    if (totalMinutes == 0) 0 else Math.round((stat.minutes.toFloat() / totalMinutes) * 100)
                Text(
                    "$pct%",
                    fontSize = AppTheme.fsBodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

