package io.github.benji377.timety.services

import android.content.Context
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.dataStore
import io.github.benji377.timety.util.LocaleHelper
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.widget.MidnightWidgetRefresh
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit


/** Builds and cancels the reminder notifications for tasks, habits, and daily/evening check-ins. */
class ReminderScheduler private constructor(private val context: Context) {

    private val notificationService = NotificationService(context)

    // Tasks.


    /** Re-arms [task]'s reminders; one already showing stays, so a resync can't swallow it. */
    fun scheduleTaskReminders(task: TaskEntity) {
        if (task.isCompleted) {
            cancelTaskReminders(task.id)
            return
        }
        cancelTaskAlarms(task.id)

        val now = Instant.now()
        val baseId = task.id.hashCode()
        var slot = 0

        task.reminders.forEach { reminder ->
            if (reminder.isAfter(now) && slot < TASK_ID_SLOTS) {
                notificationService.scheduleTaskReminder(
                    notificationId = baseId + slot,
                    title = context.getString(R.string.taskReminderTitle, task.title),
                    body = buildReminderBody(
                        task.title,
                        task.description,
                        reminder,
                        dueDate = task.dueDate
                    ),
                    scheduledTime = reminder,
                )
                slot++
            }
        }

        if (task.reminders.isEmpty() && task.dueDate != null && task.dueDate.isAfter(now)) {
            notificationService.scheduleTaskReminder(
                notificationId = baseId,
                title = context.getString(R.string.taskReminderTitle, task.title),
                body = buildReminderBody(
                    task.title,
                    task.description,
                    task.dueDate,
                    exactDueDate = true
                ),
                scheduledTime = task.dueDate,
            )
        }
    }

    fun cancelTaskReminders(taskId: String) {
        val baseId = taskId.hashCode()
        for (i in 0 until TASK_ID_SLOTS) notificationService.cancelNotification(baseId + i)
    }

    private fun cancelTaskAlarms(taskId: String) {
        val baseId = taskId.hashCode()
        for (i in 0 until TASK_ID_SLOTS) notificationService.cancelAlarm(baseId + i)
    }

    // Recurring tasks.


    /**
     * Schedules the reminders for a recurring task's *next* occurrence only; completing the
     * occurrence advances the due date and calls this again for the one after.
     */
    fun scheduleRecurringTaskReminders(task: RecurringTaskEntity) {
        cancelRecurringTaskAlarms(task.id)

        val now = Instant.now()
        val baseId = NotificationService.recurringTaskReminderBaseId(task.id)
        // No offsets configured = a single reminder at the due time, like plain tasks.
        val offsets = task.reminderOffsetsMinutes.ifEmpty { listOf(0) }.distinct().sorted()
        var slot = 0

        offsets.forEach { minutesBefore ->
            val reminder = task.dueDate.minus(minutesBefore.toLong(), ChronoUnit.MINUTES)
            if (reminder.isAfter(now) && slot < TASK_ID_SLOTS) {
                notificationService.scheduleTaskReminder(
                    notificationId = baseId + slot,
                    title = context.getString(R.string.taskReminderTitle, task.title),
                    body = buildReminderBody(
                        task.title,
                        task.description,
                        reminder,
                        dueDate = task.dueDate,
                        exactDueDate = minutesBefore == 0,
                    ),
                    scheduledTime = reminder,
                )
                slot++
            }
        }
    }

    fun cancelRecurringTaskReminders(id: String) {
        val baseId = NotificationService.recurringTaskReminderBaseId(id)
        for (i in 0 until TASK_ID_SLOTS) notificationService.cancelNotification(baseId + i)
    }

    private fun cancelRecurringTaskAlarms(id: String) {
        val baseId = NotificationService.recurringTaskReminderBaseId(id)
        for (i in 0 until TASK_ID_SLOTS) notificationService.cancelAlarm(baseId + i)
    }


