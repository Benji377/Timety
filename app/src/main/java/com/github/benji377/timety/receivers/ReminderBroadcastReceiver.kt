package com.github.benji377.timety.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.benji377.timety.utils.NotificationHelper

/**
 * Receives broadcast intents when task reminders trigger from AlarmManager.
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val taskTitle = intent.getStringExtra("taskTitle") ?: "Task Reminder"

        // Ensure notification channels are created
        NotificationHelper.createNotificationChannels(context)

        // Show notification
        NotificationHelper.showTaskReminderNotification(context, taskTitle)
    }
}

