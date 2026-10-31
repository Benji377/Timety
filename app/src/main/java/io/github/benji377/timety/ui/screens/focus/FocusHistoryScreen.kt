package io.github.benji377.timety.ui.screens.focus

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.focus.displayName
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.ui.components.common.BackNavigationIcon
import io.github.benji377.timety.ui.components.common.NeoFilterChip
import io.github.benji377.timety.ui.components.common.NeoIconButton
import io.github.benji377.timety.ui.components.common.NeoOutlinedTextField
import io.github.benji377.timety.ui.components.common.NeoTextButton
import io.github.benji377.timety.ui.components.common.NeoTopBar
import io.github.benji377.timety.ui.components.focus.EditSessionDialog
import io.github.benji377.timety.ui.components.focus.localizedFocusModeName
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.ui.utils.quantityString
import io.github.benji377.timety.ui.utils.rememberToday
import io.github.benji377.timety.ui.viewmodel.DistractionWithSession
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.TagViewModel
import io.github.benji377.timety.ui.viewmodel.activityScopedViewModel
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.stats.FocusHistoryDatePreset
import io.github.benji377.timety.util.stats.FocusHistoryEntry
import io.github.benji377.timety.util.stats.FocusHistoryFilter
import io.github.benji377.timety.util.stats.FocusHistoryMonth
import io.github.benji377.timety.util.stats.FocusHistoryQuery
import io.github.benji377.timety.util.stats.FocusHistorySort
import io.github.benji377.timety.util.stats.FocusHistorySummary
import io.github.benji377.timety.util.tag.TagFilter
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class HistorySheet {
    DATE,
    TAGS,
    TARGET,
    MODE,
    SORT,
}

/** What the list shows for the current filter and sort. */
private data class HistoryContent(
    val sessions: List<FocusSessionEntity>,
    val months: List<FocusHistoryMonth>,
    val summary: FocusHistorySummary,
)

/** Everything a session row needs that isn't on the session itself. */
private data class HistoryRowContext(
    val sessionTags: Map<String, List<TagEntity>>,
    val modeNames: Map<String, String>,
    val distractions: Map<String, List<DistractionWithSession>>,
    val untaggedLabel: String,
    val defaultModeLabel: String,
    val onEdit: (FocusSessionEntity) -> Unit,
)

private val FilterSaver = listSaver<FocusHistoryFilter, Any?>(
    save = { filter ->
        listOf(
            filter.dates?.start?.toEpochDay(),
            filter.dates?.endInclusive?.toEpochDay(),
            ArrayList(filter.tagIds),
            filter.targetType?.name,
            filter.modeId,
            filter.search,
        )
    },
    restore = { saved ->
        val (start, end) = saved
        FocusHistoryFilter(
            dates = if (start is Long && end is Long) {
                LocalDate.ofEpochDay(start)..LocalDate.ofEpochDay(end)
            } else {
                null
            },
            tagIds = (saved[2] as List<*>).filterIsInstance<String>().toSet(),
            targetType = (saved[3] as String?)?.let { FocusTargetType.valueOf(it) },
            modeId = saved[4] as String?,
            search = saved[5] as String,
        )
    },
)

/**
 * Every logged focus session, newest first and grouped by month and day. Search, filters and sort
 * narrow the list; filtering to one day adds its clock chart and distractions.
 */
