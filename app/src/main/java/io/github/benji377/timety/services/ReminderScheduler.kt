package io.github.benji377.timety.services

import android.content.Context
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.habit.QuickHabitEntity
import io.github.benji377.timety.data.model.task.RecurringTaskEntity
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.dataStore
import io.github.benji377.timety.util.LocaleHelper
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit


/** Builds and cancels the reminder notifications for tasks, habits, and daily/evening check-ins. */
class ReminderScheduler private constructor(private val context: Context) {

    private val notificationService = NotificationService(context)

    // Tasks.


    fun scheduleTaskReminders(task: TaskEntity) {
        cancelTaskReminders(task.id)
        if (task.isCompleted) return

        val now = Instant.now()
        val baseId = task.id.hashCode()
        var slot = 0

        task.reminders.forEach { reminder ->
            if (reminder.isAfter(now) && slot < TASK_ID_SLOTS) {
                notificationService.scheduleTaskReminder(
                    notificationId = baseId + slot,
                    title = context.getString(R.string.taskReminderTitle, task.title),
                    body = buildReminderBody(task.title, task.description, reminder, dueDate = task.dueDate),
                    scheduledTime = reminder,
                )
                slot++
            }
        }

        if (task.reminders.isEmpty() && task.dueDate != null && task.dueDate.isAfter(now)) {
            notificationService.scheduleTaskReminder(
                notificationId = baseId,
                title = context.getString(R.string.taskReminderTitle, task.title),
                body = buildReminderBody(task.title, task.description, task.dueDate, exactDueDate = true),
                scheduledTime = task.dueDate,
            )
        }
    }

    fun cancelTaskReminders(taskId: String) {
        val baseId = taskId.hashCode()
        for (i in 0 until TASK_ID_SLOTS) notificationService.cancelNotification(baseId + i)
    }

    // Recurring tasks.


    /**
     * Schedules the reminders for a recurring task's *next* occurrence only; completing the
     * occurrence advances the due date and calls this again for the one after.
     */
    fun scheduleRecurringTaskReminders(task: RecurringTaskEntity) {
        cancelRecurringTaskReminders(task.id)

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


    private fun buildReminderBody(
        title: String,
        description: String,
        reminderTime: Instant,
        dueDate: Instant? = null,
        exactDueDate: Boolean = false,
    ): String {
        if (exactDueDate) {
            return context.getString(R.string.taskReminderBodyExact, title) + descriptionHint(description)
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
        return context.getString(R.string.taskReminderBody, prefix, title) + descriptionHint(description)
    }

    /** The description's first non-empty line, appended on its own line so `BigTextStyle` surfaces the "why". */
    private fun descriptionHint(description: String): String {
        val hint = description.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return if (hint.isEmpty()) "" else "\n" + hint.take(100)
    }

    // Habits.


    fun scheduleHabitReminder(habit: HabitEntity) {
        notificationService.cancelHabitReminder(habit.id)
        val totalMinutes = habit.targetTimeMinutes ?: return
        val weekdays = if (habit.frequency == HabitFrequency.WEEKLY_EXACT) {
            HabitUtils.parseWeekdays(habit.targetWeekdays).toList()
        } else {
            null
        }
        notificationService.scheduleHabitReminder(
            habitId = habit.id,
            title = context.getString(R.string.notificationHabitTitle),
            body = context.getString(R.string.notificationHabitBody, habit.name),
            hour = (totalMinutes / 60).coerceIn(0, 23),
            minute = (totalMinutes % 60).coerceIn(0, 59),
            targetWeekdays = weekdays,
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


        /** Re-schedules every task, habit, and general reminder; used after boot/app-update since the OS clears pending alarms. */
        suspend fun resyncAll(context: Context) {
            val app = context.applicationContext as? TimetyApplication ?: return
            val scheduler = create(app)
            val settings = app.container.settingsRepository

            app.container.taskRepository.allTasks.first()
                .forEach { scheduler.scheduleTaskReminders(it.task) }
            app.container.recurringTaskRepository.allRecurringTasks.first()
                .forEach { scheduler.scheduleRecurringTaskReminders(it.task) }
            app.container.habitRepository.allHabits.first()
                .forEach { scheduler.scheduleHabitReminder(it) }
            app.container.quickHabitRepository.allQuickHabits.first()
                .forEach { scheduler.scheduleQuickHabit(it) }
            scheduler.scheduleDailyMotivation(settings.dailyMotivationTimeFlow.first())
            scheduler.scheduleEndOfDayCheckup(settings.endOfDayCheckupTimeFlow.first())
        }
    }
}
