package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.data.model.user.DayRatingEntity
import java.time.Instant

/**
 * Handles a Bad/OK/Great tap on the end-of-day checkup notification: stores the rating for the
 * day the notification was posted on and dismisses the notification as feedback.
 */
class DayRatingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dayKey = intent.getStringExtra(EXTRA_DAY_KEY) ?: return
        val rating = DayRating.fromValue(intent.getIntExtra(EXTRA_RATING, -1)) ?: return
        val app = context.applicationContext as? TimetyApplication ?: return

        launchAsync {
            app.container.dayRatingRepository.upsert(
                DayRatingEntity(
                    dayKey = dayKey,
                    rating = rating.value,
                    createdAt = Instant.now(),
                )
            )
            NotificationManagerCompat.from(app)
                .cancel(NotificationService.END_OF_DAY_CHECKUP_ID)
        }
    }

    companion object {
        internal const val EXTRA_DAY_KEY = "dayKey"
        internal const val EXTRA_RATING = "rating"
    }
}