@Composable
fun FocusHistoryScreen(
    onNavigateBack: () -> Unit,
    focusViewModel: FocusViewModel = activityScopedViewModel(),
    tagViewModel: TagViewModel = activityScopedViewModel(),
) {
    val sessions by focusViewModel.allSessions.collectAsState()
    val modes by focusViewModel.allModes.collectAsState()
    val allDistractions by focusViewModel.allDistractions.collectAsState()
    val sessionTags by tagViewModel.sessionTags.collectAsState()

    var requestedFilter by rememberSaveable(stateSaver = FilterSaver) {
        mutableStateOf(FocusHistoryFilter())
    }
    var sort by rememberSaveable { mutableStateOf(FocusHistorySort.NEWEST) }
    var openSheet by rememberSaveable { mutableStateOf<HistorySheet?>(null) }
    var editingSession by remember { mutableStateOf<FocusSessionEntity?>(null) }

    val zone = remember { ZoneId.systemDefault() }
    val today by rememberToday()
    val usedTags = remember(sessions, sessionTags) {
        TagFilter.usedTags(sessions.map { it.id }, sessionTags)
    }
    // A tag that's no longer in use can't keep filtering behind a hidden option.
    val filter = requestedFilter.copy(tagIds = TagFilter.activeIds(requestedFilter.tagIds, usedTags))

    val untaggedLabel = stringResource(R.string.focusTargetUntagged)
    val defaultModeLabel = stringResource(R.string.focusLabelDefault)
    val modeNames = modes.associate { it.id to localizedFocusModeName(it) }
    val distractionsBySession = remember(allDistractions) {
        allDistractions.sortedBy { it.distraction.time }.groupBy { it.session.id }
    }

    val content by produceState<HistoryContent?>(
        initialValue = null,
        sessions, filter, sort, sessionTags, modeNames,
    ) {
        value = withContext(Dispatchers.Default) {
            val matching = FocusHistoryQuery.filter(sessions, filter, sessionTags, zone) { session ->
                val tags = sessionTags[session.id].orEmpty()
                listOfNotNull(
                    session.displayName(tags),
                    tags.joinToString(" ") { it.name },
                    modeNames[session.modeId],
                ).joinToString(" ")
            }
            HistoryContent(
                sessions = if (sort == FocusHistorySort.LONGEST) FocusHistoryQuery.longestFirst(matching) else matching,
                months = FocusHistoryQuery.groupByDay(matching, filter.dates, sort, zone),
                summary = FocusHistoryQuery.summary(matching, filter.dates, zone),
            )
        }
    }

    val rowContext = HistoryRowContext(
        sessionTags = sessionTags,
        modeNames = modeNames,
        distractions = distractionsBySession,
        untaggedLabel = untaggedLabel,
        defaultModeLabel = defaultModeLabel,
        onEdit = { editingSession = it },
    )

    Scaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.focusHistoryTitle),
                navigationIcon = { BackNavigationIcon(onClick = onNavigateBack) },
                actions = {
                    NeoIconButton(
                        onClick = { openSheet = HistorySheet.SORT },
                        icon = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.focusHistorySortTitle),
                    )
                    Spacer(Modifier.padding(end = AppTheme.spaceSmall))
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (sessions.isEmpty()) {
                CenteredMessage(stringResource(R.string.focusHistoryEmpty))
            } else {
                HistoryControls(
                    filter = filter,
                    today = today,
                    tagNames = usedTags,
                    modeName = filter.modeId?.let { modeNames[it] },
                    onSearchChange = { requestedFilter = filter.copy(search = it) },
                    onOpenSheet = { openSheet = it },
                )
                content?.let {
                    HistoryBody(
                        content = it,
                        filter = filter,
                        sort = sort,
                        today = today,
                        rowContext = rowContext,
                        onDayChange = { day -> requestedFilter = filter.copy(dates = day..day) },
                        onClearFilters = { requestedFilter = FocusHistoryFilter() },
                    )
                }
            }
        }
    }

    when (openSheet) {
        HistorySheet.DATE -> HistoryDateSheet(
            current = filter.dates,
            today = today,
            daysWithSessions = remember(sessions) { FocusHistoryQuery.daysWithSessions(sessions, zone) },
            onSelect = {
                requestedFilter = filter.copy(dates = it)
                openSheet = null
            },
            onDismiss = { openSheet = null },
        )

        HistorySheet.TAGS -> HistoryOptionSheet(
            title = stringResource(R.string.focusHistoryChipTags),
            multiSelect = true,
            options = usedTags.map { tag ->
                val selected = tag.id in filter.tagIds
                HistoryOption(tag.name, selected) {
                    requestedFilter = filter.copy(
                        tagIds = if (selected) filter.tagIds - tag.id else filter.tagIds + tag.id
                    )
                }
            },
            onDismiss = { openSheet = null },
        )

        HistorySheet.TARGET -> HistoryOptionSheet(
            title = stringResource(R.string.focusHistoryChipTarget),
            options = listOf(
                HistoryOption(stringResource(R.string.taskListFilterAll), filter.targetType == null) {
                    requestedFilter = filter.copy(targetType = null)
                    openSheet = null
                }
            ) + FocusTargetType.entries.map { type ->
                HistoryOption(stringResource(type.pluralLabelRes), filter.targetType == type) {
                    requestedFilter = filter.copy(targetType = type)
                    openSheet = null
                }
            },
            onDismiss = { openSheet = null },
        )

        HistorySheet.MODE -> HistoryOptionSheet(
            title = stringResource(R.string.focusHistoryChipMode),
            options = listOf(
                HistoryOption(stringResource(R.string.taskListFilterAll), filter.modeId == null) {
                    requestedFilter = filter.copy(modeId = null)
                    openSheet = null
                }
            ) + modes.map { mode ->
                HistoryOption(modeNames[mode.id].orEmpty(), filter.modeId == mode.id) {
                    requestedFilter = filter.copy(modeId = mode.id)
                    openSheet = null
                }
            },
            onDismiss = { openSheet = null },
        )

        HistorySheet.SORT -> HistoryOptionSheet(
            title = stringResource(R.string.focusHistorySortTitle),
            options = FocusHistorySort.entries.map { option ->
                HistoryOption(stringResource(option.labelRes), sort == option) {
                    sort = option
                    openSheet = null
                }
            },
            onDismiss = { openSheet = null },
        )

        null -> Unit
    }

    editingSession?.let { session ->
        EditSessionDialog(
            session = session,
            focusViewModel = focusViewModel,
            onDismiss = { editingSession = null },
        )
    }
}

