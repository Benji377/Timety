package com.github.benji377.timety.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.github.benji377.timety.receivers.ReminderBroadcastReceiver

/**
 * Manages alarm scheduling for task reminders using AlarmManager.
 */
object ReminderManager {

    fun scheduleReminder(
        context: Context,
        taskId: Int,
        taskTitle: String,
        reminderTimeMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.github.benji377.timety.TASK_REMINDER"
            putExtra("taskId", taskId)
            putExtra("taskTitle", taskTitle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTimeMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelReminder(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleMultipleReminders(
        context: Context,
        taskId: Int,
        taskTitle: String,
        reminderTimes: List<Long>
    ) {
        reminderTimes.forEachIndexed { index, reminderTime ->
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = "com.github.benji377.timety.TASK_REMINDER"
                putExtra("taskId", taskId)
                putExtra("taskTitle", taskTitle)
                putExtra("reminderIndex", index)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId + index * 10000, // Unique ID for each reminder
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

