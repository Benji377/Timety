package io.github.benji377.timety.util.stats

import io.github.benji377.timety.data.model.focus.FocusSessionEntity
import io.github.benji377.timety.data.model.focus.FocusTargetType
import io.github.benji377.timety.data.model.tag.TagEntity
import io.github.benji377.timety.util.tag.TagFilter
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Sort orders of the focus history list. */
enum class FocusHistorySort {
    NEWEST,
    OLDEST,
    LONGEST,
}

/** What the focus history list is narrowed to; every property left empty matches everything. */
data class FocusHistoryFilter(
    val dates: ClosedRange<LocalDate>? = null,
    val tagIds: Set<String> = emptySet(),
    val targetType: FocusTargetType? = null,
    val modeId: String? = null,
    val search: String = "",
)

/** A session listed under one day, with the minutes that fell on that day. */
data class FocusHistoryEntry(val session: FocusSessionEntity, val minutesOnDay: Int)

/** One day of the list. */
data class FocusHistoryDay(val day: LocalDate, val minutes: Int, val entries: List<FocusHistoryEntry>)

/** One month of the list. */
data class FocusHistoryMonth(val month: YearMonth, val minutes: Int, val days: List<FocusHistoryDay>)

/** The list's headline: each session counts once, its minutes only within the filtered dates. */
data class FocusHistorySummary(val sessions: Int, val minutes: Int)

/** Filtering, grouping and sorting behind the focus history page. */
object FocusHistoryQuery {
    /**
     * The [sessions] matching [filter]. A session matches [FocusHistoryFilter.dates] if any day it
     * touches does. [searchTextOf] gives the display text of a session, as names are resolved by the
     * caller; the search ignores case.
     */
    fun filter(
        sessions: List<FocusSessionEntity>,
        filter: FocusHistoryFilter,
        tagsBySession: Map<String, List<TagEntity>>,
        zone: ZoneId,
        searchTextOf: (FocusSessionEntity) -> String,
    ): List<FocusSessionEntity> {
        val search = filter.search.trim()
        return sessions.filter { session ->
            (filter.dates == null || daysOf(session, zone).keys.any { it in filter.dates }) &&
                TagFilter.matches(tagsBySession[session.id].orEmpty(), filter.tagIds) &&
                (filter.targetType == null || session.targetType == filter.targetType) &&
                (filter.modeId == null || session.modeId == filter.modeId) &&
                (search.isEmpty() || searchTextOf(session).contains(search, ignoreCase = true))
        }
    }

    /**
     * [sessions] grouped by month and day. A session crossing midnight is listed under each day it
     * touches within [dates], with that day's minutes, so day totals match the charts.
     */
    fun groupByDay(
        sessions: List<FocusSessionEntity>,
        dates: ClosedRange<LocalDate>?,
        sort: FocusHistorySort,
        zone: ZoneId,
    ): List<FocusHistoryMonth> {
        val ascending = sort == FocusHistorySort.OLDEST
        val entriesByDay = HashMap<LocalDate, MutableList<FocusHistoryEntry>>()
        sessions.forEach { session ->
            daysOf(session, zone).forEach { (day, minutes) ->
                if (dates == null || day in dates) {
                    entriesByDay.getOrPut(day) { mutableListOf() } += FocusHistoryEntry(session, minutes)
                }
            }
        }
        val days = entriesByDay.map { (day, entries) ->
            val byStart = entries.sortedBy { it.session.startTime }
            FocusHistoryDay(
                day = day,
                minutes = entries.sumOf { it.minutesOnDay },
                entries = if (ascending) byStart else byStart.reversed(),
            )
        }
        val sortedDays = if (ascending) days.sortedBy { it.day } else days.sortedByDescending { it.day }
        return sortedDays.groupBy { YearMonth.from(it.day) }.map { (month, monthDays) ->
            FocusHistoryMonth(month, monthDays.sumOf { it.minutes }, monthDays)
        }
    }

    /** [sessions] as a flat list for [FocusHistorySort.LONGEST], newest first among equals. */
    fun longestFirst(sessions: List<FocusSessionEntity>): List<FocusSessionEntity> =
        sessions.sortedWith(
            compareByDescending<FocusSessionEntity> { it.totalSecondsFocused }
                .thenByDescending { it.startTime }
        )

    /** The summary line for [sessions] within [dates]. */
    fun summary(
        sessions: List<FocusSessionEntity>,
        dates: ClosedRange<LocalDate>?,
        zone: ZoneId,
    ): FocusHistorySummary = FocusHistorySummary(
        sessions = sessions.size,
        minutes = sessions.sumOf { session ->
            daysOf(session, zone).filterKeys { dates == null || it in dates }.values.sum()
        },
    )

    /** Every day any of [sessions] touches, for marking days in the date picker. */
    fun daysWithSessions(sessions: List<FocusSessionEntity>, zone: ZoneId): Set<LocalDate> =
        sessions.flatMapTo(HashSet()) { daysOf(it, zone).keys }

    // A session under a minute has no minutes to split but still belongs on its start day.
    private fun daysOf(session: FocusSessionEntity, zone: ZoneId): Map<LocalDate, Int> =
        FocusSessionDayStats.minutesByDay(session, zone)
            .ifEmpty { mapOf(session.startTime.atZone(zone).toLocalDate() to 0) }
}
