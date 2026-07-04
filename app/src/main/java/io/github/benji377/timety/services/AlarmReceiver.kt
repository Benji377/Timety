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

/**
 * Re-arms the app's repeating notifications after a device reboot or app update. `AlarmManager`
 * exact alarms (unlike Flutter's `flutter_local_notifications`, which re-registers its own alarms
 * via its own boot receiver internally) do not survive a reboot, so without this receiver the
 * daily motivation / end-of-day checkup / habit-time reminders would silently stop firing until
 * the next time something happened to reschedule them.
 *
 * One-shot task reminders are intentionally NOT re-armed here: they are re-scheduled by
 * `TaskViewModel` whenever their owning task is created/edited.
 */
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
