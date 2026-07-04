package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires when a [NotificationService]-scheduled `AlarmManager` alarm goes off: shows the
 * notification carried in the intent's extras, then - if the reminder is daily/weekly repeating -
 * re-arms the next occurrence. This replaces the implicit recurrence Flutter gets for free from
 * `flutter_local_notifications`' `matchDateTimeComponents` (there is no native `AlarmManager`
 * equivalent, so the receiver re-schedules itself on every fire instead).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationService.EXTRA_NOTIFICATION_ID, -1)
        if (notificationId == -1) return

        val title = intent.getStringExtra(NotificationService.EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(NotificationService.EXTRA_BODY) ?: ""
        val channelId = intent.getStringExtra(NotificationService.EXTRA_CHANNEL_ID)
            ?: NotificationService.CHANNEL_TASKS

        val notificationService = NotificationService(context.applicationContext)

        if (channelId == NotificationService.CHANNEL_MOTIVATION) {
            val pendingResult = goAsync()
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    val container =
                        (context.applicationContext as io.github.benji377.timety.TimetyApplication).container
                    val habits = container.habitRepository.allHabits.first()
                    val completions = container.habitRepository.allCompletions.first()

                    var finalBody = body
                    if (habits.isNotEmpty()) {
                        val habitWithLowestCompletion = habits.minByOrNull { habit ->
                            val comps = completions.filter { it.habitId == habit.id }
                            io.github.benji377.timety.util.habit.HabitUtils.getCompletionsThisWeek(
                                io.github.benji377.timety.data.model.habit.HabitWithCompletions(
                                    habit,
                                    comps
                                )
                            )
                        }
                        if (habitWithLowestCompletion != null) {
                            finalBody =
                                "Don't forget to work on '${habitWithLowestCompletion.name}' today!"
                        }
                    }

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
}
