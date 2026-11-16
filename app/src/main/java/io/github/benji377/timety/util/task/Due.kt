package io.github.benji377.timety.util.task

import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A due date with or without a time of day. A date-only due is stored as the local start of its
 * day and stays on time until that day ends.
 */
data class Due(val at: Instant, val hasTime: Boolean) {
    /** The local day this is due on. */
    fun date(zone: ZoneId = ZoneId.systemDefault()): LocalDate = at.atZone(zone).toLocalDate()

    /** When the task turns overdue: the due time, or the end of a date-only due's day. */
    fun deadline(zone: ZoneId = ZoneId.systemDefault()): Instant =
        if (hasTime) at else date(zone).plusDays(1).atStartOfDay(zone).toInstant()

    fun isOverdue(now: Instant, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        deadline(zone).isBefore(now)

    /** What reminder presets count back from: the due time, or [DATE_ONLY_REMINDER_TIME] on the day. */
    fun reminderAnchor(zone: ZoneId = ZoneId.systemDefault()): Instant =
        if (hasTime) at else date(zone).atTime(DATE_ONLY_REMINDER_TIME).atZone(zone).toInstant()

    /** The presets that make sense here; a date-only due has no time to count minutes from. */
    val presets: List<ReminderPreset>
        get() = if (hasTime) ReminderPreset.entries else ReminderPreset.entries.filter { it.isWholeDays }

    companion object {
        /** When "on the day" and day-based reminders fire for a date-only due. */
        val DATE_ONLY_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

        /** [date] at [timeMinutes] after midnight, or date-only when [timeMinutes] is null. */
        fun of(date: LocalDate, timeMinutes: Int?, zone: ZoneId = ZoneId.systemDefault()): Due =
            if (timeMinutes == null) {
                Due(date.atStartOfDay(zone).toInstant(), hasTime = false)
            } else {
                // atZone resolves DST-gap local times instead of throwing.
                Due(date.atTime(timeMinutes / 60, timeMinutes % 60).atZone(zone).toInstant(), hasTime = true)
            }
    }
}

val TaskEntity.due: Due? get() = dueDate?.let { Due(it, hasDueTime) }

val RecurringTaskEntity.due: Due get() = Due(dueDate, hasDueTime)
