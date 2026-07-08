package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.benji377.timety.R

/** Plays the focus-timer phase-end chime and a haptic pulse when its alarm fires. */
class TimerSoundReceiver : BroadcastReceiver() {
    companion object {
        private val activePools = mutableSetOf<SoundPool>()
        private var lastPlayTimeMs: Long = 0

        // The ding clip is ~8s; hold the broadcast (goAsync allows ~10s) until it finished,
        // then release. A released pool cuts playback short, so this cannot be much lower.
        private const val RELEASE_DELAY_MS = 8500L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val now = System.currentTimeMillis()
        if (now - lastPlayTimeMs < 2000) return
        lastPlayTimeMs = now

        val pendingResult = goAsync()

        // Haptic feedback
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))

        try {
            // SoundPool instead of MediaPlayer: MediaPlayer routes through mediaserver, which
            // notes the audio op with an empty attribution tag and spams
            // "attributionTag not declared in manifest" on every play/stop (Android 14+).
            val builder = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                builder.setContext(context)
            }
            val soundPool = builder.build()
            activePools.add(soundPool)

            fun done(pool: SoundPool) {
                pool.release()
                activePools.remove(pool)
                pendingResult.finish()
            }

            soundPool.setOnLoadCompleteListener { pool, sampleId, status ->
                if (status == 0) {
                    pool.play(sampleId, 1f, 1f, 1, 0, 1f)
                    Handler(Looper.getMainLooper()).postDelayed({ done(pool) }, RELEASE_DELAY_MS)
                } else {
                    done(pool)
                }
            }
            soundPool.load(context, R.raw.ding, 1)
        } catch (e: Exception) {
            e.printStackTrace()
            pendingResult.finish()
        }
    }
}
