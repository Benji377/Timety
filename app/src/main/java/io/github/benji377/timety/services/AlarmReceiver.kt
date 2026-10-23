package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.widget.MidnightWidgetRefresh


/**
 * Re-arms scheduled reminders after a reboot or app update (the OS clears pending alarms), a time
 * zone change (alarms are absolute instants) or newly granted exact-alarm access; also runs the
 * midnight widget refresh.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TimetyApplication ?: return
        if (intent.action == MidnightWidgetRefresh.ACTION) {
            launchAsync {
                MidnightWidgetRefresh.refreshWidgets(app)
                MidnightWidgetRefresh.schedule(app)
            }
            return
        }
        if (intent.action !in REARM_ACTIONS) return

        launchAsync {
            try {
                rescheduleRecurring(app)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-arm alarms after ${intent.action}", e)
            }
        }
    }

    private suspend fun rescheduleRecurring(app: TimetyApplication) {
        NotificationService(app).ensureChannels()
        // Includes task reminders: one-shot exact alarms are wiped by a reboot or app
        // update just like the repeating ones.
        ReminderScheduler.resyncAll(app)
        // A new time zone can move "today" for the widgets as well.
        MidnightWidgetRefresh.refreshWidgets(app)
    }

    private companion object {
        const val TAG = "AlarmReceiver"

        val REARM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_TIMEZONE_CHANGED,
            // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED, API 31+.
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
