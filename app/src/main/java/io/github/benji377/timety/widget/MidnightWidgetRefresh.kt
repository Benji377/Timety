package io.github.benji377.timety.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import io.github.benji377.timety.services.AlarmReceiver
import java.time.LocalDate
import java.time.ZoneId

/**
 * Refreshes both widgets just after local midnight, when "today" changes under them. The alarm
 * doesn't wake the device: a sleeping phone shows the widgets once it wakes anyway.
 */
object MidnightWidgetRefresh {
    const val ACTION = "io.github.benji377.timety.action.MIDNIGHT_WIDGET_REFRESH"
    private const val REQUEST_CODE = 9997
    private const val SECONDS_PAST_MIDNIGHT = 30L
    private const val WINDOW_MILLIS = 10 * 60 * 1000L

    fun schedule(context: Context) {
        val triggerAt = LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .plusSeconds(SECONDS_PAST_MIDNIGHT)
            .toInstant().toEpochMilli()
        // A 10-minute window (the system minimum) instead of set(), which may be deferred an hour. RTC
        // doesn't wake the device: the widgets only need to be right once the screen is on.
        context.getSystemService(AlarmManager::class.java)
            ?.setWindow(AlarmManager.RTC, triggerAt, WINDOW_MILLIS, pendingIntent(context))
    }

    suspend fun refreshWidgets(context: Context) {
        TaskWidget().updateAll(context)
        HabitWidget().updateAll(context)
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
