package io.github.benji377.timety

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL_ID = "com.timety/timer_notification"
    private val NOTIFICATION_ID = 9997 // matches focusTimerId
    private val NOTIFICATION_CHANNEL = "focus_timer_channel"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_ID).setMethodCallHandler { call, result ->
            when (call.method) {
                "showTimer" -> {
                    val title = call.argument<String>("title") ?: ""
                    val body = call.argument<String>("body") ?: ""
                    val targetTimeMs = call.argument<Long>("targetTimeMs") ?: 0L
                    val isStopwatch = call.argument<Boolean>("isStopwatch") ?: false
                    val isPaused = call.argument<Boolean>("isPaused") ?: false
                    
                    val colorArg = call.argument<Any>("color")
                    val notificationColor = (colorArg as? Number)?.toInt()

                    showCustomNotification(title, body, targetTimeMs, isStopwatch, isPaused, notificationColor)
                    result.success(null)
                }
                "cancelTimer" -> {
                    cancelCustomNotification()
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun showCustomNotification(
        title: String,
        body: String,
        targetTimeMs: Long,
        isStopwatch: Boolean,
        isPaused: Boolean,
        notificationColor: Int?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                "Focus Timer", // The user's localized string might have created this already via dart
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows the active focus timer"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val customView = RemoteViews(packageName, R.layout.custom_timer_notification)
        customView.setTextViewText(R.id.notification_title, title)
        customView.setTextViewText(R.id.notification_body, body)

        if (notificationColor != null) {
            customView.setTextColor(R.id.notification_title, notificationColor)
        }

        if (isPaused) {
            customView.setViewVisibility(R.id.notification_chronometer, android.view.View.GONE)
            customView.setViewVisibility(R.id.notification_paused_text, android.view.View.VISIBLE)
        } else {
            customView.setViewVisibility(R.id.notification_chronometer, android.view.View.VISIBLE)
            customView.setViewVisibility(R.id.notification_paused_text, android.view.View.GONE)

            val now = System.currentTimeMillis()
            val elapsedRealtime = SystemClock.elapsedRealtime()

            val baseTime: Long
            if (isStopwatch) {
                val elapsedSinceStart = now - targetTimeMs
                baseTime = elapsedRealtime - elapsedSinceStart
            } else {
                val remainingTime = targetTimeMs - now
                baseTime = elapsedRealtime + remainingTime
            }

            customView.setChronometer(R.id.notification_chronometer, baseTime, null, true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                customView.setChronometerCountDown(R.id.notification_chronometer, !isStopwatch)
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customView)
            .setCustomBigContentView(customView)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        if (notificationColor != null) {
            builder.color = notificationColor
        }

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun cancelCustomNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
    }
}
