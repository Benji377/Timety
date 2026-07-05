package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.benji377.timety.TimetyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in REARM_ACTIONS) return

        val app = context.applicationContext as? TimetyApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                rescheduleRecurring(app)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleRecurring(app: TimetyApplication) {
        NotificationService(app).ensureChannels()

        val scheduler = ReminderScheduler.create(app)
        val settings = app.container.settingsRepository

        scheduler.scheduleDailyMotivation(settings.dailyMotivationTimeFlow.first())
        scheduler.scheduleEndOfDayCheckup(settings.endOfDayCheckupTimeFlow.first())

        app.container.habitRepository.allHabits.first().forEach { habit ->
            scheduler.scheduleHabitReminder(habit)
        }
    }

    private companion object {
        val REARM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
        )
    }
}
