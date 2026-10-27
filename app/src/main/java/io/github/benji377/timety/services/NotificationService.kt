package io.github.benji377.timety.services

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.ui.components.mood.labelRes
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.TaskColor
import io.github.benji377.timety.ui.theme.UserColor
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.habit.HabitUtils
import io.github.benji377.timety.util.habit.QuickHabitScheduling
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime


/** Creates notification channels and schedules/cancels the exact alarms that back task, habit, and general reminders. */
class NotificationService(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager


    fun ensureChannels() {
        val systemManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                CHANNEL_TASKS,
                context.getString(R.string.notificationChannelTasksName),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notificationChannelTasksDesc)
                setBypassDnd(true)
            },
            NotificationChannel(
                CHANNEL_HABITS,
                context.getString(R.string.notificationChannelHabitsName),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notificationChannelHabitsDesc)
                setBypassDnd(true)
            },
            NotificationChannel(
                CHANNEL_QUICK_HABITS,
                context.getString(R.string.notificationChannelQuickHabitsName),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notificationChannelQuickHabitsDesc)
                setBypassDnd(true)
            },
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

        // Old channel IDs, superseded by bypass-DND versions below; drop the dead duplicates.
        LEGACY_CHANNEL_IDS.forEach { systemManager.deleteNotificationChannel(it) }
    }

    // Tasks.


    fun scheduleTaskReminder(
        notificationId: Int,
        title: String,
        body: String,
        scheduledTime: Instant
    ) {
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

    // Habits.


    /**
     * Arms [habitId]'s single pending reminder at [triggerAtMillis], replacing the one armed
     * before. The receiver arms the next one when it fires, like a quick habit's chain.
     */
    fun scheduleHabitReminder(habitId: String, title: String, body: String, triggerAtMillis: Long) {
        val id = habitReminderId(habitId)
        val intent = reminderIntent(
            notificationId = id,
            title = title,
            body = body,
            channelId = CHANNEL_HABITS,
            repeat = Repeat.NONE,
            habitId = habitId,
        )
        scheduleExact(triggerAtMillis, id, intent)
    }


    fun cancelHabitReminder(habitId: String) {
        cancelAlarm(habitReminderId(habitId))
        dismissHabitNotifications(habitId)
    }

    /** Clears [habitId]'s reminder if it is showing, leaving the next one armed. */
    fun dismissHabitNotifications(habitId: String) {
        notificationManager.cancel(habitReminderId(habitId))
    }

    /**
     * Cancels the per-weekday and per-time alarms older versions armed for [habitId], and any
     * reminder they posted that is still showing.
     */
    fun cancelLegacyHabitReminders(habitId: String) {
        legacyHabitReminderIds(habitId).forEach {
            cancelAlarm(it)
            notificationManager.cancel(it)
        }
    }

    // Quick habits (interval reminders).


    /**
     * Schedules the next occurrence of a quick habit as a single exact alarm; [rescheduleIfRepeating]
     * chains the following one when it fires. Cancels any existing alarm first, so this doubles as
     * the "reschedule after edit" path; a reminder already showing stays.
     */
    fun scheduleQuickHabit(
        id: String,
        title: String,
        body: String,
        intervalMinutes: Int,
        startMinuteOfDay: Int?,
        endMinuteOfDay: Int?,
        allowedWeekdays: Set<Int>,
    ) {
        val notificationId = quickHabitId(id)
        cancelAlarm(notificationId)
        val triggerAt = QuickHabitScheduling.nextTrigger(
            now = Instant.now(),
            intervalMinutes = intervalMinutes,
            startMinuteOfDay = startMinuteOfDay,
            endMinuteOfDay = endMinuteOfDay,
            allowedWeekdays = allowedWeekdays,
        )
        val intent = reminderIntent(
            notificationId = notificationId,
            title = title,
            body = body,
            channelId = CHANNEL_QUICK_HABITS,
            repeat = Repeat.INTERVAL,
            quickHabitId = id,
            intervalMinutes = intervalMinutes,
            startMinuteOfDay = startMinuteOfDay ?: -1,
            endMinuteOfDay = endMinuteOfDay ?: -1,
            weekdaysCsv = allowedWeekdays.sorted().joinToString(","),
        )
        scheduleExact(triggerAt.toEpochMilli(), notificationId, intent)
    }

    fun cancelQuickHabit(id: String) = cancelAlarmAndNotification(quickHabitId(id))

    // Daily motivation.


    fun scheduleDailyMotivation(hour: Int, minute: Int, title: String, body: String) {
        val triggerAt = nextReminderMillis(hour, minute)
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

    // End of day checkup.


    fun scheduleEndOfDayCheckup(hour: Int, minute: Int, title: String, body: String) {
        val triggerAt = nextReminderMillis(hour, minute)
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

    // Cancel.


    fun cancelNotification(notificationId: Int) = cancelAlarmAndNotification(notificationId)

    /** Cancels only the pending alarm; a reminder already showing stays until the user handles it. */
    fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        // Without a PendingIntent there is no pending alarm, so there is nothing to create or cancel.
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
    }

    // Called by ReminderReceiver when an alarm fires.

    internal fun showNotification(id: Int, channelId: String, title: String, body: String) {
        notifySafely(id, reminderNotification(id, channelId, title, body).build())
    }

    /**
     * Shows the end-of-day checkup. Unless [withRatingActions] is false (today is already rated),
     * it carries one-tap Bad/OK/Great actions handled by [DayRatingReceiver]. The rated day is
     * captured at post time, so a tap after midnight still rates the evening's day. [localized]
     * resolves the action labels in the app locale.
     */
    internal fun showEndOfDayNotification(
        id: Int,
        title: String,
        body: String,
        localized: Context,
        lines: List<String> = emptyList(),
        overflowText: String? = null,
        withRatingActions: Boolean = true,
    ) {
        val dayKey = AppDateUtils.dayKey(LocalDate.now())
        val builder = reminderNotification(id, CHANNEL_EVENING, title, body)
        // Collapsed it shows [body] (the count); expanded it lists the open items.
        if (lines.isNotEmpty()) {
            val inbox = NotificationCompat.InboxStyle().setBigContentTitle(title)
            lines.forEach { inbox.addLine(it) }
            overflowText?.let { inbox.setSummaryText(it) }
            builder.setStyle(inbox)
        }
        if (withRatingActions) DayRating.entries.forEach { rating ->
            val actionIntent = Intent(context, DayRatingReceiver::class.java).apply {
                putExtra(DayRatingReceiver.EXTRA_DAY_KEY, dayKey)
                putExtra(DayRatingReceiver.EXTRA_RATING, rating.value)
            }
            val pendingAction = PendingIntent.getBroadcast(
                context,
                // Distinct request code per rating: extras alone don't distinguish PendingIntents.
                rating.value,
                actionIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(0, localized.getString(rating.labelRes), pendingAction)
        }
        notifySafely(id, builder.build())
    }

    private fun reminderNotification(
        id: Int,
        channelId: String,
        title: String,
        body: String,
    ): NotificationCompat.Builder {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = PendingIntent.getActivity(
            context,
            id,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .setColor(channelAccentColor(channelId))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
    }

    private fun notifySafely(id: Int, notification: Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted; the alarm still fired, just silently.
        }
    }


    private fun channelAccentColor(channelId: String): Int = when (channelId) {
        CHANNEL_TASKS -> TaskColor
        CHANNEL_HABITS, CHANNEL_QUICK_HABITS -> HabitColor
        CHANNEL_FOCUS, CHANNEL_MOTIVATION -> FocusColor
        else -> UserColor
    }.toArgb()

    /** Re-arms a fired alarm for its next occurrence if it was scheduled as daily/weekly/interval repeating. */
    internal fun rescheduleIfRepeating(intent: Intent) {
        val repeat = Repeat.fromExtra(intent.getStringExtra(EXTRA_REPEAT))
        if (repeat == Repeat.NONE) return
        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (repeat == Repeat.INTERVAL) {
            val weekdays = intent.getStringExtra(EXTRA_WEEKDAYS)
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                .orEmpty()
            val triggerAt = QuickHabitScheduling.nextTrigger(
                now = Instant.now(),
                intervalMinutes = intent.getIntExtra(EXTRA_INTERVAL, 60),
                startMinuteOfDay = intent.getIntExtra(EXTRA_START_MIN, -1).takeIf { it >= 0 },
                endMinuteOfDay = intent.getIntExtra(EXTRA_END_MIN, -1).takeIf { it >= 0 },
                allowedWeekdays = weekdays,
            )
            scheduleExact(triggerAt.toEpochMilli(), id, intent)
            return
        }
        val hour = intent.getIntExtra(EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val triggerAt = nextReminderMillis(hour, minute, forceRollover = true)
        scheduleExact(triggerAt, id, intent)
    }

    // Internals.

    private fun scheduleExact(triggerAtMillis: Long, requestCode: Int, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canScheduleExact =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canScheduleExact) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                    return
                } catch (e: SecurityException) {
                    Log.w(TAG, "Exact alarm denied for $requestCode; falling back to inexact", e)
                }
            }
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: SecurityException) {
            Log.e(TAG, "Alarm denied; dropping reminder $requestCode", e)
        } catch (e: IllegalStateException) {
            // AlarmManager hard-caps pending alarms per app (~500). With enough reminder-bearing
            // tasks/habits the cap is reachable; dropping this one reminder beats crashing.
            Log.e(TAG, "Alarm limit reached; dropping reminder $requestCode", e)
        }
    }

    private fun cancelAlarmAndNotification(requestCode: Int) {
        cancelAlarm(requestCode)
        notificationManager.cancel(requestCode)
    }

    private fun reminderIntent(
        notificationId: Int,
        title: String,
        body: String,
        channelId: String,
        repeat: Repeat,
        habitId: String? = null,
        quickHabitId: String? = null,
        hour: Int = -1,
        minute: Int = -1,
        intervalMinutes: Int = -1,
        startMinuteOfDay: Int = -1,
        endMinuteOfDay: Int = -1,
        weekdaysCsv: String = "",
    ): Intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_BODY, body)
        putExtra(EXTRA_CHANNEL_ID, channelId)
        putExtra(EXTRA_REPEAT, repeat.name)
        habitId?.let { putExtra(EXTRA_HABIT_ID, it) }
        quickHabitId?.let { putExtra(EXTRA_QUICK_HABIT_ID, it) }
        putExtra(EXTRA_HOUR, hour)
        putExtra(EXTRA_MINUTE, minute)
        putExtra(EXTRA_INTERVAL, intervalMinutes)
        putExtra(EXTRA_START_MIN, startMinuteOfDay)
        putExtra(EXTRA_END_MIN, endMinuteOfDay)
        putExtra(EXTRA_WEEKDAYS, weekdaysCsv)
    }


    private fun nextReminderMillis(
        hour: Int,
        minute: Int,
        forceRollover: Boolean = false
    ): Long {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var scheduled = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        // Roll over to tomorrow if the computed time has already passed. A just-fired daily alarm
        // ([forceRollover]) also rolls over when the recomputed time is within the next minute, so
        // minor clock skew can't re-arm the same occurrence.
        val rolloverThreshold = if (forceRollover) now.plusMinutes(1) else now
        if (!scheduled.isAfter(rolloverThreshold)) {
            scheduled = scheduled.plusDays(1)
        }
        return scheduled.toInstant().toEpochMilli()
    }

    private enum class Repeat {
        NONE, DAILY, INTERVAL;

        companion object {
            fun fromExtra(value: String?): Repeat = entries.find { it.name == value } ?: NONE
        }
    }

    companion object {
        private const val TAG = "NotificationService"

        // "_v2": channel flags are immutable after creation, so bypass-DND needs a new channel ID.
        const val CHANNEL_TASKS = "task_reminders_channel_v2"
        const val CHANNEL_HABITS = "habit_reminders_channel_v2"
        const val CHANNEL_QUICK_HABITS = "quick_habit_reminders_channel_v2"
        const val CHANNEL_MOTIVATION = "daily_motivation_channel"
        const val CHANNEL_EVENING = "evening_checkup_channel"
        const val CHANNEL_FOCUS = "focus_timer_channel"

        private val LEGACY_CHANNEL_IDS = listOf(
            "task_reminders_channel",
            "habit_reminders_channel",
            "quick_habit_reminders_channel",
        )

        // Reserved IDs; kept stable to match legacy app versions so existing scheduled alarms still resolve.
        const val DAILY_MOTIVATION_ID = 9999
        const val END_OF_DAY_CHECKUP_ID = 9998

        internal const val EXTRA_NOTIFICATION_ID = "notificationId"
        internal const val EXTRA_TITLE = "title"
        internal const val EXTRA_BODY = "body"
        internal const val EXTRA_CHANNEL_ID = "channelId"
        internal const val EXTRA_HABIT_ID = "habitId"
        internal const val EXTRA_QUICK_HABIT_ID = "quickHabitId"
        internal const val EXTRA_HOUR = "hour"
        internal const val EXTRA_MINUTE = "minute"
        internal const val EXTRA_REPEAT = "repeat"
        internal const val EXTRA_INTERVAL = "intervalMinutes"
        internal const val EXTRA_START_MIN = "startMinuteOfDay"
        internal const val EXTRA_END_MIN = "endMinuteOfDay"
        internal const val EXTRA_WEEKDAYS = "allowedWeekdays"


        /** Deterministic notification/request-code ID of a habit's single pending reminder. */
        fun habitReminderId(habitId: String): Int = legacyHabitReminderId(habitId, null, 0)

        /** IDs older versions used for a habit's per-weekday and per-time alarms, other than [habitReminderId]. */
        private fun legacyHabitReminderIds(habitId: String): List<Int> =
            (0 until HabitUtils.MAX_REMINDER_TIMES).flatMap { slot ->
                listOf<Int?>(null).plus(1..7).map { weekday -> legacyHabitReminderId(habitId, weekday, slot) }
            }.filter { it != habitReminderId(habitId) }

        private fun legacyHabitReminderId(habitId: String, weekday: Int?, slot: Int): Int {
            val suffix = if (weekday == null) "daily" else "weekday_$weekday"
            val slotSuffix = if (slot == 0) "" else "_slot_$slot"
            return "habit_time_${habitId}_$suffix$slotSuffix".hashCode()
        }

        /**
         * Deterministic notification/request-code ID for a quick habit. XOR-masked so it can never
         * collide with a task reminder's slots (`task.id.hashCode() + 0..10`) even for equal ids.
         */
        fun quickHabitId(id: String): Int = id.hashCode() xor QUICK_HABIT_ID_MASK

        /**
         * Deterministic base ID for a recurring task's reminder slots (`base + 0..10`). XOR-masked
         * so it can never collide with the plain-task or quick-habit namespaces for equal ids.
         */
        fun recurringTaskReminderBaseId(id: String): Int = id.hashCode() xor RECURRING_TASK_ID_MASK

        private const val QUICK_HABIT_ID_MASK = 0x5175_4842 // "QuHB"
        private const val RECURRING_TASK_ID_MASK = 0x5265_5461 // "ReTa"
    }
}
