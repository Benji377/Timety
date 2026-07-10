package io.github.benji377.timety.services

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.user.DayRating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the evening checkup flow end to end: the reminder broadcast posts a notification
 * carrying the three day-rating actions, and tapping one stores the rating and dismisses it.
 */
@RunWith(AndroidJUnit4::class)
class EndOfDayNotificationTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext
    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    @Before
    fun clearNotification() {
        manager.cancel(NotificationService.END_OF_DAY_CHECKUP_ID)
    }

    @Test
    fun eveningCheckup_postsRatingActions_andTapStoresTheRating() {
        context.sendBroadcast(
            Intent(context, ReminderReceiver::class.java)
                .putExtra(NotificationService.EXTRA_NOTIFICATION_ID, NotificationService.END_OF_DAY_CHECKUP_ID)
                .putExtra(NotificationService.EXTRA_TITLE, "Evening checkup")
                .putExtra(NotificationService.EXTRA_BODY, "How was your day?")
                .putExtra(NotificationService.EXTRA_CHANNEL_ID, NotificationService.CHANNEL_EVENING)
        )

        val notification = awaitCheckupNotification()
        assertNotNull("evening checkup notification never appeared", notification)
        val actions = notification!!.notification.actions.orEmpty()
        assertEquals(
            listOf(
                context.getString(R.string.dayRatingBad),
                context.getString(R.string.dayRatingOk),
                context.getString(R.string.dayRatingGreat),
            ),
            actions.map { it.title.toString() },
        )

        // Fire the GREAT action's PendingIntent like a tap on the notification button would.
        actions.last().actionIntent.send()

        val app = context.applicationContext as TimetyApplication
        val stored = awaitUntilNotNull {
            runBlocking { app.container.dayRatingRepository.allRatings.first() }
                .maxByOrNull { it.createdAt }
        }
        assertNotNull("tapping the action stored no rating", stored)
        assertEquals(DayRating.GREAT, DayRating.fromValue(stored!!.rating))
        assertNull("notification should be dismissed after rating", awaitCheckupGone())
    }

    private fun awaitCheckupNotification(): StatusBarNotification? = awaitUntilNotNull {
        manager.activeNotifications.find { it.id == NotificationService.END_OF_DAY_CHECKUP_ID }
    }

    /** Polls until the checkup notification disappears; returns it if it is still up after 5s. */
    private fun awaitCheckupGone(): StatusBarNotification? {
        repeat(50) {
            val active = manager.activeNotifications
                .find { it.id == NotificationService.END_OF_DAY_CHECKUP_ID }
                ?: return null
            if (it == 49) return active
            Thread.sleep(100)
        }
        return null
    }

    private fun <T : Any> awaitUntilNotNull(block: () -> T?): T? {
        repeat(50) {
            block()?.let { return it }
            Thread.sleep(100)
        }
        return null
    }
}