private val FocusHistorySort.labelRes: Int
    get() = when (this) {
        FocusHistorySort.NEWEST -> R.string.focusHistorySortNewest
        FocusHistorySort.OLDEST -> R.string.focusHistorySortOldest
        FocusHistorySort.LONGEST -> R.string.focusHistorySortLongest
    }

/** The string naming this quick date range. */
internal val FocusHistoryDatePreset.labelRes: Int
    get() = when (this) {
        FocusHistoryDatePreset.ALL_TIME -> R.string.focusHistoryDateAllTime
        FocusHistoryDatePreset.LAST_7_DAYS -> R.string.focusHistoryDateLast7Days
        FocusHistoryDatePreset.LAST_30_DAYS -> R.string.focusHistoryDateLast30Days
        FocusHistoryDatePreset.THIS_YEAR -> R.string.focusHistoryDateThisYear
    }

@Composable
private fun HistoryControls(
    filter: FocusHistoryFilter,
    today: LocalDate,
    tagNames: List<TagEntity>,
    modeName: String?,
    onSearchChange: (String) -> Unit,
    onOpenSheet: (HistorySheet) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = AppTheme.spaceMedium),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
    ) {
        NeoOutlinedTextField(
            value = filter.search,
            onValueChange = onSearchChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.focusHistorySearchHint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (filter.search.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.focusHistorySearchClear),
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.spaceLarge),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = AppTheme.spaceLarge),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spaceSmall),
        ) {
            item {
                HistoryChip(
                    label = dateChipLabel(filter.dates, today),
                    active = filter.dates != null,
                    onClick = { onOpenSheet(HistorySheet.DATE) },
                )
            }
            if (tagNames.isNotEmpty()) {
                item {
                    HistoryChip(
                        label = tagChipLabel(filter.tagIds, tagNames),
                        active = filter.tagIds.isNotEmpty(),
                        onClick = { onOpenSheet(HistorySheet.TAGS) },
                    )
                }
            }
            item {
                HistoryChip(
                    label = stringResource(filter.targetType?.pluralLabelRes ?: R.string.focusHistoryChipTarget),
                    active = filter.targetType != null,
                    onClick = { onOpenSheet(HistorySheet.TARGET) },
                )
            }
            item {
                HistoryChip(
                    label = modeName ?: stringResource(R.string.focusHistoryChipMode),
                    active = filter.modeId != null,
                    onClick = { onOpenSheet(HistorySheet.MODE) },
                )
            }
        }
    }
}

