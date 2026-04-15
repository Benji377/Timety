package com.github.benji377.timety.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.benji377.timety.utils.NotificationHelper
import kotlinx.coroutines.*

/**
 * Foreground service that keeps the focus timer running even when the app is backgrounded.
 * This prevents the timer from stopping due to process kills.
 */
class FocusTimerService : Service() {

    private val binder = LocalBinder()
    private var timerJob: Job? = null
    private var remainingTime = 0L
    private val _timerState = MutableLiveData<Long>(0)
    val timerState: LiveData<Long> = _timerState

    inner class LocalBinder : Binder() {
        fun getService(): FocusTimerService = this@FocusTimerService
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val duration = intent.getLongExtra(EXTRA_DURATION, 25 * 60 * 1000L)
                startTimer(duration)
            }
            ACTION_PAUSE_TIMER -> pauseTimer()
            ACTION_STOP_TIMER -> stopTimer()
        }
        return START_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        remainingTime = durationMillis

        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main + Job()).launch {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + durationMillis

            while (remainingTime > 0 && isActive) {
                remainingTime = (endTime - System.currentTimeMillis()).coerceAtLeast(0)
                _timerState.postValue(remainingTime)

                // Update notification
                val minutes = remainingTime / 60000
                val seconds = (remainingTime % 60000) / 1000
                val notification = NotificationHelper.showForegroundServiceNotification(
                    this@FocusTimerService,
                    "Focus Timer Running",
                    "%02d:%02d remaining".format(minutes, seconds)
                ).build()

                startForeground(FOREGROUND_NOTIFICATION_ID, notification)

                if (remainingTime > 0) {
                    delay(1000)
                } else {
                    // Timer complete
                    NotificationHelper.showTimerCompleteNotification(
                        this@FocusTimerService,
                        "Your focus session is complete!"
                    )
                    stopSelf()
                    break
                }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
    }

    private fun stopTimer() {
        timerJob?.cancel()
        remainingTime = 0
        _timerState.postValue(0)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }

    companion object {
        const val ACTION_START_TIMER = "com.github.benji377.timety.action.START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.github.benji377.timety.action.PAUSE_TIMER"
        const val ACTION_STOP_TIMER = "com.github.benji377.timety.action.STOP_TIMER"
        const val EXTRA_DURATION = "duration"
        const val FOREGROUND_NOTIFICATION_ID = 100
    }
}


