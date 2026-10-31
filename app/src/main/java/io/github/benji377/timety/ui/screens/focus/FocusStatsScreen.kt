package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.displayName
import io.github.benji377.timety.ui.components.common.WeekNavigator
import io.github.benji377.timety.ui.components.focus.EditSessionDialog
import io.github.benji377.timety.ui.components.focus.localizedFocusModeName
import io.github.benji377.timety.ui.components.stats.SectionHeader
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.stats.FocusSessionDayStats
import java.time.LocalDate
import java.time.ZoneId

/**
 * Focus session statistics: a 24-hour clock of the selected day's sessions, a session list,
 * distraction log, weekly volume chart, and an all-time breakdown by target type.
 */
@Composable
fun FocusStatsScreen(
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val sessions by focusViewModel.allSessions.collectAsState()
    val tags by focusViewModel.allTags.collectAsState()
    val sessionTags by tagViewModel.sessionTags.collectAsState()
    val modes by focusViewModel.allModes.collectAsState()
    val distractions by focusViewModel.allDistractions.collectAsState()

    var focusedWeek by remember { mutableStateOf(LocalDate.now()) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var selectedTagFilterId by remember { mutableStateOf<String?>(null) }
    var editingSession by remember { mutableStateOf<FocusSessionEntity?>(null) }

    // A tag that no longer has sessions (deleted or retargeted away) hides its chip and the "All"
    // chip with it, so drop the filter instead of leaving the screen stuck on an empty selection.
    LaunchedEffect(sessions, tags, selectedTagFilterId) {
        val id = selectedTagFilterId ?: return@LaunchedEffect
        if (tags.none { it.id == id } || sessions.none { it.tagId == id }) selectedTagFilterId = null
    }

    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.focusStatsEmpty))
        }
        return
    }

    val zone = ZoneId.systemDefault()
    val modeById = remember(modes) { modes.associateBy { it.id } }

    val filteredSessions = remember(sessions, selectedTagFilterId) {
        if (selectedTagFilterId == null) sessions else sessions.filter { it.tagId == selectedTagFilterId }
    }

    val startOfWeek = AppDateUtils.startOfWeekMonday(focusedWeek)
    val endOfWeek = startOfWeek.plusDays(6)
    val isCurrentRealWeek = AppDateUtils.isWithinInclusive(LocalDate.now(), startOfWeek, endOfWeek)

    // Every session overlapping the day, counting only the minutes that fell on it, so a
    // 22:00-02:00 session reads 2h on each side of midnight. The clock and the session list both
    // show it on both days; it is still one stored session, listed with its real start and length.
    val clockMinutesBySession = remember(filteredSessions, selectedDay) {
        filteredSessions.mapNotNull { session ->
            FocusSessionDayStats.minutesByDay(session, zone)[selectedDay]?.let { session to it }
        }
    }
    val clockSessions = clockMinutesBySession.map { it.first }
    val clockTotalMins = clockMinutesBySession.sumOf { it.second }

    val defaultTargetUntagged = stringResource(R.string.focusTargetUntagged)
    val defaultModeLabel = stringResource(R.string.focusLabelDefault)

    fun resolveTargetName(session: FocusSessionEntity): String =
        session.displayName(sessionTags[session.id].orEmpty()) ?: defaultTargetUntagged

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
            FocusHeatmap(sessions)
            Spacer(modifier = Modifier.height(AppTheme.space3XLarge))

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
                        day = selectedDay,
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
                            quantityString(
                                R.plurals.focusStatsSectionClockSessions,
                                clockSessions.size,
                                formatArgs = arrayOf(clockSessions.size),
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
                            sessionTags[session.id].orEmpty(),
                            onEdit = { editingSession = session },
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

            // Deliberately unfiltered: this section is an all-time summary. Sessions linked to
            // anything but a tag carry no tagId, so applying the tag filter here would
            // zero out their bars and make the breakdown look broken.
            TargetBreakdownSection(sessions)
            Spacer(modifier = Modifier.height(AppTheme.space3XLarge))
        }
    }

    editingSession?.let { session ->
        EditSessionDialog(
            session = session,
            focusViewModel = focusViewModel,
            onDismiss = { editingSession = null },
        )
    }
}