@Composable
private fun HistoryChip(label: String, active: Boolean, onClick: () -> Unit) {
    NeoFilterChip(
        selected = active,
        onClick = onClick,
        label = label,
        selectedColor = FocusColor,
        leadingIcon = {
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        },
    )
}

@Composable
@ReadOnlyComposable
private fun dateChipLabel(dates: ClosedRange<LocalDate>?, today: LocalDate): String {
    val preset = FocusHistoryDatePreset.entries.find { it.range(today) == dates }
    return when {
        preset != null -> stringResource(preset.labelRes)
        dates == null -> stringResource(R.string.focusHistoryDateAllTime)
        dates.start == dates.endInclusive -> AppDateFormatUtils.formatShortDate(dates.start)
        else -> stringResource(
            R.string.focusHistoryDateRange,
            AppDateFormatUtils.formatShortDate(dates.start),
            AppDateFormatUtils.formatShortDate(dates.endInclusive),
        )
    }
}

@Composable
@ReadOnlyComposable
private fun tagChipLabel(selected: Set<String>, tags: List<TagEntity>): String = when (selected.size) {
    0 -> stringResource(R.string.focusHistoryChipTags)
    1 -> tags.first { it.id in selected }.name
    else -> stringResource(R.string.focusHistoryChipTagsCount, selected.size)
}

@Composable
private fun HistoryBody(
    content: HistoryContent,
    filter: FocusHistoryFilter,
    sort: FocusHistorySort,
    today: LocalDate,
    rowContext: HistoryRowContext,
    onDayChange: (LocalDate) -> Unit,
    onClearFilters: () -> Unit,
) {
    val singleDay = filter.dates?.takeIf { it.start == it.endInclusive }?.start
    if (content.sessions.isEmpty() && singleDay == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.focusHistoryNoResults))
            NeoTextButton(
                onClick = onClearFilters,
                text = stringResource(R.string.focusHistoryClearFilters),
            )
        }
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filter, sort) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppTheme.space3XLarge),
    ) {
        if (singleDay != null) {
            singleDayItems(singleDay, content, today, rowContext, onDayChange)
        } else {
            item(key = "summary", contentType = "summary") { HistorySummaryLine(content.summary) }
            if (sort == FocusHistorySort.LONGEST) {
                items(content.sessions, key = { it.id }, contentType = { "session" }) { session ->
                    HistoryRow(session, rowContext, showDate = true)
                }
            } else {
                groupedItems(content.months, today, rowContext)
            }
        }
    }
}

private fun LazyListScope.groupedItems(
    months: List<FocusHistoryMonth>,
    today: LocalDate,
    rowContext: HistoryRowContext,
) {
    months.forEach { month ->
        stickyHeader(key = "month-${month.month}", contentType = "month") {
            GroupHeader(
                title = AppDateFormatUtils.formatMonthYear(month.month.atDay(1)),
                minutes = month.minutes,
            )
        }
        month.days.forEach { day ->
            item(key = "day-${day.day}", contentType = "day") {
                DayHeader(day.day, day.minutes, today)
            }
            items(
                day.entries,
                key = { entry -> "${day.day}-${entry.session.id}" },
                contentType = { "session" },
            ) { entry ->
                HistoryRow(entry.session, rowContext, showDate = false)
            }
        }
    }
}

