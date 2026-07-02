package io.github.benji377.timety.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

import android.widget.RemoteViews
import android.os.SystemClock
import android.app.AlarmManager
import androidx.core.app.NotificationManagerCompat

class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Observe the timer state and update the notification
        FocusTimerManager.timerState.onEach { state ->
            if (state.isRunning || state.isPaused) {
                updateNotification(state)
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
                cancelAlarm()
                stopSelf()
            }
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val state = FocusTimerManager.timerState.value
                startForeground(NOTIFICATION_ID, buildNotification(state))
                FocusTimerManager.startTimer()
            }
            ACTION_PAUSE -> FocusTimerManager.pauseTimer()
            ACTION_STOP -> {
                FocusTimerManager.stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                cancelAlarm()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows the active focus session timer"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(state: TimerState) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
        
        // Handle alarm for sound if running
        if (state.isRunning && !state.isPaused && state.secondsRemaining > 0) {
            scheduleAlarm(state.secondsRemaining)
        } else {
            cancelAlarm()
        }
    }

    private fun scheduleAlarm(secondsRemaining: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val soundIntent = Intent(this, TimerSoundReceiver::class.java)
        val soundPendingIntent = PendingIntent.getBroadcast(
            this, 9998, soundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val targetTimeMs = System.currentTimeMillis() + (secondsRemaining * 1000L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMs, soundPendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetTimeMs, soundPendingIntent)
        }
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val soundIntent = Intent(this, TimerSoundReceiver::class.java)
        val soundPendingIntent = PendingIntent.getBroadcast(
            this, 9998, soundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(soundPendingIntent)
    }

    private fun buildNotification(state: TimerState): Notification {
        val title = when {
            state.isPaused -> "Paused - ${state.modeName}"
            state.isRestPhase -> "Resting - ${state.modeName}"
            else -> "Focusing - ${state.modeName}"
        }

        val customView = RemoteViews(packageName, R.layout.custom_timer_notification)
        customView.setTextViewText(R.id.notification_title, title)
        customView.setTextViewText(R.id.notification_body, state.phaseName)

        if (state.isPaused) {
            customView.setViewVisibility(R.id.notification_chronometer, android.view.View.GONE)
            customView.setViewVisibility(R.id.notification_paused_text, android.view.View.VISIBLE)
        } else {
            customView.setViewVisibility(R.id.notification_chronometer, android.view.View.VISIBLE)
            customView.setViewVisibility(R.id.notification_paused_text, android.view.View.GONE)

            val elapsedRealtime = SystemClock.elapsedRealtime()
            val baseTime = elapsedRealtime + (state.secondsRemaining * 1000L)
            
            customView.setChronometer(R.id.notification_chronometer, baseTime, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                customView.setChronometerCountDown(R.id.notification_chronometer, true)
            }
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Add actions
        val pauseResumeIntent = Intent(this, FocusTimerService::class.java).apply {
            action = if (state.isRunning) ACTION_PAUSE else ACTION_START
        }
        val pendingPauseResumeIntent = PendingIntent.getService(
            this, 1, pauseResumeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingContentIntent)
            .addAction(
                if (state.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (state.isRunning) "Pause" else "Resume",
                pendingPauseResumeIntent
            )
            .addAction(android.R.drawable.ic_delete, "Stop", pendingStopIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
    }
}
