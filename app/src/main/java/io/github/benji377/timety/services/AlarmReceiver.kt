package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.habit.HabitFrequency
import io.github.benji377.timety.util.habit.HabitUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-arms the app's repeating notifications after a device reboot. `AlarmManager` exact alarms
 * (unlike Flutter's `flutter_local_notifications`, which re-registers its own alarms via its own
 * boot receiver internally) do not survive a reboot, so without this receiver the daily
 * motivation / end-of-day checkup / habit-time reminders would silently stop firing until the
 * next time something happened to reschedule them.
 *
 * One-shot task reminders are intentionally NOT re-armed here (they are naturally re-scheduled
 * whenever their owning task is next created/edited - the same as everywhere else in this
 * codebase, wiring `TaskViewModel`/`HabitViewModel` to actually call [NotificationService] on
 * save is a follow-up, see report).
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        val app = appContext as? TimetyApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                rescheduleRecurring(appContext, app)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleRecurring(context: Context, app: TimetyApplication) {
        val notificationService = NotificationService(context)
        notificationService.ensureChannels()

        val settings = app.container.settingsRepository

        val (motivationHour, motivationMinute) = parseTime(settings.dailyMotivationTimeFlow.first())
        val quotes = listOf(
            context.getString(R.string.notificationQuote1),
            context.getString(R.string.notificationQuote2),
            context.getString(R.string.notificationQuote3),
        )
        val dayOfMonth = java.time.LocalDate.now().dayOfMonth
        notificationService.scheduleDailyMotivation(
            hour = motivationHour,
            minute = motivationMinute,
            title = context.getString(R.string.notificationGoodMorning),
            body = quotes[dayOfMonth % quotes.size],
        )

        val (eveningHour, eveningMinute) = parseTime(settings.endOfDayCheckupTimeFlow.first())
        notificationService.scheduleEndOfDayCheckup(
            hour = eveningHour,
            minute = eveningMinute,
            title = context.getString(R.string.notificationEveningTitle),
            body = context.getString(R.string.notificationEveningBody),
        )

        val habits = app.container.habitRepository.allHabits.first()
        for (habit in habits) {
            val totalMinutes = habit.targetTimeMinutes ?: continue
            val hour = (totalMinutes / 60).coerceIn(0, 23)
            val minute = (totalMinutes % 60).coerceIn(0, 59)
            val weekdays = if (habit.frequency == HabitFrequency.WEEKLY_EXACT) {
                HabitUtils.parseWeekdays(habit.targetWeekdays).toList()
            } else {
                null
            }
            notificationService.scheduleHabitReminder(
                habitId = habit.id,
                title = context.getString(R.string.notificationHabitTitle),
                body = context.getString(R.string.notificationHabitBody, habit.name),
                hour = hour,
                minute = minute,
                targetWeekdays = weekdays,
            )
        }
    }

    /** Parses a stored "HH:mm" [SettingsRepository] time string; falls back to 00:00 if malformed. */
    private fun parseTime(raw: String): Pair<Int, Int> {
        val parts = raw.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return hour to minute
    }
}