    private fun buildReminderBody(
        title: String,
        description: String,
        reminderTime: Instant,
        dueDate: Instant? = null,
        exactDueDate: Boolean = false,
    ): String {
        if (exactDueDate) {
            return context.getString(R.string.taskReminderBodyExact, title) + descriptionHint(
                description
            )
        }

        val reference = dueDate ?: reminderTime
        val minutesUntilDue = Duration.between(reminderTime, reference).toMinutes()
        val res = context.resources
        val prefix = when {
            minutesUntilDue <= 0 -> context.getString(R.string.taskReminderPrefixNow)
            minutesUntilDue < 60 -> {
                val m = minutesUntilDue.toInt()
                res.getQuantityString(R.plurals.nTaskReminderPrefixMinutes, m, m)
            }

            minutesUntilDue < 24 * 60 -> {
                val h = (minutesUntilDue / 60).toInt()
                res.getQuantityString(R.plurals.nTaskReminderPrefixHours, h, h)
            }

            else -> {
                val d = (minutesUntilDue / (24 * 60)).toInt()
                res.getQuantityString(R.plurals.nTaskReminderPrefixDays, d, d)
            }
        }
        return context.getString(R.string.taskReminderBody, prefix, title) + descriptionHint(
            description
        )
    }

    /** The description's first non-empty line, appended on its own line so `BigTextStyle` surfaces the "why". */
    private fun descriptionHint(description: String): String {
        val hint = description.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return if (hint.isEmpty()) "" else "\n" + hint.take(100)
    }

    // Habits.


    /**
     * Arms [hwc]'s next reminder, its only pending one. Today's remaining times are skipped once
     * it no longer needs a reminder today; with no reminder left the habit's alarm is cancelled.
     */
    fun scheduleHabitReminder(hwc: HabitWithCompletions) {
        val habit = hwc.habit
        val next = HabitUtils.nextReminderAt(hwc, ZonedDateTime.now())
        if (next == null) {
            notificationService.cancelHabitReminder(habit.id)
            return
        }
        notificationService.scheduleHabitReminder(
            habitId = habit.id,
            title = context.getString(R.string.notificationHabitTitle),
            body = context.getString(R.string.notificationHabitBody, habit.name),
            triggerAtMillis = next.toInstant().toEpochMilli(),
        )
    }

    fun cancelHabitReminder(habitId: String) = notificationService.cancelHabitReminder(habitId)

    // Quick habits (interval reminders).


    /** Schedules (or reschedules) a quick habit's interval alarm; disabled quick habits are only cancelled. */
    fun scheduleQuickHabit(quickHabit: QuickHabitEntity) {
        if (!quickHabit.isEnabled) {
            notificationService.cancelQuickHabit(quickHabit.id)
            return
        }
        notificationService.scheduleQuickHabit(
            id = quickHabit.id,
            title = context.getString(R.string.quickHabitNotificationTitle),
            body = quickHabit.name,
            intervalMinutes = quickHabit.intervalMinutes,
            startMinuteOfDay = quickHabit.startMinuteOfDay,
            endMinuteOfDay = quickHabit.endMinuteOfDay,
            allowedWeekdays = HabitUtils.parseWeekdays(quickHabit.targetWeekdays),
        )
    }

    fun cancelQuickHabit(id: String) = notificationService.cancelQuickHabit(id)

    // Daily motivation / evening checkup.


    fun scheduleDailyMotivation(time: String) {
        val (hour, minute) = AppDateFormatUtils.parseHHmm(time, defaultHour = 0, defaultMinute = 0)
        val quotes = listOf(
            context.getString(R.string.notificationQuote1),
            context.getString(R.string.notificationQuote2),
            context.getString(R.string.notificationQuote3),
        )
        notificationService.scheduleDailyMotivation(
            hour = hour,
            minute = minute,
            title = context.getString(R.string.notificationGoodMorning),
            body = quotes[LocalDate.now().dayOfMonth % quotes.size],
        )
    }


