package io.github.benji377.timety.services

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.benji377.timety.MainActivity
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.ErrorColor
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.WarningColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val alarmManager by lazy { getSystemService(ALARM_SERVICE) as AlarmManager }

    override fun onCreate() {
        super.onCreate()
        NotificationService(this).ensureChannels()

        // Only re-render/re-schedule on meaningful state transitions (mirrors Flutter, which only
        // calls `_updateNotification` at transition points, never once per tick) - re-notifying and
        // re-arming the exact alarm every second was wasteful and unnecessary since the chronometer
        // counts down on its own once set.
        FocusTimerManager.timerState
            .distinctUntilChanged { old, new ->
                old.isRunning == new.isRunning &&
                        old.isPaused == new.isPaused &&
                        old.isAwaitingContinue == new.isAwaitingContinue &&
                        old.isRestPhase == new.isRestPhase &&
                        old.modeName == new.modeName &&
                        old.totalPhaseSeconds == new.totalPhaseSeconds
            }
            .onEach { state ->
                if (state.isRunning || state.isPaused || state.isAwaitingContinue) {
                    updateNotification(state)
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (val action = intent?.action) {
            ACTION_START -> {
                val state = FocusTimerManager.timerState.value
                startForeground(NOTIFICATION_ID, buildNotification(state))
                FocusTimerManager.startTimer()
            }

            ACTION_PAUSE -> {
                FocusTimerManager.pauseTimer()
                cancelAlarm()
            }
            ACTION_STOP, ACTION_DISCARD -> {
                FocusTimerManager.stopTimer(discard = action == ACTION_DISCARD)
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

    private fun updateNotification(state: TimerState) {
        try {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
        } catch (e: SecurityException) {
            // Notification permission not granted; the foreground service keeps running silently.
        }

        // A single exact alarm per phase, targeting the phase's absolute end time - mirrors
        // `showCustomNotification`'s `targetTimeMs`-based scheduling (not re-armed every tick).
        if (state.isRunning && !state.isPaused && !state.isStopwatch && state.secondsRemaining > 0) {
            scheduleAlarm(state.secondsRemaining)
        }
    }

    private fun scheduleAlarm(secondsRemaining: Int) {
        val soundIntent = Intent(this, TimerSoundReceiver::class.java)
        val soundPendingIntent = PendingIntent.getBroadcast(
            this, ALARM_REQUEST_CODE, soundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val targetTimeMs = System.currentTimeMillis() + (secondsRemaining * 1000L)

        // Same guard as NotificationService.scheduleExact: exact-alarm access can be revoked
        // on Android 12+, and the phase-end chime is not worth a SecurityException crash.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetTimeMs,
                soundPendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetTimeMs,
                soundPendingIntent
            )
        }
    }

    private fun cancelAlarm() {
        val soundIntent = Intent(this, TimerSoundReceiver::class.java)
        val soundPendingIntent = PendingIntent.getBroadcast(
            this, ALARM_REQUEST_CODE, soundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(soundPendingIntent)
    }

    private fun buildNotification(state: TimerState): Notification {
        // "Static" rendering (paused readout, frozen accent) covers both a real pause and the
        // "ready to continue" gap between phases - mirrors `_updateNotification(asPaused: true)`
        // being called from both `pauseSession()` and the natural-completion path.
        val isStatic = state.isPaused || state.isAwaitingContinue

        val title = when {
            state.isAwaitingContinue -> getString(R.string.focusStateReady)
            state.isPaused -> getString(R.string.focusStatePaused)
            state.isRestPhase -> getString(R.string.focusStateResting)
            else -> getString(R.string.focusStateActive)
        }
        val accentColor = when {
            isStatic -> ErrorColor
            state.isRestPhase -> WarningColor
            else -> FocusColor
        }.toArgb()

        val customView = RemoteViews(packageName, R.layout.custom_timer_notification)
        customView.setTextViewText(R.id.notification_title, title)
        customView.setTextViewText(R.id.notification_body, state.modeName)
        customView.setTextColor(R.id.notification_title, accentColor)
        customView.setTextColor(R.id.notification_chronometer, accentColor)
        customView.setTextColor(R.id.notification_paused_text, accentColor)

        if (isStatic) {
            // Show the frozen time (elapsed for stopwatch, remaining for countdown) instead of
            // the layout's "--:--" placeholder.
            customView.setTextViewText(R.id.notification_paused_text, state.centerText)
            customView.setViewVisibility(R.id.notification_chronometer, android.view.View.GONE)
            customView.setViewVisibility(R.id.notification_paused_text, android.view.View.VISIBLE)
        } else {
            customView.setViewVisibility(R.id.notification_chronometer, android.view.View.VISIBLE)
            customView.setViewVisibility(R.id.notification_paused_text, android.view.View.GONE)

            val elapsedRealtime = SystemClock.elapsedRealtime()
            if (state.isStopwatch) {
                val baseTime = elapsedRealtime - (state.elapsedSeconds * 1000L)
                customView.setChronometer(R.id.notification_chronometer, baseTime, null, true)
                customView.setChronometerCountDown(R.id.notification_chronometer, false)
            } else {
                val baseTime = elapsedRealtime + (state.secondsRemaining * 1000L)
                customView.setChronometer(R.id.notification_chronometer, baseTime, null, true)
                customView.setChronometerCountDown(R.id.notification_chronometer, true)
            }
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationService.CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingContentIntent)
            .setColor(accentColor)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        // Kotlin-only enhancement (no Flutter equivalent): interactive actions. The pause/resume
        // action is omitted while awaiting continue - resuming there needs the next phase's
        // config, which only `FocusScreen`/`FocusViewModel` know, so only "Stop" is offered.
        // Session bookkeeping for Stop happens app-side via [FocusTimerManager.stopEvent], so the
        // action stays correct while the app is backgrounded.
        if (!state.isAwaitingContinue) {
            val pauseResumeIntent = Intent(this, FocusTimerService::class.java).apply {
                action = if (state.isRunning) ACTION_PAUSE else ACTION_START
            }
            val pendingPauseResumeIntent = PendingIntent.getService(
                this,
                1,
                pauseResumeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                if (state.isRunning) R.drawable.ic_action_pause else R.drawable.ic_action_play,
                getString(if (state.isRunning) R.string.focusActionPause else R.string.focusActionResume),
                pendingPauseResumeIntent
            )
        }

        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            R.drawable.ic_action_stop,
            getString(R.string.focusActionStop),
            pendingStopIntent
        )

        return builder.build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val ALARM_REQUEST_CODE = 9998

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"


        const val ACTION_DISCARD = "ACTION_DISCARD"
    }
}
