package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.util.LocaleHelper
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.util.reminder.EveningSummary
import io.github.benji377.timety.util.reminder.EveningSummaryBuilder
import kotlinx.coroutines.flow.first
import java.time.Instant


/** Handles a fired reminder alarm by showing its notification and re-arming it if it repeats. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationService.EXTRA_NOTIFICATION_ID, -1)
        if (notificationId == -1) return

        val title = intent.getStringExtra(NotificationService.EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(NotificationService.EXTRA_BODY) ?: ""
        val channelId = intent.getStringExtra(NotificationService.EXTRA_CHANNEL_ID)
            ?: NotificationService.CHANNEL_TASKS

        val appContext = context.applicationContext
        val notificationService = NotificationService(appContext)

        if (channelId == NotificationService.CHANNEL_MOTIVATION) {
            launchAsync {
                val finalBody = motivationBody(appContext, fallback = body)
                notificationService.showNotification(notificationId, channelId, title, finalBody)
                notificationService.rescheduleIfRepeating(intent)
            }
            return
        }

        if (channelId == NotificationService.CHANNEL_EVENING) {
            launchAsync {
                val localized = localizedContext(appContext)
                val summary = eveningSummary(appContext)
                when {
                    summary == null -> notificationService.showEndOfDayNotification(
                        notificationId, title, body, localized,
                    )

                    summary.isAllDone -> notificationService.showEndOfDayNotification(
                        notificationId,
                        title,
                        localized.getString(R.string.notificationEveningAllDone),
                        localized,
                    )

                    else -> {
                        val preview = summary.preview(EVENING_MAX_LINES)
                        notificationService.showEndOfDayNotification(
                            notificationId,
                            title,
                            localized.resources.getQuantityString(
                                R.plurals.nNotificationEveningOpen, summary.total, summary.total,
                            ),
                            localized,
                            lines = preview.items.map {
                                localized.getString(
                                    if (it.isTask) R.string.notificationEveningLineTask
                                    else R.string.notificationEveningLineHabit,
                                    it.name,
                                )
                            },
                            overflowText = preview.hiddenCount.takeIf { it > 0 }?.let {
                                localized.getString(R.string.notificationEveningMore, it)
                            },
                        )
                    }
                }
                notificationService.rescheduleIfRepeating(intent)
            }
            return
        }

        val habitId = intent.getStringExtra(NotificationService.EXTRA_HABIT_ID)
        if (channelId == NotificationService.CHANNEL_HABITS && habitId != null) {
            launchAsync {
                // Null means the habit was deleted, so its alarm chain ends here.
                val needsReminder = habitNeedsReminder(appContext, habitId) ?: return@launchAsync
                if (needsReminder) {
                    notificationService.showNotification(notificationId, channelId, title, body)
                }
                notificationService.rescheduleIfRepeating(intent)
            }
            return
        }

        notificationService.showNotification(notificationId, channelId, title, body)
        notificationService.rescheduleIfRepeating(intent)
    }


    /** Whether [habitId] still has something to do today, or null if the habit no longer exists. */
    private suspend fun habitNeedsReminder(context: Context, habitId: String): Boolean? {
        val app = context.applicationContext as? TimetyApplication ?: return true
        val repository = app.container.habitRepository
        val habit = repository.getHabitById(habitId) ?: return null
        val completions = repository.getCompletionsForHabit(habitId).first()
        return HabitUtils.needsReminderToday(HabitWithCompletions(habit, completions))
    }


    /** What is still open today, or null when the app container isn't available. */
    private suspend fun eveningSummary(context: Context): EveningSummary? {
        val app = context.applicationContext as? TimetyApplication ?: return null
        val container = app.container
        val completionsByHabit = container.habitRepository.allCompletions.first()
            .groupBy { it.habitId }
        return EveningSummaryBuilder.build(
            habits = container.habitRepository.allHabits.first()
                .map { HabitWithCompletions(it, completionsByHabit[it.id].orEmpty()) },
            tasks = container.taskRepository.allTasks.first().map { it.task },
            recurringTasks = container.recurringTaskRepository.allRecurringTasks.first()
                .map { it.task },
            now = Instant.now(),
        )
    }


    /** [context] wrapped in the user's chosen app locale; the plain context if unavailable. */
    private suspend fun localizedContext(context: Context): Context {
        val app = context.applicationContext as? TimetyApplication ?: return context
        return LocaleHelper.wrap(
            app,
            app.container.settingsRepository.appLocaleCodeFlow.first()
        )
    }


    private suspend fun motivationBody(context: Context, fallback: String): String {
        val app = context.applicationContext as? TimetyApplication ?: return fallback
        val localized = localizedContext(app)
        val quotes = listOf(
            localized.getString(R.string.notificationQuote1),
            localized.getString(R.string.notificationQuote2),
            localized.getString(R.string.notificationQuote3),
        )
        val quote = quotes[java.time.LocalDate.now().dayOfMonth % quotes.size]
        return quote + todaysHabitsSuffix(app, localized)
    }


    private suspend fun todaysHabitsSuffix(app: TimetyApplication, context: Context): String {
        val habits = app.container.habitRepository.allHabits.first()
        if (habits.isEmpty()) return ""
        val completions = app.container.habitRepository.allCompletions.first()
        val completionsByHabit = completions.groupBy { it.habitId }

        val todaysHabits = habits
            .map { HabitWithCompletions(it, completionsByHabit[it.id].orEmpty()) }
            .filter { HabitUtils.isHabitDueToday(it) }
            .map { it.habit.name }
        if (todaysHabits.isEmpty()) return ""

        // List up to two habit names, appending an "and more" suffix if there are additional ones.
        var habitList = todaysHabits.take(2).joinToString(", ")
        if (todaysHabits.size > 2) {
            habitList += context.getString(R.string.notificationHabitListSuffix)
        }
        return context.getString(R.string.notificationHabitReminder, habitList)
    }

    private companion object {
        /** InboxStyle shows at most five lines; the rest collapse into a "+N more" footer. */
        const val EVENING_MAX_LINES = 5
    }
}