    fun scheduleEndOfDayCheckup(time: String) {
        val (hour, minute) = AppDateFormatUtils.parseHHmm(time, defaultHour = 0, defaultMinute = 0)
        notificationService.scheduleEndOfDayCheckup(
            hour = hour,
            minute = minute,
            title = context.getString(R.string.notificationEveningTitle),
            body = context.getString(R.string.notificationEveningBody),
        )
    }


    companion object {

        private const val TASK_ID_SLOTS = 11

        /** Builds a scheduler whose string resources are resolved in the app's configured locale. */
        suspend fun create(context: Context): ReminderScheduler {
            val appContext = context.applicationContext
            val code = SettingsRepository(appContext.dataStore).appLocaleCodeFlow.first()
            return ReminderScheduler(LocaleHelper.wrap(appContext, code))
        }


        /**
         * Re-arms [habitId]'s reminder from its stored state, after the habit or its completions
         * changed. A reminder already showing is cleared once the habit is done today.
         */
        suspend fun refreshHabitReminder(context: Context, habitId: String) {
            val app = context.applicationContext as? TimetyApplication ?: return
            val repository = app.container.habitRepository
            val habit = repository.getHabitById(habitId) ?: return
            val hwc = HabitWithCompletions(habit, repository.getCompletionsForHabit(habitId).first())
            val scheduler = create(app)
            if (HabitUtils.isCompletedOn(hwc, LocalDate.now())) {
                scheduler.notificationService.dismissHabitNotifications(habitId)
            }
            scheduler.scheduleHabitReminder(hwc)
        }


        /** Re-arms every pending reminder and the midnight widget refresh; runs on each process start and system alarm reset. */
        suspend fun resyncAll(context: Context) {
            val app = context.applicationContext as? TimetyApplication ?: return
            val scheduler = create(app)
            val settings = app.container.settingsRepository

            // Android caps an app's pending alarms (500). The few app-wide alarms go first, so if a
            // huge number of reminders ever reaches the cap, the ones left out are item reminders.
            scheduler.scheduleDailyMotivation(settings.dailyMotivationTimeFlow.first())
            scheduler.scheduleEndOfDayCheckup(settings.endOfDayCheckupTimeFlow.first())
            MidnightWidgetRefresh.schedule(app)

            val now = Instant.now()
            // Only items with something ahead: completed or past ones were cancelled when they
            // changed, and re-cancelling each of them costs a dozen system calls on every start.
            app.container.taskRepository.allTasks.first()
                .map { it.task }
                .filter { task ->
                    !task.isCompleted &&
                        (task.reminders.any { it.isAfter(now) } || task.dueDate?.isAfter(now) == true)
                }
                .forEach { scheduler.scheduleTaskReminders(it) }
            app.container.recurringTaskRepository.allRecurringTasks.first()
                .forEach { scheduler.scheduleRecurringTaskReminders(it.task) }
            // Whether a habit still needs a reminder today depends only on this week's completions.
            val weekStart = AppDateUtils.startOfWeekMonday(LocalDate.now())
                .atStartOfDay(ZoneId.systemDefault()).toInstant()
            val recentCompletions = app.container.habitRepository.getCompletionsSince(weekStart)
                .groupBy { it.habitId }
            val habits = app.container.habitRepository.allHabits.first()
            if (!settings.isLegacyHabitAlarmsCleared()) {
                habits.forEach { scheduler.notificationService.cancelLegacyHabitReminders(it.id) }
                settings.saveLegacyHabitAlarmsCleared()
            }
            habits
                .filter { HabitUtils.parseReminderTimes(it.reminderTimes).isNotEmpty() }
                .forEach { habit ->
                    scheduler.scheduleHabitReminder(
                        HabitWithCompletions(habit, recentCompletions[habit.id].orEmpty())
                    )
                }
            app.container.quickHabitRepository.allQuickHabits.first()
                .filter { it.isEnabled }
                .forEach { scheduler.scheduleQuickHabit(it) }
        }
    }
}
