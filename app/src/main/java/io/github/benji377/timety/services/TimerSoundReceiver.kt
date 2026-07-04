package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

class TimerSoundReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        // Haptic Feedback
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }

        val resId = context.resources.getIdentifier("ding", "raw", context.packageName)
        if (resId != 0) {
            try {
                val uri = Uri.parse("android.resource://" + context.packageName + "/" + resId)
                val mediaPlayer = MediaPlayer()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                mediaPlayer.setDataSource(context, uri)
                mediaPlayer.setOnCompletionListener {
                    it.release()
                    pendingResult.finish()
                }
                mediaPlayer.prepare()
                mediaPlayer.start()
            } catch (e: Exception) {
                e.printStackTrace()
                pendingResult.finish()
            }
        } else {
            pendingResult.finish()
        }
    }
}
