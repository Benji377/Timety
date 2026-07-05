package io.github.benji377.timety.services

import android.content.Context
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.habit.HabitEntity
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.data.model.task.TaskEntity
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.dataStore
import io.github.benji377.timety.util.LocaleHelper
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.LocalDate


class ReminderScheduler private constructor(private val context: Context) {

    private val notificationService = NotificationService(context)

    // --- TASKS ---


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
                    body = buildReminderBody(task, reminder, dueDate = task.dueDate),
                    scheduledTime = reminder,
                )
                slot++
            }
        }

        if (task.reminders.isEmpty() && task.dueDate != null && task.dueDate.isAfter(now)) {
            notificationService.scheduleTaskReminder(
                notificationId = baseId,
                title = context.getString(R.string.taskReminderTitle, task.title),
                body = buildReminderBody(task, task.dueDate, exactDueDate = true),
                scheduledTime = task.dueDate,
            )
        }
    }

    fun cancelTaskReminders(taskId: String) {
        val baseId = taskId.hashCode()
        for (i in 0 until TASK_ID_SLOTS) notificationService.cancelNotification(baseId + i)
    }


    private fun buildReminderBody(
        task: TaskEntity,
        reminderTime: Instant,
        dueDate: Instant? = null,
        exactDueDate: Boolean = false,
    ): String {
        if (exactDueDate) return context.getString(R.string.taskReminderBodyExact, task.title)

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
        return context.getString(R.string.taskReminderBody, prefix, task.title)
    }

    // --- HABITS ---


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

    // --- DAILY MOTIVATION / EVENING CHECKUP ---


    fun scheduleDailyMotivation(time: String) {
        val (hour, minute) = parseTime(time)
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
        val (hour, minute) = parseTime(time)
        notificationService.scheduleEndOfDayCheckup(
            hour = hour,
            minute = minute,
            title = context.getString(R.string.notificationEveningTitle),
            body = context.getString(R.string.notificationEveningBody),
        )
    }


    private fun parseTime(raw: String): Pair<Int, Int> {
        val parts = raw.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return hour to minute
    }

    companion object {

        private const val TASK_ID_SLOTS = 11


        suspend fun create(context: Context): ReminderScheduler {
            val appContext = context.applicationContext
            val code = SettingsRepository(appContext.dataStore).appLocaleCodeFlow.first()
            return ReminderScheduler(LocaleHelper.wrap(appContext, code))
        }
    }
}
