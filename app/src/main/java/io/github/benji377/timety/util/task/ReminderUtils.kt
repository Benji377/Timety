package io.github.benji377.timety.util.task

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One-tap reminder offsets before a due date; [minutes] is how long before it fires. */
enum class ReminderPreset(val minutes: Int) {
    ON_TIME(0),
    MINUTES_30(30),
    HOUR_1(60),
    DAY_1(24 * 60),
    WEEK_1(7 * 24 * 60);

    /** Whether this counts back whole days, the only presets a date-only due offers. */
    val isWholeDays: Boolean get() = minutes % MINUTES_PER_DAY == 0
}

private const val MINUTES_PER_DAY = 24 * 60

/** Why a custom reminder time was rejected. */
enum class CustomReminderIssue { IN_PAST, AFTER_DUE }

/** The reminders after a due-date change, and whether that changed them. */
data class ReminderShift(val reminders: List<Instant>, val changed: Boolean)

/**
 * Reminder rules for tasks, which store absolute instants. Presets are inferred by comparing a
 * reminder with the due's [Due.reminderAnchor], so no offset is stored. Pure and unit-testable.
 */
object ReminderUtils {
    /** Matches the scheduler's notification slots (11) with one spare. */
    const val MAX_REMINDERS = 10

    /** The preset of [due] that [reminder] sits on, or null. */
    fun presetOf(due: Due, reminder: Instant, zone: ZoneId = ZoneId.systemDefault()): ReminderPreset? {
        val gap = Duration.between(reminder, due.reminderAnchor(zone))
        return due.presets.firstOrNull { gap == Duration.ofMinutes(it.minutes.toLong()) }
    }

    /** The instant [preset] resolves to for [due]. */
    fun timeFor(due: Due, preset: ReminderPreset, zone: ZoneId = ZoneId.systemDefault()): Instant =
        due.reminderAnchor(zone).minus(preset.minutes.toLong(), ChronoUnit.MINUTES)

    /** [reminders] minus the presets of [due]. */
    fun withoutPresets(reminders: List<Instant>, due: Due, zone: ZoneId = ZoneId.systemDefault()): List<Instant> =
        reminders.filter { presetOf(due, it, zone) == null }

    /** Removes [preset]'s reminder if present, otherwise adds it. */
    fun toggle(
        reminders: List<Instant>,
        due: Due,
        preset: ReminderPreset,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Instant> {
        val time = timeFor(due, preset, zone)
        return if (time in reminders) reminders - time else (reminders + time).sorted()
    }

    /** Whether [preset] can be tapped: a selected one can always be removed, a new one must fit. */
    fun isPresetAvailable(
        reminders: List<Instant>,
        due: Due,
        preset: ReminderPreset,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (preset !in due.presets) return false
        val time = timeFor(due, preset, zone)
        return time in reminders || (time.isAfter(now) && reminders.size < MAX_REMINDERS)
    }

    /** Null when [time] is a valid custom reminder for a task with [due] (which may be unset). */
    fun customIssue(
        time: Instant,
        due: Due?,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): CustomReminderIssue? = when {
        !time.isAfter(now) -> CustomReminderIssue.IN_PAST
        due != null && time.isAfter(due.deadline(zone)) -> CustomReminderIssue.AFTER_DUE
        else -> null
    }

    /**
     * Moves preset reminders along with a due change from [oldDue] to [newDue]. Custom reminders
     * stay put, except those that would end up after the new deadline. Presets the new due doesn't
     * offer, and shifted ones that land in the past, are dropped.
     */
    fun shiftWithDueDate(
        reminders: List<Instant>,
        oldDue: Due?,
        newDue: Due?,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ReminderShift {
        if (oldDue == null || newDue == null || oldDue == newDue) return ReminderShift(reminders, false)
        val newDeadline = newDue.deadline(zone)
        val shifted = reminders.mapNotNull { reminder ->
            val preset = presetOf(oldDue, reminder, zone)
            when {
                preset == null -> reminder.takeUnless { it.isAfter(newDeadline) }
                preset !in newDue.presets -> null
                else -> timeFor(newDue, preset, zone).takeIf { it.isAfter(now) }
            }
        }.distinct().sorted()
        return ReminderShift(shifted, shifted != reminders.sorted())
    }
}
