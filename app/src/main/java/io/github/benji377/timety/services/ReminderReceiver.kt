package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.habit.HabitWithCompletions
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


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
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val finalBody = body + todaysHabitsSuffix(appContext)
                    notificationService.showNotification(
                        notificationId,
                        channelId,
                        title,
                        finalBody
                    )
                    notificationService.rescheduleIfRepeating(intent)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        notificationService.showNotification(notificationId, channelId, title, body)
        notificationService.rescheduleIfRepeating(intent)
    }


    private suspend fun todaysHabitsSuffix(context: Context): String {
        val app = context as? TimetyApplication ?: return ""
        val habits = app.container.habitRepository.allHabits.first()
        if (habits.isEmpty()) return ""
        val completions = app.container.habitRepository.allCompletions.first()
        val completionsByHabit = completions.groupBy { it.habitId }

        val todaysHabits = habits
            .map { HabitWithCompletions(it, completionsByHabit[it.id].orEmpty()) }
            .filter { HabitUtils.isHabitDueToday(it) }
            .map { it.habit.name }
        if (todaysHabits.isEmpty()) return ""

        var habitList = todaysHabits.take(2).joinToString(", ")
        if (todaysHabits.size > 2) {
            habitList += context.getString(R.string.notificationHabitListSuffix)
        }
        return context.getString(R.string.notificationHabitReminder, habitList)
    }
}
