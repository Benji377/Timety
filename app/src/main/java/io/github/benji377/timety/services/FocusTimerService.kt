package io.github.benji377.timety.services

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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

/**
 * Foreground service driving the persistent focus-timer notification. Mirrors the native Android
 * bridge in the Flutter reference (`android/.../MainActivity.kt`'s `showCustomNotification`/
 * `cancelCustomNotification`, invoked from `FocusProvider._updateNotification`): a
 * `DecoratedCustomViewStyle` notification with a live `Chronometer` while running, a static
 * "paused" readout otherwise, an accent color that flags paused/rest/active state, and a single
 * exact alarm (via [TimerSoundReceiver]) that fires the "ding" sound when the current phase's
 * absolute target time is reached.
 *
 * Two enhancements beyond the Flutter reference (intentionally kept, see report): the notification
 * exposes Pause/Resume + Stop actions (Flutter's is purely informational), and [TimerSoundReceiver]
 * also vibrates.
 */
class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val alarmManager by lazy { getSystemService(Context.ALARM_SERVICE) as AlarmManager }

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
        } else {
            cancelAlarm()
        }
    }

    private fun scheduleAlarm(secondsRemaining: Int) {
        val soundIntent = Intent(this, TimerSoundReceiver::class.java)
        val soundPendingIntent = PendingIntent.getBroadcast(
            this, ALARM_REQUEST_CODE, soundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val targetTimeMs = System.currentTimeMillis() + (secondsRemaining * 1000L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetTimeMs,
                soundPendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetTimeMs, soundPendingIntent)
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

        if (isStatic) {
            customView.setViewVisibility(R.id.notification_chronometer, android.view.View.GONE)
            customView.setViewVisibility(R.id.notification_paused_text, android.view.View.VISIBLE)
        } else {
            customView.setViewVisibility(R.id.notification_chronometer, android.view.View.VISIBLE)
            customView.setViewVisibility(R.id.notification_paused_text, android.view.View.GONE)

            val elapsedRealtime = SystemClock.elapsedRealtime()
            if (state.isStopwatch) {
                val baseTime = elapsedRealtime - (state.elapsedSeconds * 1000L)
                customView.setChronometer(R.id.notification_chronometer, baseTime, null, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    customView.setChronometerCountDown(R.id.notification_chronometer, false)
                }
            } else {
                val baseTime = elapsedRealtime + (state.secondsRemaining * 1000L)
                customView.setChronometer(R.id.notification_chronometer, baseTime, null, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    customView.setChronometerCountDown(R.id.notification_chronometer, true)
                }
            }
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationService.CHANNEL_FOCUS)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingContentIntent)
            .setColor(accentColor)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        // Kotlin-only enhancement (no Flutter equivalent): interactive actions. The pause/resume
        // action is omitted while awaiting continue - resuming there needs the next phase's
        // config, which only `FocusScreen`/`FocusViewModel` know, so only "Stop" is offered.
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
                if (state.isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                // No Flutter equivalent exists for these action labels (Flutter's notification has
                // no interactive actions at all - see class doc); kept as English literals per
                // CONVENTIONS.md guidance for strings with no source text to mirror.
                if (state.isRunning) "Pause" else "Resume",
                pendingPauseResumeIntent
            )
        }

        val stopIntent = Intent(this, FocusTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(android.R.drawable.ic_delete, "Stop", pendingStopIntent)

        return builder.build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val ALARM_REQUEST_CODE = 9998

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
    }
}
