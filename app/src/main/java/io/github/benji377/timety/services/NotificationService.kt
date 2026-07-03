package io.github.benji377.timety.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules and shows the app's non-focus-timer notifications (task/habit reminders, daily
 * motivation, end-of-day checkup). Mirrors `services/notification_service.dart`, swapping
 * `flutter_local_notifications`' `zonedSchedule` for a plain `AlarmManager` exact alarm +
 * [ReminderReceiver] that renders the notification when the alarm fires (and re-arms itself for
 * repeating reminders, since `AlarmManager` - unlike the Dart plugin's OS-level scheduler on
 * Android - does not support `matchDateTimeComponents`-style implicit recurrence).
 *
 * The focus-timer's own persistent notification is intentionally NOT handled here: it has a
 * completely different lifecycle (bound to the [FocusTimerService] foreground service, updated
 * from live [FocusTimerManager] state rather than a one-shot alarm) and is built directly by
 * [FocusTimerService] instead - see that class's doc. [ensureChannels] still creates its channel
 * so channel setup has one call site.
 */
class NotificationService(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Creates all notification channels used by the app. Safe to call repeatedly (idempotent). */
    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                CHANNEL_TASKS,
                context.getString(R.string.notificationChannelTasksName),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.notificationChannelTasksDesc) },
            NotificationChannel(
                CHANNEL_HABITS,
                context.getString(R.string.notificationChannelHabitsName),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.notificationChannelHabitsDesc) },
            NotificationChannel(
                CHANNEL_MOTIVATION,
                context.getString(R.string.notificationChannelMotivationName),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notificationChannelMotivationDesc) },
            NotificationChannel(
                CHANNEL_EVENING,
                context.getString(R.string.notificationChannelEveningName),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notificationChannelEveningDesc) },
            NotificationChannel(
                CHANNEL_FOCUS,
                context.getString(R.string.notificationChannelFocusName),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notificationChannelFocusDesc)
                setSound(null, null)
            },
        )
        channels.forEach { systemManager.createNotificationChannel(it) }
    }

    // --- TASKS ---

    /** One-shot reminder for a specific task. Mirrors `scheduleTaskReminder`. */
    fun scheduleTaskReminder(notificationId: Int, title: String, body: String, scheduledTime: Instant) {
        if (scheduledTime.isBefore(Instant.now())) return
        val intent = reminderIntent(
            notificationId = notificationId,
            title = title,
            body = body,
            channelId = CHANNEL_TASKS,
            repeat = Repeat.NONE,
        )
        scheduleExact(scheduledTime.toEpochMilli(), notificationId, intent)
    }

    // --- HABITS ---

    /**
     * Schedules a habit reminder at [hour]:[minute]. If [targetWeekdays] is null/empty, fires
     * daily; otherwise fires once per listed ISO weekday (1=Mon..7=Sun). Mirrors
     * `scheduleHabitReminder`.
     */
    fun scheduleHabitReminder(
        habitId: String,
        title: String,
        body: String,
        hour: Int,
        minute: Int,
        targetWeekdays: List<Int>? = null,
    ) {
        cancelHabitReminder(habitId)
        val weekdays = targetWeekdays?.toSortedSet()?.toList()
        val reminderDays: List<Int?> = if (weekdays.isNullOrEmpty()) listOf(null) else weekdays

        for (weekday in reminderDays) {
            val id = habitReminderId(habitId, weekday)
            val triggerAt = nextReminderMillis(hour, minute, weekday)
            val intent = reminderIntent(
                notificationId = id,
                title = title,
                body = body,
                channelId = CHANNEL_HABITS,
                repeat = if (weekday == null) Repeat.DAILY else Repeat.WEEKLY,
                habitId = habitId,
                weekday = weekday ?: -1,
                hour = hour,
                minute = minute,
            )
            scheduleExact(triggerAt, id, intent)
        }
    }

    /** Cancels every reminder (daily + all 7 weekday variants) for [habitId]. Mirrors `cancelHabitReminder`. */
    fun cancelHabitReminder(habitId: String) {
        cancelAlarmAndNotification(habitReminderId(habitId, null))
        for (weekday in 1..7) cancelAlarmAndNotification(habitReminderId(habitId, weekday))
    }

    // --- DAILY MOTIVATION ---

    /** Mirrors `scheduleDailyMotivation` (quote selection is the caller's responsibility here,
     * since it lives behind localized strings the caller already resolved). */
    fun scheduleDailyMotivation(hour: Int, minute: Int, title: String, body: String) {
        val triggerAt = nextReminderMillis(hour, minute, null)
        val intent = reminderIntent(
            notificationId = DAILY_MOTIVATION_ID,
            title = title,
            body = body,
            channelId = CHANNEL_MOTIVATION,
            repeat = Repeat.DAILY,
            hour = hour,
            minute = minute,
        )
        scheduleExact(triggerAt, DAILY_MOTIVATION_ID, intent)
    }

    // --- END OF DAY CHECKUP ---

    /** Mirrors `scheduleEndOfDayCheckup`. */
    fun scheduleEndOfDayCheckup(hour: Int, minute: Int, title: String, body: String) {
        val triggerAt = nextReminderMillis(hour, minute, null)
        val intent = reminderIntent(
            notificationId = END_OF_DAY_CHECKUP_ID,
            title = title,
            body = body,
            channelId = CHANNEL_EVENING,
            repeat = Repeat.DAILY,
            hour = hour,
            minute = minute,
        )
        scheduleExact(triggerAt, END_OF_DAY_CHECKUP_ID, intent)
    }

    // --- CANCEL ---

    /** Mirrors `cancelNotification`. */
    fun cancelNotification(notificationId: Int) = cancelAlarmAndNotification(notificationId)

    // --- Called by ReminderReceiver when an alarm fires ---

    internal fun showNotification(id: Int, channelId: String, title: String, body: String) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context, id, contentIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            notificationManager.notify(id, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted; the alarm still fired, just silently.
        }
    }

    internal fun rescheduleIfRepeating(intent: Intent) {
        val repeat = Repeat.fromExtra(intent.getStringExtra(EXTRA_REPEAT))
        if (repeat == Repeat.NONE) return
        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val hour = intent.getIntExtra(EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val weekday = if (repeat == Repeat.WEEKLY) intent.getIntExtra(EXTRA_WEEKDAY, -1) else null
        val triggerAt = nextReminderMillis(hour, minute, weekday, forceRollover = true)
        scheduleExact(triggerAt, id, intent)
    }

    // --- internals ---

    private fun scheduleExact(triggerAtMillis: Long, requestCode: Int, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarmAndNotification(requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        notificationManager.cancel(requestCode)
    }

    private fun reminderIntent(
        notificationId: Int,
        title: String,
        body: String,
        channelId: String,
        repeat: Repeat,
        habitId: String? = null,
        weekday: Int = -1,
        hour: Int = -1,
        minute: Int = -1,
    ): Intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_BODY, body)
        putExtra(EXTRA_CHANNEL_ID, channelId)
        putExtra(EXTRA_REPEAT, repeat.name)
        habitId?.let { putExtra(EXTRA_HABIT_ID, it) }
        putExtra(EXTRA_WEEKDAY, weekday)
        putExtra(EXTRA_HOUR, hour)
        putExtra(EXTRA_MINUTE, minute)
    }

    /**
     * Next epoch-millis occurrence of [hour]:[minute], either daily ([weekday] null) or on the
     * given ISO [weekday] (1=Mon..7=Sun). Mirrors `NotificationService._nextReminderDate`. When
     * [forceRollover] is true (used to re-arm a just-fired repeating reminder) the computed time
     * is always advanced past "now" by a full cycle, matching the alarm having just fired at
     * (approximately) that exact instant.
     */
    private fun nextReminderMillis(hour: Int, minute: Int, weekday: Int?, forceRollover: Boolean = false): Long {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var scheduled = if (weekday == null) {
            now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        } else {
            val target = DayOfWeek.of(weekday)
            val daysUntil = ((target.value - now.dayOfWeek.value) + 7) % 7
            now.plusDays(daysUntil.toLong()).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        }
        if (forceRollover || scheduled.isBefore(now) || !scheduled.isAfter(now)) {
            scheduled = if (weekday == null) scheduled.plusDays(1) else scheduled.plusDays(7)
        }
        return scheduled.toInstant().toEpochMilli()
    }

    private enum class Repeat {
        NONE, DAILY, WEEKLY;

        companion object {
            fun fromExtra(value: String?): Repeat = entries.find { it.name == value } ?: NONE
        }
    }

    companion object {
        const val CHANNEL_TASKS = "task_reminders_channel"
        const val CHANNEL_HABITS = "habit_reminders_channel"
        const val CHANNEL_MOTIVATION = "daily_motivation_channel"
        const val CHANNEL_EVENING = "evening_checkup_channel"
        const val CHANNEL_FOCUS = "focus_timer_channel"

        // Reserved IDs, matching the Flutter reference exactly (`NotificationService` constants).
        const val DAILY_MOTIVATION_ID = 9999
        const val END_OF_DAY_CHECKUP_ID = 9998
        const val FOCUS_TIMER_ID = 9997 // Owned by FocusTimerService; kept here for documentation.

        internal const val EXTRA_NOTIFICATION_ID = "notificationId"
        internal const val EXTRA_TITLE = "title"
        internal const val EXTRA_BODY = "body"
        internal const val EXTRA_CHANNEL_ID = "channelId"
        internal const val EXTRA_HABIT_ID = "habitId"
        internal const val EXTRA_WEEKDAY = "weekday"
        internal const val EXTRA_HOUR = "hour"
        internal const val EXTRA_MINUTE = "minute"
        internal const val EXTRA_REPEAT = "repeat"

        /** Predictable per-habit alarm/notification id. Mirrors `_habitReminderId`. */
        fun habitReminderId(habitId: String, weekday: Int?): Int {
            val suffix = if (weekday == null) "daily" else "weekday_$weekday"
            return "habit_time_${habitId}_$suffix".hashCode()
        }
    }
}
