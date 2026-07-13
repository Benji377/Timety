package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.benji377.timety.TimetyApplication


/**
 * Re-arms scheduled reminders after a device reboot or app update, since the OS clears all
 * pending alarms in both cases.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in REARM_ACTIONS) return

        val app = context.applicationContext as? TimetyApplication ?: return
        launchAsync {
            try {
                rescheduleRecurring(app)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun rescheduleRecurring(app: TimetyApplication) {
        NotificationService(app).ensureChannels()
        // Includes task reminders: one-shot exact alarms are wiped by a reboot or app
        // update just like the repeating ones.
        ReminderScheduler.resyncAll(app)
    }

    private companion object {
        val REARM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
        )
    }
}
