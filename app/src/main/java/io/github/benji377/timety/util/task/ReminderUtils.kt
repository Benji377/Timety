package io.github.benji377.timety.util.task

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/** One-tap reminder offsets before a due date; [minutes] is how long before it fires. */
enum class ReminderPreset(val minutes: Int) {
    ON_TIME(0),
    MINUTES_30(30),
    HOUR_1(60),
    DAY_1(24 * 60),
}

/** Why a custom reminder time was rejected. */
enum class CustomReminderIssue { IN_PAST, AFTER_DUE }

/** The reminders after a due-date change, and whether that changed them. */
data class ReminderShift(val reminders: List<Instant>, val changed: Boolean)

/**
 * Reminder rules for tasks, which store absolute instants. Presets are inferred by comparing a
 * reminder with the due date, so no offset is stored. Pure and unit-testable.
 */
object ReminderUtils {

    /** Matches the scheduler's notification slots (11) with one spare. */
    const val MAX_REMINDERS = 10

    /** The preset [reminder] sits exactly [ReminderPreset.minutes] before [due], or null. */
    fun presetOf(due: Instant, reminder: Instant): ReminderPreset? {
        val gap = Duration.between(reminder, due)
        return ReminderPreset.entries.firstOrNull { gap == Duration.ofMinutes(it.minutes.toLong()) }
    }

    /** The instant [preset] resolves to for [due]. */
    fun timeFor(due: Instant, preset: ReminderPreset): Instant =
        due.minus(preset.minutes.toLong(), ChronoUnit.MINUTES)

    /** Removes [preset]'s reminder if present, otherwise adds it. */
    fun toggle(reminders: List<Instant>, due: Instant, preset: ReminderPreset): List<Instant> {
        val time = timeFor(due, preset)
        return if (time in reminders) reminders - time else (reminders + time).sorted()
    }

    /** Whether [preset] can be tapped: a selected one can always be removed, a new one must fit. */
    fun isPresetAvailable(
        reminders: List<Instant>,
        due: Instant,
        preset: ReminderPreset,
        now: Instant,
    ): Boolean {
        val time = timeFor(due, preset)
        return time in reminders || (time.isAfter(now) && reminders.size < MAX_REMINDERS)
    }

    /** Null when [time] is a valid custom reminder for a task due at [due] (which may be unset). */
    fun customIssue(time: Instant, due: Instant?, now: Instant): CustomReminderIssue? = when {
        !time.isAfter(now) -> CustomReminderIssue.IN_PAST
        due != null && time.isAfter(due) -> CustomReminderIssue.AFTER_DUE
        else -> null
    }

    /**
     * Moves preset reminders along with a due date change from [oldDue] to [newDue]. Custom
     * reminders stay put, except those that would end up after the new due date. Shifted reminders
     * that land in the past are dropped because they could never fire.
     */
    fun shiftWithDueDate(
        reminders: List<Instant>,
        oldDue: Instant?,
        newDue: Instant?,
        now: Instant,
    ): ReminderShift {
        if (oldDue == null || newDue == null || oldDue == newDue) return ReminderShift(reminders, false)
        val shifted = reminders.mapNotNull { reminder ->
            val preset = presetOf(oldDue, reminder)
            if (preset != null) timeFor(newDue, preset).takeIf { it.isAfter(now) }
            else reminder.takeUnless { it.isAfter(newDue) }
        }.distinct().sorted()
        return ReminderShift(shifted, shifted != reminders.sorted())
    }
}