private fun LazyListScope.singleDayItems(
    day: LocalDate,
    content: HistoryContent,
    today: LocalDate,
    rowContext: HistoryRowContext,
    onDayChange: (LocalDate) -> Unit,
) {
    val entries: List<FocusHistoryEntry> = content.months.flatMap { it.days }.flatMap { it.entries }
    item(key = "day-summary", contentType = "day-summary") {
        SingleDayHeader(
            day = day,
            today = today,
            sessions = content.sessions,
            minutes = content.summary.minutes,
            onDayChange = onDayChange,
        )
    }
    if (entries.isEmpty()) {
        item(key = "day-empty", contentType = "day-empty") {
            Text(
                stringResource(R.string.focusStatsSectionSessionsEmpty),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spaceLarge),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(entries, key = { it.session.id }, contentType = { "session" }) { entry ->
        HistoryRow(entry.session, rowContext, showDate = false, showDistractions = true)
    }
}

@Composable
private fun HistorySummaryLine(summary: FocusHistorySummary) {
    Text(
        text = quantityString(
            R.plurals.focusHistorySummary,
            summary.sessions,
            formatArgs = arrayOf<Any>(summary.sessions, AppDateFormatUtils.formatMinutesCompact(summary.minutes)),
        ),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall),
    )
}

@Composable
private fun GroupHeader(title: String, minutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = AppTheme.spaceLarge, vertical = AppTheme.spaceSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Text(AppDateFormatUtils.formatMinutesCompact(minutes), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayHeader(day: LocalDate, minutes: Int, today: LocalDate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AppTheme.spaceLarge,
                end = AppTheme.spaceLarge,
                top = AppTheme.spaceMedium,
                bottom = AppTheme.spaceXSmall,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (day == today) {
                stringResource(R.string.focusHistoryToday)
            } else {
                AppDateFormatUtils.formatWeekdayDay(day)
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = AppDateFormatUtils.formatMinutesCompact(minutes),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SingleDayHeader(
    day: LocalDate,
    today: LocalDate,
    sessions: List<FocusSessionEntity>,
    minutes: Int,
    onDayChange: (LocalDate) -> Unit,
) {
    val dfs = LocalDateFormatSettings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spaceLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoIconButton(
                onClick = { onDayChange(day.minusDays(1)) },
                icon = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.commonLabelPrevious),
            )
            Text(
                AppDateFormatUtils.formatDate(day, dfs.dateFormatCode),
                fontWeight = FontWeight.Bold,
            )
            NeoIconButton(
                onClick = { onDayChange(day.plusDays(1)) },
                icon = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.commonLabelNext),
                enabled = day.isBefore(today),
            )
        }
        Spacer(Modifier.height(AppTheme.spaceLarge))
        Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
            FocusClockChart(
                sessions = sessions,
                day = day,
                color = FocusColor,
                modifier = Modifier.fillMaxSize(),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    AppDateFormatUtils.formatMinutesCompact(minutes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    quantityString(
                        R.plurals.focusStatsSectionClockSessions,
                        sessions.size,
                        formatArgs = arrayOf(sessions.size),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    session: FocusSessionEntity,
    context: HistoryRowContext,
    showDate: Boolean,
    showDistractions: Boolean = false,
) {
    val tags = context.sessionTags[session.id].orEmpty()
    val targetName = session.displayName(tags) ?: context.untaggedLabel
    val distractions = context.distractions[session.id].orEmpty()
    Column(
        modifier = Modifier.padding(
            horizontal = AppTheme.spaceLarge,
            vertical = AppTheme.spaceSmall,
        ),
    ) {
        SessionRow(
            session = session,
            targetName = targetName,
            modeName = context.modeNames[session.modeId] ?: context.defaultModeLabel,
            tags = tags,
            onEdit = { context.onEdit(session) },
            showDate = showDate,
            showTags = true,
            distractionCount = if (showDistractions) 0 else distractions.size,
        )
        if (showDistractions) {
            distractions.forEach { entry ->
                DistractionRow(entry, targetName)
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = AppTheme.spaceLarge))
}

@Composable
private fun CenteredMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message)
    }
}
